plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)

    implementation(project(":core:common"))

    testImplementation(libs.junit4)
}
