pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProjectLedger"

include(":app")

include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:database")

include(":domain:transactions")

include(":feature:dashboard")
include(":feature:transactions")
include(":feature:reports")

include(":platform:device")
