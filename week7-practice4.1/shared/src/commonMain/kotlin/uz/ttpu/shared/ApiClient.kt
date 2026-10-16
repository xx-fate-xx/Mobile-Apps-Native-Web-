package uz.ttpu.shared

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

// Task 9 - engine is picked from the platform dependency (OkHttp / Darwin)
class ApiClient {
    private val client = HttpClient()

    suspend fun loadText(url: String): String = client.get(url).bodyAsText()
}
