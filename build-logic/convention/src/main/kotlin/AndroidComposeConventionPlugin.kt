import com.android.build.api.dsl.CommonExtension
import com.softsuave.resumecreationapp.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Compose-enabled modules.
 *
 * Applies the Kotlin Compose compiler plugin and configures
 * all Compose dependencies via BOM. Also registers a Gradle task
 * to generate Compose compiler reports/metrics for performance analysis.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val commonExtension = extensions.findByType(CommonExtension::class.java)
                ?: error("AndroidComposeConventionPlugin requires an Android plugin (application or library) to be applied first.")

            commonExtension.apply {
                buildFeatures {
                    compose = true
                }
            }

            extensions.configure<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension>("composeCompiler") {
                if (project.hasProperty("composeCompilerReports")) {
                    reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
                    metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
                }
            }

            dependencies {
                val bom = libs.findLibrary("compose-bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))

                add("implementation", libs.findBundle("compose").get())
                add("implementation", libs.findLibrary("activity-compose").get())
                add("debugImplementation", libs.findBundle("compose-debug").get())
            }

            // Compose compiler reports task for performance analysis
            // Usage: ./gradlew assembleRelease -PcomposeCompilerReports=true
            tasks.register("composeCompilerReports") {
                group = "compose"
                description = "Generate Compose compiler metrics and reports"
                doLast {
                    println("Run with: ./gradlew assembleRelease -PcomposeCompilerReports=true")
                    println("Reports will be in: build/compose_compiler/")
                }
            }
        }
    }
}
