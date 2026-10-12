package pl.xsenia.fv.ksef

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import pl.xsenia.fv.invoice.config.Address
import pl.xsenia.fv.invoice.config.BankAccount
import pl.xsenia.fv.invoice.config.Company
import pl.xsenia.fv.invoice.config.Counterparty
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.Fa3XmlWriter
import pl.xsenia.fv.invoice.issue.SalesInvoiceDraft
import pl.xsenia.fv.ksef.auth.KsefAuthenticator
import pl.xsenia.fv.ksef.auth.KsefPublicKeyProvider
import pl.xsenia.fv.ksef.auth.KsefTokenManager
import pl.xsenia.fv.ksef.http.KsefHttpClientFactory
import pl.xsenia.fv.ksef.sending.KsefInvoiceSender
import pl.xsenia.fv.ksef.support.KsefTestConfig
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * ZAD-014: creates fictitious PURCHASE invoices for the main TEST context, issued by a second TEST context
 * (supplier, `ksef.supplier.*` in secrets). Run only on request — invoices in KSeF cannot be deleted:
 *
 *     ./gradlew :ksef:integrationTest --tests '*PurchaseTestDataGenerator*' -PcreatePurchaseInvoices
 */
class PurchaseTestDataGenerator {

    private val config = KsefTestConfig.loadOrSkip().also {
        assumeTrue(System.getProperty("ksef.createPurchaseInvoices") == "true", "run with -PcreatePurchaseInvoices")
    }
    private val supplier = requireNotNull(config.supplier) { "ksef.supplier.nip / ksef.supplier.token missing in secrets" }
    private val httpClient = KsefHttpClientFactory.create(config.environment)
    private val publicKeys = KsefPublicKeyProvider.fromApi(httpClient)
    private val sender = KsefInvoiceSender(
        httpClient,
        KsefTokenManager(KsefAuthenticator(httpClient, publicKeys), supplier.nip, ksefToken = { supplier.ksefToken }),
        publicKeys,
    )

    private val seller = Company("Testowy Dostawca Sp. z o.o.", supplier.nip, Address("ul. Dostawcza 5", "00-010 Testowo", null), null, null)
    private val buyer = Counterparty("Jan Testowy Usługi Testowe", config.nip, Address("ul. Testowa 1", "00-001 Testowo", null), null)
    private val account = BankAccount("Konto testowe", "Santander Bank Polska", "55109004081797992973253811", "PLN")

    @AfterEach
    fun closeClient() = httpClient.close()

    @Test
    fun `create purchase invoices`(): Unit = runBlocking {
        val hosting = send(
            "ZAK/TEST/101 — VAT 23%, delivery before issue (September)",
            vatInvoice("ZAK/TEST/101", issue = "2026-10-02", delivery = "2026-09-30", DraftLine("Hosting serwera testowego", BigDecimal.ONE, "mies.", BigDecimal("49.00"))),
        )
        send(
            "ZAK/TEST/102 — several lines, delivery = issue (October)",
            vatInvoice(
                "ZAK/TEST/102", issue = "2026-10-10", delivery = "2026-10-10",
                DraftLine("Licencja Lorem Ipsum", BigDecimal(2), "szt.", BigDecimal("120.00")),
                DraftLine("Wsparcie techniczne", BigDecimal("3.5"), "godz.", BigDecimal("80.00")),
            ),
        )
        send("ZAK/TEST/103 — 23% / 8% / exempt, per-line delivery dates (September)", multiRateInvoice())
        send("ZAK/TEST/104 — EUR (October)", euroInvoice())
        send("ZAK/TEST/101/KOR — correction of ZAK/TEST/101 (September)", correctionInvoice(hosting))
    }

    private suspend fun send(label: String, xml: ByteArray): String {
        val sent = sender.send(xml)
        println("SENT $label -> ${sent.ksefNumber} (UPO: ${sent.upo != null})")
        return sent.ksefNumber
    }

    private fun vatInvoice(number: String, issue: String, delivery: String, vararg lines: DraftLine) = Fa3XmlWriter.write(
        SalesInvoiceDraft(number, LocalDate.parse(issue), LocalDate.parse(delivery), seller, buyer, account, lines.toList()),
        Instant.now(),
    )

    private fun multiRateInvoice() = invoice(
        """
        <KodWaluty>PLN</KodWaluty>
        <P_1>2026-10-05</P_1>
        <P_2>ZAK/TEST/103</P_2>
        <P_13_1>100.00</P_13_1>
        <P_14_1>23.00</P_14_1>
        <P_13_2>50.00</P_13_2>
        <P_14_2>4.00</P_14_2>
        <P_13_7>10.00</P_13_7>
        <P_15>187.00</P_15>
        ${annotations(exemption = "<P_19>1</P_19><P_19A>Art. 43 ust. 1 pkt 37 ustawy o VAT (test)</P_19A>")}
        <RodzajFaktury>VAT</RodzajFaktury>
        ${line(1, "Lorem ipsum 23%", "godz.", "2.5", "40.00", "100.00", "23", delivery = "2026-09-10")}
        ${line(2, "Dolor sit 8%", "szt.", "5", "10.00", "50.00", "8", delivery = "2026-09-28")}
        ${line(3, "Ubezpieczenie testowe (zw)", "usł.", "1", "10.00", "10.00", "zw", delivery = "2026-09-15")}
        <Platnosc>
          <Zaplacono>1</Zaplacono>
          <DataZaplaty>2026-10-05</DataZaplaty>
          <FormaPlatnosci>2</FormaPlatnosci>
        </Platnosc>
        """,
    )

    private fun euroInvoice() = invoice(
        """
        <KodWaluty>EUR</KodWaluty>
        <P_1>2026-10-08</P_1>
        <P_2>ZAK/TEST/104</P_2>
        <P_13_1>100.00</P_13_1>
        <P_14_1>23.00</P_14_1>
        <P_14_1W>97.75</P_14_1W>
        <P_15>123.00</P_15>
        ${annotations()}
        <RodzajFaktury>VAT</RodzajFaktury>
        ${line(1, "Subskrypcja testowa (EUR)", "mies.", "1", "100.00", "100.00", "23", exchangeRate = "4.2500")}
        <Platnosc>
          <TerminPlatnosci><Termin>2026-10-22</Termin></TerminPlatnosci>
          <FormaPlatnosci>6</FormaPlatnosci>
          <RachunekBankowy><NrRB>55109004081797992973253811</NrRB><NazwaBanku>Santander Bank Polska</NazwaBanku></RachunekBankowy>
        </Platnosc>
        """,
    )

    private fun correctionInvoice(correctedKsefNumber: String) = invoice(
        """
        <KodWaluty>PLN</KodWaluty>
        <P_1>2026-10-12</P_1>
        <P_2>ZAK/TEST/101/KOR</P_2>
        <P_6>2026-09-30</P_6>
        <P_13_1>-10.00</P_13_1>
        <P_14_1>-2.30</P_14_1>
        <P_15>-12.30</P_15>
        ${annotations()}
        <RodzajFaktury>KOR</RodzajFaktury>
        <PrzyczynaKorekty>Rabat za niedostępność usługi (test)</PrzyczynaKorekty>
        <TypKorekty>1</TypKorekty>
        <DaneFaKorygowanej>
          <DataWystFaKorygowanej>2026-10-02</DataWystFaKorygowanej>
          <NrFaKorygowanej>ZAK/TEST/101</NrFaKorygowanej>
          <NrKSeF>1</NrKSeF>
          <NrKSeFFaKorygowanej>$correctedKsefNumber</NrKSeFFaKorygowanej>
        </DaneFaKorygowanej>
        ${line(1, "Hosting serwera testowego", "mies.", "1", "49.00", "49.00", "23", stateBefore = true)}
        ${line(2, "Hosting serwera testowego", "mies.", "1", "39.00", "39.00", "23")}
        <Platnosc>
          <TerminPlatnosci><Termin>2026-10-26</Termin></TerminPlatnosci>
          <FormaPlatnosci>6</FormaPlatnosci>
          <RachunekBankowy><NrRB>55109004081797992973253811</NrRB></RachunekBankowy>
        </Platnosc>
        """,
    )

    private fun invoice(fa: String): ByteArray = """
        <?xml version="1.0" encoding="UTF-8"?>
        <Faktura xmlns="http://crd.gov.pl/wzor/2025/06/25/13775/">
          <Naglowek>
            <KodFormularza kodSystemowy="FA (3)" wersjaSchemy="1-0E">FA</KodFormularza>
            <WariantFormularza>3</WariantFormularza>
            <DataWytworzeniaFa>${Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)}</DataWytworzeniaFa>
            <SystemInfo>xsenia-fv test data</SystemInfo>
          </Naglowek>
          <Podmiot1>
            <DaneIdentyfikacyjne><NIP>${seller.nip}</NIP><Nazwa>${seller.name}</Nazwa></DaneIdentyfikacyjne>
            <Adres><KodKraju>PL</KodKraju><AdresL1>${seller.address.line1}</AdresL1><AdresL2>${seller.address.line2}</AdresL2></Adres>
          </Podmiot1>
          <Podmiot2>
            <DaneIdentyfikacyjne><NIP>${buyer.nip}</NIP><Nazwa>${buyer.name}</Nazwa></DaneIdentyfikacyjne>
            <Adres><KodKraju>PL</KodKraju><AdresL1>${buyer.address.line1}</AdresL1><AdresL2>${buyer.address.line2}</AdresL2></Adres>
            <JST>2</JST>
            <GV>2</GV>
          </Podmiot2>
          <Fa>
            $fa
          </Fa>
        </Faktura>
    """.trimIndent().trim().toByteArray()

    private fun annotations(exemption: String = "<P_19N>1</P_19N>") = """
        <Adnotacje>
          <P_16>2</P_16><P_17>2</P_17><P_18>2</P_18><P_18A>2</P_18A>
          <Zwolnienie>$exemption</Zwolnienie>
          <NoweSrodkiTransportu><P_22N>1</P_22N></NoweSrodkiTransportu>
          <P_23>2</P_23>
          <PMarzy><P_PMarzyN>1</P_PMarzyN></PMarzy>
        </Adnotacje>
    """

    private fun line(
        number: Int,
        name: String,
        unit: String,
        quantity: String,
        unitPrice: String,
        net: String,
        rate: String,
        delivery: String? = null,
        exchangeRate: String? = null,
        stateBefore: Boolean = false,
    ) = """
        <FaWiersz>
          <NrWierszaFa>$number</NrWierszaFa>
          ${delivery?.let { "<P_6A>$it</P_6A>" }.orEmpty()}
          <P_7>$name</P_7>
          <P_8A>$unit</P_8A>
          <P_8B>$quantity</P_8B>
          <P_9A>$unitPrice</P_9A>
          <P_11>$net</P_11>
          <P_12>$rate</P_12>
          ${exchangeRate?.let { "<KursWaluty>$it</KursWaluty>" }.orEmpty()}
          ${if (stateBefore) "<StanPrzed>1</StanPrzed>" else ""}
        </FaWiersz>
    """
}
