package be.shiro.thetacat

import android.content.Context
import fi.iki.elonen.NanoHTTPD


class WebServer(
    private val context: Context,
    private val listener: Listener?
) : NanoHTTPD(8888) {
    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.GET && session.uri == "/") {
            return serveIndexFile()
        } else if (session.method == Method.POST && session.uri == "/meow") {
            listener?.onMeowRequest()
        } else if (session.method == Method.POST && session.uri == "/take_picture") {
            listener?.onReleaseRequest()
        }
        return redirectToIndex()
    }

    private fun serveIndexFile(): Response {
        val body = context.assets.open("index.html").reader().readText()
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, body)
    }

    private fun redirectToIndex(): Response {
        return newFixedLengthResponse(Response.Status.REDIRECT, MIME_PLAINTEXT, "Redirect to index").apply {
            addHeader("Location", "/")
        }
    }

    interface Listener {
        fun onMeowRequest()
        fun onReleaseRequest()
    }
}
