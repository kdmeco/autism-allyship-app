package org.autismallyship.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketTokenTest {

    // The Worker mints 20 random bytes in base64url, which comes out at 27 characters.
    @Test
    fun acceptsARealBase64UrlToken() {
        assertTrue(isPlausibleTicketToken("kJ8sH2xQvN4mZ6rT1wY9uL3pA7b"))
    }

    @Test
    fun rejectsAnEmptyToken() {
        assertFalse(isPlausibleTicketToken(""))
    }

    @Test
    fun rejectsATokenContainingASlash() {
        assertFalse(isPlausibleTicketToken("kJ8sH2xQvN4mZ6/rT1wY9uL3pA7b"))
    }

    @Test
    fun rejectsAScannedUrl() {
        assertFalse(isPlausibleTicketToken("https://example.com"))
    }

    @Test
    fun rejectsAnOverlongToken() {
        assertFalse(isPlausibleTicketToken("a".repeat(200)))
    }

    @Test
    fun rejectsATokenContainingASpace() {
        assertFalse(isPlausibleTicketToken("kJ8sH2xQvN4m Z6rT1wY9uL3pA7b"))
    }

    @Test
    fun rejectsATokenBelowTheMinimumLength() {
        assertFalse(isPlausibleTicketToken("kJ8sH2xQvN4m"))
    }
}
