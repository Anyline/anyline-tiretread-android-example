plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "2.0.20"
    alias(libs.plugins.compose.compiler)
}

ext.apply {
    set("versionMajor", 7)
    set("versionMinor", 4)
    set("versionPatch", 0)
    set("buildNumber", System.getenv("BUILD_NUMBER"))
}

android {
    namespace = "io.anyline.tiretread.devexample"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.anyline.tiretread.devexample"
        minSdk = 26
        targetSdk = 34
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
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)

    // Anyline Tire Tread SDK dependency
    // ideally, you should pin (at least) the Major version of the SDK to avoid unexpected breaking changes
    implementation(libs.tireTread)

    // Include the 'Compose' dependency to be able to
    // integrate the TireTreadScanView in your Pages
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)

    implementation(libs.kotlinx.serialization.json)

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