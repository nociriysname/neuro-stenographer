package ru.nuclearhack.stenographer.common.uikit.components.state

import androidx.compose.runtime.Immutable
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons

/** Действие панели состояния: кнопка с подписью и необязательной иконкой. */
@Immutable
data class StenoStateAction(val text: String, val onClick: () -> Unit, val icon: StenoIcons? = null)
