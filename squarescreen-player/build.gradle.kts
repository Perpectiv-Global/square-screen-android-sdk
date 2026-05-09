import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.squarescreen.player"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":squarescreen-core"))
    implementation(project(":squarescreen-network"))
    implementation(project(":squarescreen-cache"))
    implementation(libs.workmanager.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.workmanager.testing)
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary(variant = "release", sourcesJar = true, publishJavadocJar = false))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(
        groupId = project.property("GROUP") as String,
        artifactId = "squarescreen-player",
        version = project.property("VERSION_NAME") as String,
    )

    pom {
        name = "SquareScreen Player"
        description = "Core player engine for the SquareScreen Android SDK — heartbeat, emergency alerts, commands, and playlist management."
        inceptionYear = "2024"
        url = project.property("POM_URL") as String
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "squarescreen"
                name = "SquareScreen"
                url = "https://squarescreen.io"
            }
        }
        scm {
            url = project.property("POM_SCM_URL") as String
            connection = project.property("POM_SCM_CONNECTION") as String
            developerConnection = project.property("POM_SCM_DEV_CONNECTION") as String
        }
    }
}
