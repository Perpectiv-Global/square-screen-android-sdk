import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.squarescreen.cache"
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
    implementation(project(":squarescreen-core"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(
        groupId = project.property("GROUP") as String,
        artifactId = "squarescreen-cache",
        version = project.property("VERSION_NAME") as String,
    )

    pom {
        name = "SquareScreen Cache"
        description = "Room-based metadata cache and media file cache for the SquareScreen Android SDK."
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
