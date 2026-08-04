// AGP 9 má Kotlin zabudovaný - plugin org.jetbrains.kotlin.android sa už NEAPLIKUJE
// (build s ním padne rovno pri konfigurácii). Compose compiler plugin ostáva.
plugins {
    alias(libs.plugins.android).apply(false)
    alias(libs.plugins.kotlin.compose).apply(false)
    alias(libs.plugins.ksp).apply(false)
}
