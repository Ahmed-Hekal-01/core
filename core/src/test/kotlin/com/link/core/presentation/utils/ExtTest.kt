package com.link.core.presentation.utils

import android.content.Context
import com.link.core.R
import com.link.core.domain.models.NetworkError
import com.link.core.presentation.models.StringWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtTest {

    @Test
    fun `network error to wrapper mapping`() {
        assertEquals(StringWrapper.FromResource(R.string.no_internet_connection), NetworkError.NoInternet.asStringWrapper())
        assertEquals(StringWrapper.FromResource(R.string.unauthorized_session), NetworkError.Unauthorized.asStringWrapper())
        assertEquals(StringWrapper.FromResource(R.string.forbidden_access), NetworkError.Forbidden.asStringWrapper())
        assertEquals(StringWrapper.FromResource(R.string.something_went_wrong), NetworkError.Unknown.asStringWrapper())
    }

    @Test
    fun `message is preferred when present, resource fallback when null`() {
        assertEquals(StringWrapper.FromString("gone"), NetworkError.NotFound("gone").asStringWrapper())
        assertEquals(StringWrapper.FromResource(R.string.not_found), NetworkError.NotFound(null).asStringWrapper())
        assertEquals(StringWrapper.FromString("db down"), NetworkError.ServerError(500, "db down").asStringWrapper())
        assertEquals(StringWrapper.FromResource(R.string.something_went_wrong), NetworkError.ServerError(500, null).asStringWrapper())
    }

    /** BUG HUNT: blank/empty server messages would show an empty UI string. */
    @Test
    fun `blank message falls back to resource`() {
        assertEquals(StringWrapper.FromResource(R.string.not_found), NetworkError.NotFound("").asStringWrapper())
        assertEquals(StringWrapper.FromResource(R.string.something_went_wrong), NetworkError.ServerError(500, "   ").asStringWrapper())
    }

    @Test
    fun `asString uses raw string untouched`() {
        val ctx = mockk<Context>()
        assertEquals("a%sb", StringWrapper.FromString("a%sb").asString(ctx))
        assertEquals("", StringWrapper.FromString("").asString(ctx))
    }

    @Test
    fun `asString without args calls getString(resId) only`() {
        val ctx = mockk<Context> { every { getString(5) } returns "five" }
        assertEquals("five", StringWrapper.FromResource(5).asString(ctx))
        verify(exactly = 0) { ctx.getString(any(), *anyVararg()) }
    }

    @Test
    fun `asString with args spreads them in order`() {
        val ctx = mockk<Context> { every { getString(7, "x", 2) } returns "x-2" }
        assertEquals("x-2", StringWrapper.FromResource(7, listOf("x", 2)).asString(ctx))
    }
}
