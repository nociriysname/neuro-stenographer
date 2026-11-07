package ru.nuclearhack.stenographer.common.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResamplerTest {

    @Test
    fun `48k to 16k keeps speech-band tone`() {
        val input = sine(frequency = 1_000.0, sampleRate = 48_000, seconds = 1.0)

        val output = Resampler.resample(input, fromRate = 48_000, toRate = 16_000)

        assertEquals(16_000, output.size)
        assertEquals(SINE_RMS, rms(output.middle()), absoluteTolerance = 0.01)
        // Средние 0.8 с тона 1 кГц — ~1600 пересечений нуля: частота не уехала.
        val crossings = zeroCrossings(output.middle())
        assertTrue(abs(crossings - 1_600) <= 2, "crossings = $crossings")
    }

    @Test
    fun `44_1k to 16k keeps amplitude`() {
        val input = sine(frequency = 3_000.0, sampleRate = 44_100, seconds = 1.0)

        val output = Resampler.resample(input, fromRate = 44_100, toRate = 16_000)

        assertEquals(16_000, output.size)
        assertEquals(SINE_RMS, rms(output.middle()), absoluteTolerance = 0.01)
    }

    @Test
    fun `48k to 16k suppresses tone above new Nyquist instead of aliasing it`() {
        // Линейная интерполяция превратила бы 12 кГц в слышимые 4 кГц той же громкости.
        val input = sine(frequency = 12_000.0, sampleRate = 48_000, seconds = 1.0)

        val output = Resampler.resample(input, fromRate = 48_000, toRate = 16_000)

        assertTrue(rms(output.middle()) < 0.01, "rms = ${rms(output.middle())}")
    }

    @Test
    fun `8k to 16k upsampling keeps amplitude`() {
        val input = sine(frequency = 1_000.0, sampleRate = 8_000, seconds = 1.0)

        val output = Resampler.resample(input, fromRate = 8_000, toRate = 16_000)

        assertEquals(16_000, output.size)
        assertEquals(SINE_RMS, rms(output.middle()), absoluteTolerance = 0.01)
    }

    @Test
    fun `same rate returns a copy`() {
        val input = floatArrayOf(0.1f, -0.2f, 0.3f)

        val output = Resampler.resample(input, fromRate = 16_000, toRate = 16_000)

        assertContentEquals(input, output)
        assertTrue(input !== output)
    }

    private fun sine(frequency: Double, sampleRate: Int, seconds: Double): FloatArray =
        FloatArray((sampleRate * seconds).toInt()) { i -> sin(2 * PI * frequency * i / sampleRate).toFloat() }

    /** Без краёв: там фильтр видит нули за пределами сигнала. */
    private fun FloatArray.middle(): FloatArray = copyOfRange(size / 10, size - size / 10)

    private fun rms(samples: FloatArray): Double = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)

    private fun zeroCrossings(samples: FloatArray): Int =
        samples.toList().zipWithNext().count { (a, b) -> (a < 0f) != (b < 0f) }

    private companion object {
        val SINE_RMS = 1 / sqrt(2.0)
    }
}
