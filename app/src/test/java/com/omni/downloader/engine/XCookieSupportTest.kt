package com.omni.downloader.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

import org.junit.Assert.assertTrue
import org.junit.Test

class XCookieSupportTest {

    @Test
    fun `parses compact auth_token and ct0`() {
        val pairs = XCookieSupport.parseCookiePairs("auth_token=abc123; ct0=def456")
        assertEquals("abc123", pairs["auth_token"])
        assertEquals("def456", pairs["ct0"])
        assertTrue(XCookieSupport.hasUsableCookie("auth_token=abc123; ct0=def456"))
    }

    @Test
    fun `parses full browser cookie string`() {
        val raw = "guest_id=v1%3A1712345678; lang=en; auth_token=abc123; ct0=def456; twid=u%3D42"
        val pairs = XCookieSupport.parseCookiePairs(raw)
        assertEquals("abc123", pairs["auth_token"])
        assertEquals("def456", pairs["ct0"])
        assertEquals("v1%3A1712345678", pairs["guest_id"])
    }

    @Test
    fun `parses pasted netscape cookie lines`() {
        val raw = """
            # Netscape HTTP Cookie File
            .x.com	TRUE	/	TRUE	1893456000	auth_token	abc123
            .x.com	TRUE	/	TRUE	1893456000	ct0	def456
        """.trimIndent()
        val pairs = XCookieSupport.parseCookiePairs(raw)
        assertEquals("abc123", pairs["auth_token"])
        assertEquals("def456", pairs["ct0"])
    }

    @Test
    fun `strips quotes and blank values`() {
        val pairs = XCookieSupport.parseCookiePairs("auth_token=\"abc123\"; empty=; ct0=def456")
        assertEquals("abc123", pairs["auth_token"])
        assertEquals("def456", pairs["ct0"])
        assertEquals("", pairs["empty"])
    }

    @Test
    fun `rejects cookie without csrf token`() {
        assertFalse(XCookieSupport.hasUsableCookie("auth_token=abc123"))
        assertFalse(XCookieSupport.hasUsableCookie(""))
        assertFalse(XCookieSupport.hasUsableCookie("garbage"))
    }

    @Test
    fun `ignores unrelated junk keys`() {
        val pairs = XCookieSupport.parseCookiePairs("如何获取？auth_token=x; ct0=y; 中文说明=1")
        assertEquals("y", pairs["ct0"])
        assertFalse(pairs.containsKey("如何获取？auth_token"))
        assertFalse(pairs.containsKey("中文说明"))
    }
}
