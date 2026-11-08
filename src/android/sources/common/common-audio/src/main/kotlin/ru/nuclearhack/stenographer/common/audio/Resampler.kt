package ru.nuclearhack.stenographer.common.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Полифазный ресемплер с windowed-sinc фильтром (окно Блэкмана).
 *
 * Зачем не линейная интерполяция: при 44.1/48 кГц → 16 кГц она пропускает всё выше 8 кГц как алиасинг
 * (свист, шипение поверх речи), а ASR к этому чувствителен. Здесь при понижении частоты фильтр режет
 * спектр ниже новой частоты Найквиста. Коэффициенты считаются один раз на фазу, без sin() в цикле.
 */
internal object Resampler {

    /** Число пересечений нуля sinc с каждой стороны. Больше — круче срез, но медленнее. */
    private const val ZERO_CROSSINGS = 12

    /** Запас по полосе: срез на 97 % от частоты Найквиста, чтобы переходная полоса не залезала за неё. */
    private const val BANDWIDTH = 0.97

    /** Окно Блэкмана: a0 + a1·cos(t) + a2·cos(2t). */
    private const val BLACKMAN_A0 = 0.42
    private const val BLACKMAN_A1 = 0.5
    private const val BLACKMAN_A2 = 0.08

    /** Около нуля sinc(x) считаем равным 1, чтобы не делить 0 на 0. */
    private const val SINC_EPSILON = 1e-9

    fun resample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        require(fromRate > 0 && toRate > 0) { "Sample rates must be positive: $fromRate -> $toRate" }
        if (fromRate == toRate || input.isEmpty()) return input.copyOf()

        val divisor = gcd(fromRate, toRate)
        val up = toRate / divisor
        val down = fromRate / divisor

        val cutoff = min(1.0, up.toDouble() / down) * BANDWIDTH
        val halfTaps = ceil(ZERO_CROSSINGS / cutoff).toInt()
        val taps = 2 * halfTaps
        val filters = Array(up) { phase -> buildFilter(phase.toDouble() / up, cutoff, halfTaps, taps) }

        val output = FloatArray((input.size.toLong() * up / down).toInt())
        for (n in output.indices) {
            val position = n.toLong() * down
            val base = (position / up).toInt()
            val filter = filters[(position % up).toInt()]

            val start = base - halfTaps + 1
            val kFrom = maxOf(0, -start)
            val kTo = min(taps, input.size - start)
            var acc = 0f
            for (k in kFrom until kTo) acc += input[start + k] * filter[k]
            output[n] = acc
        }
        return output
    }

    private fun buildFilter(fraction: Double, cutoff: Double, halfTaps: Int, taps: Int): FloatArray =
        FloatArray(taps) { k ->
            // Расстояние (в отсчётах входа) от входного отсчёта до момента выходного отсчёта.
            val x = (k - halfTaps + 1) - fraction
            (cutoff * sinc(cutoff * x) * blackman(x, halfTaps.toDouble())).toFloat()
        }

    private fun sinc(x: Double): Double = if (abs(x) < SINC_EPSILON) 1.0 else sin(PI * x) / (PI * x)

    private fun blackman(x: Double, half: Double): Double {
        if (abs(x) > half) return 0.0
        val t = PI * x / half
        return BLACKMAN_A0 + BLACKMAN_A1 * cos(t) + BLACKMAN_A2 * cos(2 * t)
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
