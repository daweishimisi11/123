package com.av123.video.data.net

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `isNewer handles v prefix without shifting segments`() {
        assertFalse(UpdateChecker.isNewer("v0.1.0", "0.1.0"))
        assertFalse(UpdateChecker.isNewer("V0.1.0", "v0.1.0"))
        assertTrue(UpdateChecker.isNewer("v0.1.12", "0.1.9"))
        assertFalse(UpdateChecker.isNewer("v0.1.9", "0.1.12"))
    }

    @Test
    fun `isNewer handles suffixes and invalid tags safely`() {
        assertTrue(UpdateChecker.isNewer("v0.1.0-fork.2", "0.1.0-fork.1"))
        assertTrue(UpdateChecker.isNewer("v0.1.0-fork.10", "0.1.0-fork.2"))
        assertFalse(UpdateChecker.isNewer("v0.1.0-fork.1", "0.1.0-fork.1"))
        assertTrue(UpdateChecker.isNewer("v0.1.0", "0.1.0-beta.1"))
        assertFalse(UpdateChecker.isNewer("v0.1.0-beta.1", "0.1.0"))
        assertFalse(UpdateChecker.isNewer("invalid", "0.1.0"))
        assertFalse(UpdateChecker.isNewer("0.1.0", ""))
    }
}
