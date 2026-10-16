package pl.xsenia.fv.profile

import pl.xsenia.fv.invoice.validation.isAsciiDigits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ProfileState {
    data object Loading : ProfileState

    /** No KSeF token with a NIP yet — the app needs the token first. */
    data object NoProfile : ProfileState

    data class Active(val session: ProfileSession) : ProfileState
}

/**
 * Picks the active company profile: the one of the NIP in the KSeF token (ADR 0006). Another token NIP switches to
 * another profile; nothing is deleted. Sessions are created once per NIP.
 *
 * @param tokenNip NIP of the stored KSeF token, null when there is no token (or it has no NIP)
 */
class ProfileManager(
    private val filesDir: File,
    tokenNip: Flow<String?>,
    scope: CoroutineScope,
    private val createSession: (nip: String, directory: File) -> ProfileSession,
) {

    private val sessions = mutableMapOf<String, ProfileSession>()

    val state: StateFlow<ProfileState> = tokenNip
        .distinctUntilChanged()
        .map { nip -> if (nip == null) ProfileState.NoProfile else ProfileState.Active(sessionFor(nip)) }
        .stateIn(scope, SharingStarted.Eagerly, ProfileState.Loading)

    /** The active session, if any. */
    val active: ProfileSession?
        get() = (state.value as? ProfileState.Active)?.session

    private suspend fun sessionFor(nip: String): ProfileSession {
        sessions[nip]?.let { return it }
        val directory = profileDirectory(filesDir, nip)
        // Data saved before profiles existed belongs to the company of the first token NIP seen after the update.
        withContext(Dispatchers.IO) { LegacyDataMigration.run(filesDir, directory) }
        return createSession(nip, directory).also { sessions[nip] = it }
    }

    companion object {
        fun profileDirectory(filesDir: File, nip: String): File {
            require(nip.length == 10 && nip.isAsciiDigits()) { "NIP must be 10 digits" }
            return File(File(filesDir, "profiles"), nip)
        }
    }
}
