import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "com.android_baklava.desktop"
version = "1.1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation(compose.animation)
    implementation(compose.animationGraphics)
    
    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    
    // AndroidX Collection for ArraySet
    implementation("androidx.collection:collection:1.4.0")
}

compose.desktop {
    application {
        mainClass = "com.android_baklava.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "Landroid"
            packageVersion = "1.1.0"
            description = "Android 16 Baklava Easter Egg - Landroid Space Explorer"
            vendor = "Android Easter Eggs"
            
            // Include all resources in the app directory
            includeAllModules = true
            
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "Android Easter Eggs"
                upgradeUuid = "020fdbf7-8a86-473b-a439-41defcccea3e"
                // Create portable directory-based installation
                dirChooser = true
                perUserInstall = false
                shortcut = true
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}
