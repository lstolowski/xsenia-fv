package pl.xsenia.fv.mirror

import kotlinx.serialization.Serializable
import pl.xsenia.fv.ksef.http.InstantSerializer
import pl.xsenia.fv.ksef.http.LocalDateSerializer
import pl.xsenia.fv.ksef.invoices.InvoiceMetadata
import pl.xsenia.fv.ksef.invoices.InvoiceSubjectType
import java.time.Instant
import java.time.LocalDate

/** The two invoice lists of the app. */
enum class InvoiceCategory(val subjectType: InvoiceSubjectType, val directoryName: String) {
    /** Faktury sprzedażowe — I am Podmiot1. */
    SALES(InvoiceSubjectType.SELLER, "sales"),

    /** Faktury zakupowe — I am Podmiot2. */
    PURCHASE(InvoiceSubjectType.BUYER, "purchase"),
}

/** Invoice in the local mirror: KSeF metadata plus data read from its XML. */
@Serializable
data class MirrorEntry(
    val metadata: InvoiceMetadata,
    /** Delivery date from the XML (`P_6` → `P_6_Do` → latest `P_6A` → `P_1`), decision D2. */
    @Serializable(with = LocalDateSerializer::class) val deliveryDate: LocalDate,
    /** Set when the XML could not be parsed; [deliveryDate] then falls back to the issue date. */
    val parseError: String? = null,
)

@Serializable
data class CategorySyncState(
    /** KSeF high water mark reached: all invoices stored up to this point are in the mirror. */
    @Serializable(with = InstantSerializer::class) val checkpoint: Instant? = null,
    /** Last successful synchronization of this category. */
    @Serializable(with = InstantSerializer::class) val lastSyncAt: Instant? = null,
)

@Serializable
data class SyncState(
    val sales: CategorySyncState = CategorySyncState(),
    val purchase: CategorySyncState = CategorySyncState(),
) {
    operator fun get(category: InvoiceCategory): CategorySyncState = when (category) {
        InvoiceCategory.SALES -> sales
        InvoiceCategory.PURCHASE -> purchase
    }

    fun with(category: InvoiceCategory, state: CategorySyncState): SyncState = when (category) {
        InvoiceCategory.SALES -> copy(sales = state)
        InvoiceCategory.PURCHASE -> copy(purchase = state)
    }

    /** Oldest successful sync of both categories, i.e. when the whole mirror was last up to date. */
    val lastSyncAt: Instant?
        get() = listOfNotNull(sales.lastSyncAt, purchase.lastSyncAt).takeIf { it.size == 2 }?.minOrNull()
}
