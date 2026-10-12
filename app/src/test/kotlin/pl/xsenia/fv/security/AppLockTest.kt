package pl.xsenia.fv.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppLockTest {

    private var now = 0L
    private val lock = AppLock(elapsedRealtime = { now }, relockAfterMillis = 60_000)

    @Test
    fun `is locked on cold start`() {
        assertTrue(lock.locked.value)
    }

    @Test
    fun `stays unlocked after a short trip to background`() {
        lock.unlock()
        lock.onBackground()
        now += 59_999
        lock.onForeground()

        assertFalse(lock.locked.value)
    }

    @Test
    fun `locks again after a minute in background`() {
        lock.unlock()
        lock.onBackground()
        now += 60_000
        lock.onForeground()

        assertTrue(lock.locked.value)
    }

    @Test
    fun `foreground without preceding background does not lock`() {
        lock.unlock()
        now += 600_000
        lock.onForeground()

        assertFalse(lock.locked.value)
    }
}
