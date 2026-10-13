package pl.xsenia.fv.whitelist

import kotlinx.serialization.Serializable

// Response of GET /api/search/nip/{nip}; only the fields the app uses.

@Serializable
internal data class EntityResponse(val result: EntityResult? = null)

@Serializable
internal data class EntityResult(val subject: EntityDto? = null)

@Serializable
internal data class EntityDto(
    val name: String? = null,
    val nip: String? = null,
    val statusVat: String? = null,
    val regon: String? = null,
    val krs: String? = null,
    val residenceAddress: String? = null,
    val workingAddress: String? = null,
    val accountNumbers: List<String>? = null,
)

@Serializable
internal data class ErrorResponse(val code: String? = null, val message: String? = null)

internal fun EntityDto.toSubject(requestedNip: String) = WhiteListSubject(
    name = name.orEmpty().trim(),
    nip = nip ?: requestedNip,
    vatStatus = statusVat,
    address = WhiteListAddress.parse(workingAddress?.takeIf { it.isNotBlank() } ?: residenceAddress),
    accountNumbers = accountNumbers.orEmpty(),
    regon = regon,
    krs = krs,
)
