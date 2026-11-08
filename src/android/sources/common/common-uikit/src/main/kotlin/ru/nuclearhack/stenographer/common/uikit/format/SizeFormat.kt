package ru.nuclearhack.stenographer.common.uikit.format

import kotlin.math.roundToLong

private const val BYTES_PER_MEGABYTE = 1_048_576.0

/**
 * «323 МБ» (мегабайт = 2²⁰ байт), округление до целого; ненулевой объём не бывает «0 МБ».
 * Между числом и единицей — неразрывный пробел, чтобы «МБ» не переносилось на другую строку.
 */
fun formatMegabytes(bytes: Long): String {
    val megabytes = (bytes / BYTES_PER_MEGABYTE).roundToLong()
    return "${if (bytes > 0) megabytes.coerceAtLeast(1) else 0} МБ"
}
