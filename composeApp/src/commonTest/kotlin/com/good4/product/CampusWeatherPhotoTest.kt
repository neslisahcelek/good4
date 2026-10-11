package com.good4.product

import com.good4.product.presentation.product_list.views.campusPhotoFor
import com.good4.product.presentation.product_list.views.isRainy
import good4.composeapp.generated.resources.*
import kotlin.test.*

class CampusWeatherPhotoTest {
    @Test
    fun rainSleetAndThunderCountAsRain() {
        for (code in listOf("lightrain", "rainshowers_day", "heavyrainandthunder", "sleet", "lightssleetshowers_night")) {
            assertTrue(isRainy(code), code)
        }
        for (code in listOf(null, "", "clearsky_day", "partlycloudy_night", "fog", "snow")) {
            assertFalse(isRainy(code), code.toString())
        }
    }

    @Test
    fun rainBeatsTimeOfDayAndDayAndEveningUseTheirOwnPhotos() {
        val octoberNoon = 12 * 60
        val octoberNight = 22 * 60
        assertEquals(Res.drawable.akdeniz_campus_weather_rain, campusPhotoFor(true, octoberNight, 10, 284).image)
        val dayPhotos = (0..1).map { campusPhotoFor(false, octoberNoon, 10, it).image }.toSet()
        assertEquals(setOf(Res.drawable.akdeniz_campus_weather, Res.drawable.akdeniz_campus_weather_day_courts), dayPhotos)
        val eveningPhotos = (0..2).map { campusPhotoFor(false, octoberNight, 10, it).image }.toSet()
        assertEquals(
            setOf(Res.drawable.akdeniz_campus_weather_night, Res.drawable.akdeniz_campus_weather_sunset, Res.drawable.akdeniz_campus_weather_night_palms),
            eveningPhotos
        )
        // The same day always shows the same photo.
        assertEquals(campusPhotoFor(false, octoberNoon, 10, 7), campusPhotoFor(false, octoberNoon + 90, 10, 7))
    }
}
