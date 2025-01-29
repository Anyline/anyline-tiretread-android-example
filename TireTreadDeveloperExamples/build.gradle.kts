// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("org.sonarqube") version "6.0.1.5171"
}

sonarqube {
    properties {
        property("sonar.projectKey", "anylinecom_anyline-ttr-devex-android")
        property("sonar.organization", "anyline")
        property("sonar.projectName", "Anyline TTR Mobile Dev Examples Android")
        property(
            "sonar.projectVersion", "${project(":app").extra["versionMajor"]}." +
                    "${project(":app").extra["versionMinor"]}." +
                    "${project(":app").extra["versionPatch"]}"
        )
    }
}
