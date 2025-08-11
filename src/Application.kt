package org.jetbrains.amper.ktor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

@Serializable
data class Response(
    val elements: List<Element>
)

@Serializable
data class Element(
    val bounds: Bounds,
    val tags: Tags
)

@Serializable
data class Bounds(
    val minlat: Double,
    val minlon: Double,
    val maxlat: Double,
    val maxlon: Double
)

@Serializable
data class Tags(
    val name: String,
)

fun main() = runBlocking {
    val districtName = "Quận 1"
    val streetName = "Nguyễn Đình Chiểu"

    val query = """
        [out:json][timeout:90];
        area["type"="boundary"]["name"="$districtName"]->.district;
        relation(area.district)["admin_level"="6"]["boundary"="administrative"];
        out geom;
    """.trimIndent()

    val query2 = """   
        [out:json][timeout:90];
        area["type"="boundary"]["name"="$districtName"]->.district;    
        way(area.district)["name"~"$streetName"]["name"!~"Hẻm"];
        out geom;
    """.trimIndent()

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true }
            )
        }
    }

    val overpassUrl = "https://overpass.private.coffee/api/interpreter/"

    val response = async {
        httpClient.post(overpassUrl) {
            setBody(query)
        }.body<Response>()
    }

    val response2 = async {
        httpClient.post(overpassUrl) {
            setBody(query2)
        }.body<Response>()
    }

    val wards = response.await().elements
    val streets = response2.await().elements
    val factory = GeometryFactory()

    wards.forEach { ward ->
        val wardPoints = ward.bounds.run {
            arrayOf(
                Coordinate(minlon, minlat), // bottom-left
                Coordinate(minlon, maxlat), // top-left
                Coordinate(maxlon, maxlat), // top-right
                Coordinate(maxlon, minlat), // bottom-right
                Coordinate(minlon, minlat), // bottom-left to complete polygon
            )
        }

        val wardPolygon = factory.createPolygon(wardPoints)

        val hasIntersect = streets.any { street ->
            val streetPoints = street.bounds.run {
                arrayOf(
                    Coordinate(minlon, minlat), // bottom-left
                    Coordinate(minlon, maxlat), // top-left
                    Coordinate(maxlon, maxlat), // top-right
                    Coordinate(maxlon, minlat), // bottom-right
                    Coordinate(minlon, minlat), // bottom-left to complete polygon
                )
            }

            val streetPolygon = factory.createPolygon(streetPoints)
            wardPolygon.intersects(streetPolygon)
        }

        if (hasIntersect) {
            println("Đường $streetName - $districtName có đi qua ${ward.tags.name}")
        }
    }
}
