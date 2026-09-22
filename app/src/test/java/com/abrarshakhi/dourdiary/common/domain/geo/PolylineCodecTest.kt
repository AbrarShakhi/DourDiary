package com.abrarshakhi.dourdiary.common.domain.geo

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineCodecTest {
    private val referencePoints = listOf(
        GeoPoint(38.5, -120.2),
        GeoPoint(40.7, -120.95),
        GeoPoint(43.252, -126.453),
    )
    private val referenceEncoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"

    @Test
    fun `encodes the reference vector exactly`() {
        assertEquals(referenceEncoded, PolylineCodec.encode(referencePoints))
    }

    @Test
    fun `decodes the reference vector exactly`() {
        val decoded = PolylineCodec.decode(referenceEncoded)

        assertEquals(referencePoints.size, decoded.size)
        referencePoints.zip(decoded).forEach { (expected, actual) ->
            assertEquals(expected.latitude, actual.latitude, 1e-5)
            assertEquals(expected.longitude, actual.longitude, 1e-5)
        }
    }

    @Test
    fun `a real route survives a round trip`() {
        val route = (0..200).map { GeoPoint(52.52 + it * 0.0001, 13.405 + it * 0.00007) }

        val decoded = PolylineCodec.decode(PolylineCodec.encode(route))

        assertEquals(route.size, decoded.size)
        route.zip(decoded).forEach { (expected, actual) ->
            assertEquals(expected.latitude, actual.latitude, 1e-5)
            assertEquals(expected.longitude, actual.longitude, 1e-5)
        }
    }

    @Test
    fun `an empty route encodes to an empty string`() {
        assertEquals("", PolylineCodec.encode(emptyList()))
        assertEquals(emptyList<GeoPoint>(), PolylineCodec.decode(""))
    }

    @Test
    fun `negative and southern coordinates round trip`() {
        val route = listOf(GeoPoint(-33.8688, 151.2093), GeoPoint(-33.87, 151.21))

        val decoded = PolylineCodec.decode(PolylineCodec.encode(route))

        assertEquals(-33.8688, decoded[0].latitude, 1e-5)
        assertEquals(151.2093, decoded[0].longitude, 1e-5)
    }

    @Test
    fun `a truncated string decodes what it can instead of throwing`() {
        val truncated = referenceEncoded.dropLast(3)

        val decoded = PolylineCodec.decode(truncated)

        assertTrue("Decoding a damaged polyline must not throw", decoded.size <= referencePoints.size)
    }

    @Test
    fun `garbage does not throw and does not invent coordinates`() {
        val decoded = PolylineCodec.decode("!!!not a polyline!!!")

        assertTrue(decoded.all { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 })
    }

    @Test
    fun `the encoding is much smaller than plain coordinates`() {
        val route = (0..500).map { GeoPoint(52.52 + it * 0.0001, 13.405 + it * 0.00007) }

        val encoded = PolylineCodec.encode(route)
        val plain = route.joinToString(";") { "${it.latitude},${it.longitude}" }

        assertTrue(
            "Encoded ${encoded.length} vs plain ${plain.length}",
            encoded.length < plain.length / 3,
        )
    }
}
