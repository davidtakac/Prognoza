package hr.dtakac.prognoza.common.utils

import ca.rmen.sunrisesunset.SunriseSunset
import hr.dtakac.prognoza.model.database.ForecastMeta
import hr.dtakac.prognoza.model.database.ForecastTimeSpan
import hr.dtakac.prognoza.model.database.Place
import hr.dtakac.prognoza.model.repository.*
import hr.dtakac.prognoza.model.ui.RepresentativeWeatherDescription
import hr.dtakac.prognoza.model.ui.WEATHER_ICONS
import hr.dtakac.prognoza.model.ui.cell.DayUiModel
import hr.dtakac.prognoza.model.ui.cell.HourUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.util.*

fun ForecastTimeSpan.toHourUiModel() =
    HourUiModel(
        id = "$placeId-$startTime",
        temperature = instantTemperature,
        feelsLike = if (instantTemperature == null) {
            null
        } else {
            calculateFeelsLikeTemperature(
                instantTemperature,
                instantWindSpeed,
                instantRelativeHumidity
            )
        },
        precipitationAmount = precipitationAmount,
        windSpeed = instantWindSpeed,
        weatherDescription = WEATHER_ICONS[symbolCode],
        time = startTime,
        relativeHumidity = instantRelativeHumidity,
        windFromCompassDirection = instantWindFromDirection?.toCompassDirection(),
        airPressureAtSeaLevel = instantAirPressureAtSeaLevel
    )

suspend fun List<ForecastTimeSpan>.toDayUiModel(
    coroutineScope: CoroutineScope,
    place: Place
): DayUiModel {
    val weatherIconAsync = coroutineScope.async { representativeWeatherIcon(place) }
    val lowTempAsync = coroutineScope.async { lowestTemperature() }
    val highTempAsync = coroutineScope.async { highestTemperature() }
    val precipitationAsync = coroutineScope.async { totalPrecipitationAmount() }
    val hourWithMaxWindSpeedAsync = coroutineScope.async { hourWithMaxWindSpeed() }
    val maxHumidityAsync = coroutineScope.async { highestRelativeHumidity() }
    val maxPressureAsync = coroutineScope.async { highestPressure() }
    val firstHour = get(0)
    return DayUiModel(
        id = "${firstHour.placeId}-${firstHour.startTime}",
        time = firstHour.startTime,
        representativeWeatherDescription = weatherIconAsync.await(),
        lowTemperature = lowTempAsync.await(),
        highTemperature = highTempAsync.await(),
        totalPrecipitationAmount = precipitationAsync.await(),
        maxWindSpeed = hourWithMaxWindSpeedAsync.await()?.instantWindSpeed,
        windFromCompassDirection = hourWithMaxWindSpeedAsync.await()?.instantWindFromDirection?.toCompassDirection(),
        maxHumidity = maxHumidityAsync.await(),
        maxPressure = maxPressureAsync.await()
    )
}

fun List<ForecastTimeSpan>.toForecastResult(
    meta: ForecastMeta?,
    error: ForecastError?
): ForecastResult {
    return if (isNullOrEmpty()) {
        Empty(error)
    } else {
        val success = Success(meta, this)
        if (error == null) {
            success
        } else {
            CachedSuccess(success, error)
        }
    }
}

fun List<ForecastTimeSpan>.highestTemperature(): Double? {
    val max = maxOf { it.airTemperatureMax ?: Double.MIN_VALUE }
    return if (max == Double.MIN_VALUE) {
        null
    } else {
        max
    }
}

fun List<ForecastTimeSpan>.lowestTemperature(): Double? {
    val min = minOf { it.airTemperatureMin ?: Double.MAX_VALUE }
    return if (min == Double.MAX_VALUE) {
        null
    } else {
        min
    }
}

fun List<ForecastTimeSpan>.highestRelativeHumidity(): Double? {
    val max = maxOf { it.instantRelativeHumidity ?: Double.MIN_VALUE }
    return if (max == Double.MIN_VALUE) {
        null
    } else {
        max
    }
}

fun List<ForecastTimeSpan>.highestPressure(): Double? {
    val max = maxOf { it.instantAirPressureAtSeaLevel ?: Double.MIN_VALUE }
    return if (max == Double.MIN_VALUE) {
        null
    } else {
        max
    }
}

fun List<ForecastTimeSpan>.representativeWeatherIcon(place: Place): RepresentativeWeatherDescription? {
    val timeSpansGroupedByIsDay = groupBy {
        SunriseSunset.isDay(
            GregorianCalendar.from(it.startTime),
            place.latitude,
            place.longitude
        )
    }
    val dayTimeSpans = timeSpansGroupedByIsDay[true] ?: listOf()
    val nightTimeSpans = timeSpansGroupedByIsDay[false] ?: listOf()
    val eligibleSymbolCodes = if (dayTimeSpans.isEmpty()) {
        nightTimeSpans
    } else {
        dayTimeSpans
    }.mapNotNull { it.symbolCode }
    val representativeSymbolCode = eligibleSymbolCodes.mostCommon()
    val weatherIcon = WEATHER_ICONS[representativeSymbolCode]
    return if (weatherIcon == null) {
        null
    } else {
        RepresentativeWeatherDescription(
            weatherDescription = weatherIcon,
            isMostly = eligibleSymbolCodes.any { it != representativeSymbolCode }
        )
    }
}

fun List<ForecastTimeSpan>.totalPrecipitationAmount(): Double {
    return sumOf { it.precipitationAmount ?: 0.0 }
}

fun List<ForecastTimeSpan>.hourWithMaxWindSpeed() = maxWithOrNull { o1, o2 ->
    val difference =
        (o1.instantWindSpeed ?: Double.MIN_VALUE) - (o2.instantWindSpeed ?: Double.MIN_VALUE)
    when {
        difference < 0 -> -1
        difference > 0 -> 1
        else -> 0
    }
}