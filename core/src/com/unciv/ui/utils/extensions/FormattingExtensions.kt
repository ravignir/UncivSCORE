package com.unciv.ui.utils.extensions

import com.unciv.models.translations.tr
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

/** Translate a percentage number - e.g. 25 - to the multiplication value - e.g. 1.25f */
fun String.toPercent() = toFloat().toPercent()

/** Translate a percentage number - e.g. 25 - to the multiplication value - e.g. 1.25f */
fun Int.toPercent() = toFloat().toPercent()

/** Translate a percentage number - e.g. 25 - to the multiplication value - e.g. 1.25f */
fun Float.toPercent() = 1 + this/100

/** Convert a [resource name][this] into "Consumes [amount] $resource" string (untranslated) */
fun String.getConsumesAmountString(amount: Int) = "Consumes [$amount] [$this]"

/** Convert a [resource name][this] into "Need [amount] more $resource" string (untranslated) */
fun String.getNeedMoreAmountString(amount: Int) = "Need [$amount] more [$this]"

/** Formats the [Duration] into a translated string */
fun Duration.format(): String {
    val sb = StringBuilder()
    var firstPartAlreadyAdded = false
    for ((unit, part) in toParts()) {
        if (part == 0L) continue

        if (firstPartAlreadyAdded) {
            sb.append(", ")
        }
        sb.append("[${part}] $unit")
        firstPartAlreadyAdded = true
    }
    return sb.toString()
}

private fun Duration.toParts(): SortedMap<ChronoUnit, Long> {
    return buildMap {
        val secondsPart = seconds % 60
        val minutePart = toMinutes() % 60
        val hourPart = toHours() % 24
        put(ChronoUnit.SECONDS, secondsPart)
        put(ChronoUnit.MINUTES, minutePart)
        put(ChronoUnit.HOURS, hourPart)
        put(ChronoUnit.DAYS, toDays())
    }.toSortedMap(compareByDescending { it })
}

/** Formats the [Duration] into a translated string, but only showing the most significant time unit */
fun Duration.formatShort(): String {
    val parts = toParts()
    for ((unit, part) in parts) {
        if (part > 0) return "[${part}] $unit".tr()
    }
    return "[${parts[ChronoUnit.SECONDS]}] ${ChronoUnit.SECONDS}".tr()
}
/**
 * Standardize date formatting so dates are presented in a consistent style and all decisions
 * to change date handling are encapsulated here
 */
object UncivDateFormat {
    private val standardFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    /** Format a date to ISO format with minutes */
    fun Date.formatDate(): String = standardFormat.format(this)
    // Previously also used:
    //val updateString = "{Updated}: " +DateFormat.getDateInstance(DateFormat.SHORT).format(date)

    // Everything under java.time is from Java 8 onwards, meaning older phones that use Java 7 won't be able to handle it :/
    // So we're forced to use ancient Java 6 classes instead of the newer and nicer LocalDateTime.parse :(
    // Direct solution from https://stackoverflow.com/questions/2201925/converting-iso-8601-compliant-string-to-java-util-date

    @Suppress("SpellCheckingInspection")
    private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    /** Parse an UTC date as passed by online API's
     * example: `"2021-04-11T14:43:33Z".parseDate()`
     */
    fun String.parseDate(): Date = utcFormat.parse(this)
}

/** For filters containing '{', apply the [predicate] to each part inside "{}" and aggregate using [operation];
 *  otherwise return `null` for Elvis chaining of the individual filter. */
fun <T> String.filterCompositeLogic(predicate: (String) -> T?, operation: (T, T) -> T): T? {
    val elements: List<T> = removePrefix("{").removeSuffix("}").split("} {")
        .mapNotNull(predicate)
    if (elements.isEmpty()) return null
    return elements.reduce(operation)
}
/** If a filter string contains '{', apply the [predicate] to each part inside "{}" then 'and' (`&&`) them together;
 *  otherwise return `null` for Elvis chaining of the individual filter. */
fun String.filterAndLogic(predicate: (String) -> Boolean): Boolean? =
        if (contains('{')) filterCompositeLogic(predicate) { a, b -> a && b } else null

