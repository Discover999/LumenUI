package com.lumenui.app.ui.components

import com.lumenui.app.data.Backend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendInputTest {

    @Test
    fun parse_foldsFullWidthColonInsteadOfStrippingIt() {
        val parsed = BackendInput.parse("192.168.31.217：9090")

        assertEquals("192.168.31.217", parsed?.host)
        assertEquals(9090, parsed?.port)
    }

    @Test
    fun parse_splitsHostAndPort() {
        val parsed = BackendInput.parse("192.168.31.217:9090")

        assertEquals("192.168.31.217", parsed?.host)
        assertEquals(9090, parsed?.port)
        assertNull(parsed?.protocol)
    }

    @Test
    fun parse_bareHostKeepsPortUnset() {
        val parsed = BackendInput.parse("192.168.31.217")

        assertEquals("192.168.31.217", parsed?.host)
        assertNull(parsed?.port)
    }

    @Test
    fun parse_readsAPastedUrl() {
        val parsed = BackendInput.parse("http://192.168.31.217:9090")

        assertEquals("http", parsed?.protocol)
        assertEquals("192.168.31.217", parsed?.host)
        assertEquals(9090, parsed?.port)
        assertNull(parsed?.secondaryPath)
    }

    @Test
    fun parse_keepsTheUrlPath() {
        val parsed = BackendInput.parse("https://example.com/api")

        assertEquals("https", parsed?.protocol)
        assertEquals("example.com", parsed?.host)
        assertEquals(443, parsed?.port)
        assertEquals("/api", parsed?.secondaryPath)
    }

    @Test
    fun parse_rejectsAnUnusableSchemeUrl() {
        assertNull(BackendInput.parse("http://"))
        assertNull(BackendInput.parse("not a url://"))
    }

    @Test
    fun isValid_rejectsTheDoublePortAndStrippedHostForms() {
        assertFalse(BackendInput.isValid(Backend(host = "192.168.31.217:9090", port = 9090)))
        assertFalse(BackendInput.isValid(Backend(host = "192.168.31.2179090", port = 9090)))

        assertTrue(BackendInput.isValid(Backend(host = "192.168.31.217", port = 9090)))
    }
}
