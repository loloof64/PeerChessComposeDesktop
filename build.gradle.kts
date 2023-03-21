import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.loloof64"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("cafe.adriel.lyricist:lyricist:1.2.2")
                implementation("io.github.wolfraam:chessgame:1.4")
                api("com.arkivanov.decompose:decompose:1.0.0-beta-01")
                api("com.arkivanov.decompose:extensions-compose-jetbrains:1.0.0-beta-01")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PeerChess"
            packageVersion = "1.0.0"
            description = "Play chess remotely with your friends."
            vendor = "Laurent Bernabe"
            licenseFile.set(project.file("license.txt"))
        }
    }
}
