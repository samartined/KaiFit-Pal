// Environment-only workaround (never commit): repo.maven.apache.org / repo1.maven.org
// answer HTTP 429 intermittently from this container. Google's public mirror of
// Maven Central is tried first; the project's own repositories stay as fallback.
fun org.gradle.api.artifacts.dsl.RepositoryHandler.centralMirrorFirst() {
    val mirror = maven {
        name = "MavenCentralGoogleMirror"
        url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
        content {
            excludeGroupByRegex("androidx.*")
            excludeGroupByRegex("com\\.android.*")
            excludeGroupByRegex("com\\.google\\.android.*")
        }
    }
    remove(mirror)
    addFirst(mirror)
}
beforeSettings {
    pluginManagement.repositories.centralMirrorFirst()
    dependencyResolutionManagement.repositories.centralMirrorFirst()
}
// Robolectric downloads android-all-instrumented jars at test runtime (outside Gradle
// dependency resolution); point it at the same mirror.
allprojects {
    tasks.withType<Test>().configureEach {
        systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
    }
}
