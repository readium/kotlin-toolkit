import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import kotlin.reflect.full.memberProperties

// "Module" means "subproject" in terminology of Gradle API.
// To be specific each "Android module" is a Gradle "subproject"
@Suppress("unused")
object ModuleDependency {
    // All consts are accessed via reflection
    const val APP = ":app"
    const val FEATURE_DATA = ":data"

    // False positive" function can be private"
    // See: https://youtrack.jetbrains.com/issue/KT-33610
    /*
    Return list of all modules in the project
     */
    private fun getAllModules() = ModuleDependency::class.memberProperties
        .filter { it.isConst }
        .map { it.getter.call().toString() }
        .toSet()

    /*
     Return list of feature modules in the project
     */
    fun getFeatureModules(): Set<String> {
        val featurePrefix = ""

        return getAllModules()
            .filter { it.startsWith(featurePrefix) }
            .toSet()
    }

    object Project {
        fun DependencyHandler.streamer(): Dependency = project(mapOf("path" to ":readium:streamer"))
        fun DependencyHandler.navigator(): Dependency =
            project(mapOf("path" to ":readium:navigator"))

        fun DependencyHandler.navigatorMedia2(): Dependency =
            project(mapOf("path" to ":readium:navigator-media2"))

        fun DependencyHandler.opds(): Dependency = project(mapOf("path" to ":readium:opds"))
        fun DependencyHandler.lcp(): Dependency = project(mapOf("path" to ":readium:lcp"))
        fun DependencyHandler.shared(): Dependency = project(mapOf("path" to ":readium:shared"))
        fun DependencyHandler.app(): Dependency = project(mapOf("path" to ":test-app"))
    }
}
