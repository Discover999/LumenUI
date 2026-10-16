import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing material lives outside the repo (keystore.properties is git-ignored).
// Format:
//   storeFile=/absolute/or/repo-relative/path/to/release.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Without the file the release build is left unsigned; CI still builds it to exercise R8.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

// The bundled panel knows its own version and bakes it into the JS bundle it ships, so read it
// out of the panel files rather than assuming it matches this app's. The literal is a bare
// template-string argument to a minified helper (`mw=I(`3.29.1`)`), which is why the pattern
// insists on exactly one match: several candidates would mean the guess is no longer safe, and an
// empty value is better than a wrong one.
val panelVersion: String? = run {
    val bundle = file("src/main/assets/zashboard/assets").listFiles()
        ?.firstOrNull { it.isFile && it.name.startsWith("index-") && it.name.endsWith(".js") }
    if (bundle == null) {
        logger.warn("No bundled panel script found; BuildConfig.PANEL_VERSION stays empty")
        return@run null
    }
    val found = Regex("""[A-Za-z_$][\w$]*\(`(\d+\.\d+\.\d+)`\)""")
        .findAll(bundle.readText())
        .map { it.groupValues[1] }
        .distinct()
        .toList()
    if (found.size != 1) {
        logger.warn("Expected one panel version in ${bundle.name}, found $found")
        return@run null
    }
    found.single()
}

android {
    namespace = "com.lumenui.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lumenui.app"
        minSdk = 26
        targetSdk = 37
        // Version follows the bundled zashboard panel (assets/zashboard): 3.29.1 -> 32901.
        // Bump both when the panel is updated so the app version never drifts from the panel.
        versionCode = 32901
        versionName = "3.29.1"
        // Read from the panel files instead, so the About page can show both and a drift between
        // them is visible. Empty when the panel's bundle could not be read (a build warning says
        // so); the UI then shows 未知 rather than repeating this app's number.
        buildConfigField("String", "PANEL_VERSION", "\"${panelVersion.orEmpty()}\"")
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        // Only the dependency-version checks stay off: dependency upgrades are proposed by
        // Dependabot, so all three would report every bump on every run and nothing else.
        //
        // InsecureBaseConfiguration and ObsoleteSdkInt each had exactly one deliberate finding
        // here, so they are scoped to those files in app/lint.xml instead of being switched off
        // project-wide — they still watch the rest of the code.
        disable += setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion"
        )
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.webkit)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.composables.icons.lucide)
    implementation(libs.flagkit)
    implementation(libs.simple.icons)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
