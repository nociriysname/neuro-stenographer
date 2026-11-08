package ru.nuclearhack.stenographer.detekt

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal val KtFile.normalizedPath: String
    get() = virtualFilePath.replace('\\', '/')

internal val KtElement.normalizedPath: String
    get() = containingKtFile.normalizedPath

/** Модуль дизайн-системы: `sources/common/common-uikit`. */
internal fun String.isUikitModulePath(): Boolean = "/sources/common/common-uikit/" in "/${trimStart('/')}"
