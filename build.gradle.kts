// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Los plugins se declaran aqui con `apply false` (version unica desde el version catalog) y cada
// modulo los aplica sin re-declarar version. Necesario en multi-modulo con plugins de Kotlin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.google.services) apply false
}
