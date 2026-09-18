pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Serves a locally patched org.fossify:commons AAR (see tools/patch-fossify-commons/) that
        // no-ops the "fake version of the app" dialog baked into the upstream artifact. Scoped to
        // that one module only, so it can't shadow anything else.
        exclusiveContent {
            forRepository {
                maven { setUrl("local-repo") }
            }
            filter {
                includeModule("org.fossify", "commons")
            }
        }
        google()
        mavenCentral()
        maven { setUrl("https://www.jitpack.io") }
        mavenLocal()
    }
}
include(":app")
