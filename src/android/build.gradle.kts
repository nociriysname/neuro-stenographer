// Все плагины объявляются здесь с apply false: так они попадают в один classloader,
// и AGP со встроенным Kotlin использует ту версию KGP, что указана в каталоге.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.metro) apply false
}
