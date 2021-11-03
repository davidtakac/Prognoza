package hr.dtakac.prognoza.common.utils

import java.time.ZoneId
import java.time.ZonedDateTime

fun ZonedDateTime.atStartOfDay(): ZonedDateTime =
    toLocalDate().atStartOfDay(ZoneId.systemDefault())
