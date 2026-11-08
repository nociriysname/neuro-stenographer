package ru.nuclearhack.stenographer.common.uikit.foundation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Движение: fast — отклик контролов, base — смена состояний, slow — появление шитов и панелей.
 * При системной настройке «убрать анимации» ([reduced]) переходы мгновенные, а циклы (пульс, бегущая полоса,
 * скелетон) компоненты заменяют статикой.
 */
@Immutable
class StenoMotion(val reduced: Boolean) {

    fun <T> fast(): FiniteAnimationSpec<T> = spec(durationMillis = 120, easing = EaseOut)

    fun <T> base(): FiniteAnimationSpec<T> = spec(durationMillis = 200, easing = Standard)

    fun <T> slow(): FiniteAnimationSpec<T> = spec(durationMillis = 320, easing = Emphasized)

    /** Периоды бесконечных анимаций, мс. */
    val pulseMillis: Int get() = 1_400
    val indeterminateMillis: Int get() = 1_500
    val skeletonMillis: Int get() = 1_400

    /** Сколько тост висит на экране, мс. */
    val toastMillis: Long get() = 4_000L

    private fun <T> spec(durationMillis: Int, easing: Easing): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis, easing = easing)

    private companion object {
        val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    }
}
