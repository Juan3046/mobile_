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
include(":core:sensors")
include(":feature:home")
include(":feature:session")
include(":feature:game:escape")
include(":feature:game:onion")
include(":feature:game:blink")
include(":feature:game:compass")
include(":feature:game:soda")
