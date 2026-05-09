import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.squarescreen.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-proguard-rules.pro")
        buildConfigField("String", "BASE_URL", "\"https://square-screen-api-development-f7zuxa.laravel.cloud/api/v1\"")
    }

    buildTypes {
        debug {
            // Point to production until a staging environment is available.
            buildConfigField("String", "BASE_URL", "\"https://square-screen-api-development-f7zuxa.laravel.cloud/api/v1\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://square-screen-api-development-f7zuxa.laravel.cloud/api/v1\"")
        }
    }

    buildFeatures {
        buildConfig = true
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
    implementation(project(":squarescreen-core"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary(variant = "release", sourcesJar = true, publishJavadocJar = false))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(
        groupId = project.property("GROUP") as String,
        artifactId = "squarescreen-network",
        version = project.property("VERSION_NAME") as String,
    )

    pom {
        name = "SquareScreen Network"
        description = "Retrofit-based network layer for the SquareScreen Android SDK."
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
