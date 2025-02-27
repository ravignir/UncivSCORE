import com.unciv.build.BuildConfig
import java.util.*
import com.unciv.build.AndroidImagePacker

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 32
    sourceSets {
        getByName("main").apply {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            aidl.srcDirs("src")
            renderscript.srcDirs("src")
            res.srcDirs("res")
            assets.srcDirs("assets")
            jniLibs.srcDirs("libs")
        }
    }
    packagingOptions {
        resources.excludes += "META-INF/robovm/ios/robovm.xml"
        // part of kotlinx-coroutines-android, should not go into the apk
        resources.excludes += "DebugProbesKt.bin"
    }
    defaultConfig {
        applicationId = "com.unciv.app"
        minSdk = 21
        targetSdk = 32 // See #5044
        versionCode = BuildConfig.appCodeNumber
        versionName = BuildConfig.appVersion

        base.archivesName.set("Unciv")
    }

    // necessary for Android Work lib
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    // Had to add this crap for Travis to build, it wanted to sign the app
    // but couldn't create the debug keystore for some reason

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            // If you make this true you get a version of the game that just flat-out doesn't run
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

    }
    lint {
        disable += "MissingTranslation"   // see res/values/strings.xml
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        isCoreLibraryDesugaringEnabled = true
    }
    androidResources {
        // Don't add local save files and fonts to release, obviously
        ignoreAssetsPattern = "!SaveFiles:!fonts:!maps:!music:!mods"
    }
    buildToolsVersion = "32.0.0"
}

task("texturePacker") {
    doFirst {
        logger.info("Calling TexturePacker")
        AndroidImagePacker.packImages(projectDir.path,false)
    }
}

// called every time gradle gets executed, takes the native dependencies of
// the natives configuration, and extracts them to the proper libs/ folders
// so they get packed with the APK.
task("copyAndroidNatives") {
    val natives: Configuration by configurations

    doFirst {
        val rx = Regex(""".*natives-([^.]+)\.jar$""")
        natives.forEach { jar ->
            if (rx.matches(jar.name)) {
                val outputDir = file(rx.replace(jar.name) { "libs/" + it.groups[1]!!.value })
                outputDir.mkdirs()
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
    dependsOn("texturePacker")
}

tasks.whenTaskAdded {
    // See https://github.com/yairm210/Unciv/issues/4842
    if ("package" in name || "assemble" in name || "bundleRelease" in name) {
        dependsOn("copyAndroidNatives")
    }
}

tasks.register<JavaExec>("run") {
    val localProperties = project.file("../local.properties")
    val path = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }

        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
    } else {
        System.getenv("ANDROID_HOME")
    }

    val adb = "$path/platform-tools/adb"

    doFirst {
        project.exec {
            commandLine(adb, "shell", "am", "start", "-n", "com.unciv.app/AndroidLauncher")
        }
    }
}

dependencies {
    // Updating to latest version would require upgrading sourceCompatibility and targetCompatibility to 1_8, and targetSdk to 31 -
    //   run `./gradlew build --scan` to see details
    // Known Android Lint warning: "GradleDependency"
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    // Needed to convert e.g. Android 26 API calls to Android 21
    // If you remove this run `./gradlew :android:lintDebug` to ensure everything's okay.
    // If you want to upgrade this, check it's working by building an apk,
    //   or by running `./gradlew :android:assembleRelease` which does that
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}
