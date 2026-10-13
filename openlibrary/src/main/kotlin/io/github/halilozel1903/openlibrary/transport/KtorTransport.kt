package io.github.halilozel1903.openlibrary.transport

import io.github.halilozel1903.openlibrary.OpenLibraryException
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/** The default transport: GET requests with Ktor, following redirects (`/isbn/...` redirects to `/books/...`). */
internal class KtorTransport(
    httpClient: HttpClient?,
    engine: HttpClientEngine?,
    private val userAgent: String,
    requestTimeoutMillis: Long,
    retries: Int,
) : OpenLibraryTransport, AutoCloseable {

    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: run {
        val setup: HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            followRedirects = true
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
            }
            if (retries > 0) {
                install(HttpRequestRetry) {
                    retryIf(maxRetries = retries) { _, response ->
                        response.status.value == 429 || response.status.value in 500..599
                    }
                    retryOnException(maxRetries = retries, retryOnTimeout = true)
                    exponentialDelay()
                }
            }
        }
        if (engine != null) HttpClient(engine, setup) else HttpClient(CIO, setup)
    }

    override suspend fun get(url: String): String {
        try {
            val response = client.get(url) {
                header(HttpHeaders.UserAgent, userAgent)
                accept(ContentType.Application.Json)
            }
            when {
                response.status == HttpStatusCode.NotFound -> throw OpenLibraryException.NotFound(url)
                response.status == HttpStatusCode.TooManyRequests -> throw OpenLibraryException.RateLimited(
                    url,
                    response.headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull(),
                )
                !response.status.isSuccess() -> throw OpenLibraryException.Http(url, response.status.value)
            }
            return response.bodyAsText()
        } catch (e: OpenLibraryException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OpenLibraryException.Network(url, e)
        }
    }

    override fun close() {
        if (ownsClient) client.close()
    }
}
