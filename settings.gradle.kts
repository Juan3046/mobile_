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
rootProject.name = "RM Games"
include(":app")
include(":core:common")
include(":core:data")
include(":core:audio")
include(":core:online")
include(":core:sensors")
include(":core:vision")
include(":feature:home")
include(":feature:session")
include(":feature:settings")
include(":feature:ranking")
include(":feature:onboarding")
include(":feature:game:casino")
include(":feature:game:onion")
include(":feature:game:blink")
include(":feature:game:compass")
include(":feature:game:soda")
include(":feature:game:rush")
include(":feature:game:sound")
