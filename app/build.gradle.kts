import java.util.Properties

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val rootProps = Properties().apply {
    rootProject.file("gradle.properties").inputStream().use { load(it) }
}

android {
    namespace = "sk.havrila.cvicime"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "sk.havrila.cvicime"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = rootProps.getProperty("VERSION_CODE").toInt()
        versionName = rootProps.getProperty("VERSION_NAME")
        vectorDrawables.useSupportLibrary = true
    }

    // Podpisovanie: v CI cez premenne prostredia (GitHub Secrets).
    // Lokalne sa signingConfig nevytvori a release build spadne na nepodpisany - to je v poriadku,
    // lebo release stavia vyhradne GitHub Actions.
    val storeFileEnv = System.getenv("SIGNING_STORE_FILE")
    signingConfigs {
        if (!storeFileEnv.isNullOrBlank()) {
            create("release") {
                storeFile = file(storeFileEnv)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Zamerne vypnute: R8 v minulom projekte (Galeria+) rozbil kniznice, ktore si hladaju
            // volajucu triedu na zasobniku. Pri verejnom vydani zapnut spat s presnymi keep pravidlami.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (!storeFileEnv.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // Pri zabudovanom Kotline sa jvmTarget odvodí od targetCompatibility - netreba ho nastavovať.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Robolectric potrebuje prístup k assets (knižnica cvikov) aj k resources.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.bundles.test)
}
