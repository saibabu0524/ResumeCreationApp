import androidx.room.gradle.RoomExtension
import com.softsuave.resumecreationapp.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Room-enabled modules (:core:database).
 *
 * Applies Room + KSP and configures schema export directory.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("androidx.room")
                apply("com.google.devtools.ksp")
            }

            extensions.configure<RoomExtension> {
                // Schema export directory for migration auditing
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.findBundle("room").get())
                add("ksp", libs.findLibrary("room-compiler").get())
            }
        }
    }
}
