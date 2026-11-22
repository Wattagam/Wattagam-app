package com.wattagam.app.data.api

import com.wattagam.app.data.auth.Session
import com.wattagam.app.data.auth.SessionStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사진 요청에 로그인 토큰을 왔다감 서버로 갈 때만 붙이는지 */
class ImageClientTest {
    private class FixedSession(private val session: Session?) : SessionStorage {
        override fun load() = session
        override fun save(session: Session) = Unit
        override fun clear() = Unit
    }

    private fun authorizationSentTo(url: String, session: Session?, apiBaseUrl: String = "http://10.0.2.2:8080"): String? {
        var sent: Request? = null
        val client = authorizedImageClient(apiBaseUrl, FixedSession(session)).newBuilder().addInterceptor { chain ->
            sent = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .body(ByteArray(0).toResponseBody("image/jpeg".toMediaType())).build()
        }.build()
        client.newCall(Request.Builder().url(url).build()).execute().close()
        return sent!!.header("Authorization")
    }

    private val loggedIn = Session("jwt", Long.MAX_VALUE, "하늘")

    @Test fun serverImageGetsToken() {
        assertEquals("Bearer jwt", authorizationSentTo("http://10.0.2.2:8080/api/guestbook/1/image", loggedIn))
    }

    @Test fun otherHostOrPortGetsNoToken() {
        assertNull(authorizationSentTo("https://example.com/a.jpg", loggedIn))
        assertNull(authorizationSentTo("http://10.0.2.2:9090/api/guestbook/1/image", loggedIn))
    }

    @Test fun noTokenWhenLoggedOutOrServerMissing() {
        assertNull(authorizationSentTo("http://10.0.2.2:8080/api/guestbook/1/image", session = null))
        assertNull(authorizationSentTo("http://10.0.2.2:8080/api/guestbook/1/image", loggedIn, apiBaseUrl = ""))
    }
}
