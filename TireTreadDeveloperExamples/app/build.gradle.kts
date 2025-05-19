plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("plugin.serialization") version "2.0.20"
}

ext.apply {
    set("versionMajor", 8)
    set("versionMinor", 0)
    set("versionPatch", 0)
    set("buildNumber", System.getenv("BUILD_NUMBER"))
}

android {
    namespace = "io.anyline.tiretread.devexample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.anyline.tiretread.devexample"
        minSdk = 26
        targetSdk = 35
        versionCode = generateVersionCode()
        versionName = generateVersionName()

        buildConfigField(
            "String", "LICENSE_KEY", "\"${System.getenv("TTR_SDK_DEVEX_LICENSE_KEY")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("DEVELOPER_EXAMPLES_KEY_ALIAS")
            keyPassword = System.getenv("DEVELOPER_EXAMPLES_KEY_PASSWORD")
            storeFile = file("../my.keystore")
            storePassword = System.getenv("DEVELOPER_EXAMPLES_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {

    implementation(libs.tireTread)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.appcompat)

    // Coil
    implementation(libs.coil.compose.core)
    implementation(libs.coil.compose)
    implementation(libs.coil)
    implementation(libs.coil.network.ktor)

    // Ktor
    implementation(libs.ktor.client.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

/**
 * Generate the version code based on the defined ext. variables:
 * versionMajor, versionMinor, versionPatch
 * e.g.: Version 1.0.0 => Version code 10000;
 */
fun generateVersionCode(): Int {
    val versionMajor = (ext["versionMajor"]) as Int
    val versionMinor = (ext["versionMinor"]) as Int
    val versionPatch = (ext["versionPatch"]) as Int
    return versionMajor * 10000 + versionMinor * 100 + versionPatch

}

/**
 * Generate the version name based on the defined ext. variables:
 * versionMajor, versionMinor, versionPatch.
 */
fun generateVersionName(): String {
    var versionName =
        "${ext["versionMajor"]}.${ext["versionMinor"]}.${ext["versionPatch"]}"

    if (ext["buildNumber"] != null && ext["buildNumber"].toString() != "") {
        versionName += "+" + ext["buildNumber"]
    }
    return versionName
}