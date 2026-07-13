plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
            "**/.phase-backups/**",
            "**/phase-*-update/**",
        )
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "android_studio",
                "ktlint_standard_filename" to "disabled",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
            "**/.phase-backups/**",
            "**/phase-*-update/**",
        )
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf("ktlint_code_style" to "android_studio"),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("projectFiles") {
        target(
            "**/*.md",
            "**/*.properties",
            "**/*.yml",
            "**/*.yaml",
            "**/.gitignore",
            "**/.gitattributes",
        )
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
            "**/.phase-backups/**",
            "**/phase-*-update/**",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    parallel = false
    ignoreFailures = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    setSource(
        fileTree(rootDir) {
            include("**/*.kt")
            exclude(
                "**/build/**",
                "**/.gradle/**",
                "**/.phase-backups/**",
                "**/phase-*-update/**",
            )
        },
    )
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = providers.gradleProperty("warningsAsErrors")
                .map(String::toBoolean)
                .orElse(true)
                .get()
            freeCompilerArgs += listOf("-Xjsr305=strict")
        }
    }
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs formatting and static-analysis checks for the whole project."
    dependsOn("spotlessCheck", "detekt")
}

tasks.register("qualityFix") {
    group = "formatting"
    description = "Formats supported project files using the configured style."
    dependsOn("spotlessApply")
}

tasks.named<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
