package com.lumenui.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackendTest {

    @Test
    fun baseUrl_withoutPath() {
        val backend = Backend(protocol = "http", host = "192.168.1.5", port = 9090)
        assertEquals("http://192.168.1.5:9090", backend.baseUrl)
    }

    @Test
    fun baseUrl_withLeadingSlashPath() {
        val backend = Backend(protocol = "https", host = "example.com", port = 443, secondaryPath = "/api")
        assertEquals("https://example.com:443/api", backend.baseUrl)
    }

    @Test
    fun baseUrl_addsMissingLeadingSlash() {
        val backend = Backend(protocol = "http", host = "example.com", port = 9090, secondaryPath = "proxies")
        assertEquals("http://example.com:9090/proxies", backend.baseUrl)
    }

    @Test
    fun baseUrl_blankPathIgnored() {
        val backend = Backend(protocol = "http", host = "example.com", port = 9090, secondaryPath = "   ")
        assertEquals("http://example.com:9090", backend.baseUrl)
    }

    @Test
    fun displayName_fallsBackToHostAndPort() {
        assertEquals("host:9090", Backend(host = "host", port = 9090).displayName)
        assertEquals("Named", Backend(name = "Named", host = "host", port = 9090).displayName)
    }
}
