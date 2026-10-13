package com.lumenui.app.web

import com.lumenui.app.data.Backend
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelBackendsJsonTest {

    /** evaluateJavascript hands a JS string back as a JSON-encoded string literal. */
    private fun quoted(json: String): String = Json.encodeToString(json)

    @Test
    fun parseScan_readsListAndActive() {
        val inner = """
            {"ok":true,"list":[
              {"protocol":"http","host":"a","port":9090,"secret":"s","path":"/p","label":"A"},
              {"protocol":"https","host":"b","port":443}
            ],"active":{"protocol":"https","host":"b","port":443}}
        """.trimIndent()
        val scan = PanelBackendsJson.parseScan(quoted(inner))
        assertTrue(scan.ok)
        assertEquals(2, scan.list.size)
        assertEquals("a", scan.list[0].host)
        assertEquals(9090, scan.list[0].port)
        assertEquals("s", scan.list[0].secret)
        assertEquals("/p", scan.list[0].secondaryPath)
        assertEquals("A", scan.list[0].name)
        assertEquals("b", scan.active?.host)
        assertEquals(443, scan.active?.port)
    }

    @Test
    fun parseScan_noActiveIsNull() {
        val scan = PanelBackendsJson.parseScan(quoted("""{"ok":true,"list":[{"host":"a","port":9090}]}"""))
        assertTrue(scan.ok)
        assertNull(scan.active)
    }

    @Test
    fun parseScan_dropsInvalidEntries() {
        val inner = """{"ok":true,"list":[{"host":"","port":9090},{"host":"ok","port":0},{"host":"good","port":8080}]}"""
        val scan = PanelBackendsJson.parseScan(quoted(inner))
        assertEquals(listOf("good"), scan.list.map { it.host })
    }

    @Test
    fun parseScan_malformedInputIsNotOk() {
        assertFalse(PanelBackendsJson.parseScan(null).ok)
        assertFalse(PanelBackendsJson.parseScan("null").ok)
        assertFalse(PanelBackendsJson.parseScan("not-json").ok)
    }

    @Test
    fun encodeOne_mapsAllFields() {
        val backend = Backend(
            protocol = "https", host = "h", port = 8443,
            secret = "sec", secondaryPath = "/x", name = "N"
        )
        val dto = Json.decodeFromString<PanelBackendsJson.Dto>(PanelBackendsJson.encodeOne(backend))
        assertEquals("https", dto.protocol)
        assertEquals("h", dto.host)
        assertEquals(8443, dto.port)
        assertEquals("sec", dto.secret)
        assertEquals("/x", dto.path)
        assertEquals("N", dto.label)
    }

    @Test
    fun encode_producesArrayOfDtos() {
        val list = listOf(
            Backend(protocol = "http", host = "a", port = 1),
            Backend(protocol = "http", host = "b", port = 2)
        )
        val decoded = Json.decodeFromString<List<PanelBackendsJson.Dto>>(PanelBackendsJson.encode(list))
        assertEquals(listOf("a", "b"), decoded.map { it.host })
    }
}
