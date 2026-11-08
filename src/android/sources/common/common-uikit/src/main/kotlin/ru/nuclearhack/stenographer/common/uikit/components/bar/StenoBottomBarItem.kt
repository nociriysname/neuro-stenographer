package ru.nuclearhack.stenographer.common.uikit.components.bar

import androidx.compose.runtime.Immutable
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons

/** Пункт нижней панели: подпись и иконка. */
@Immutable
data class StenoBottomBarItem(val label: String, val icon: StenoIcons)
