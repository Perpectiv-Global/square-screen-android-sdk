import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.squarescreen.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
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
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(
        groupId = project.property("GROUP") as String,
        artifactId = "squarescreen-core",
        version = project.property("VERSION_NAME") as String,
    )

    pom {
        name = "SquareScreen Core"
        description = "Core models, contracts, and result types for the SquareScreen Android SDK."
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
