package org.jetbrains.amper.ktor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Response (
    val elements: List<Element>
)

@Serializable
data class Element (
    val bounds: Bounds,
    val tags: Tags
)

@Serializable
data class Bounds (
    val minlat: Double,
    val minlon: Double,
    val maxlat: Double,
    val maxlon: Double
)

@Serializable
data class Tags (
    @SerialName("admin_level")
    val adminLevel: String,

    val boundary: String,
    val name: String,
    val type: String
)

fun main() = runBlocking {
    val query = """
        [out:json][timeout:90];
        area["name"="Quận 5"]->.district;
        relation(area.district)["admin_level"="6"]->.wards;
        .wards out geom;
    """.trimIndent()

    val httpClient = HttpClient() {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true }
            )
        }
    }

    val overpassUrl = "https://overpass.private.coffee/api/interpreter/"

    val response = httpClient.post(overpassUrl) {
        setBody(query)
    }.body<Response>()

    println(response)
    httpClient.close()
}
