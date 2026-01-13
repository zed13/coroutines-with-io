package test.io

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionConfigurationException
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

class ReportDirectoryExtension : BeforeAllCallback, TestInstancePostProcessor {

    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create(ReportDirectoryExtension::class.java)
        private const val REPORT_DIR_KEY = "reportDirectory"
    }

    override fun beforeAll(context: ExtensionContext) {
        val testClassName = context.requiredTestClass.simpleName
        val baseReportDir = Path("./build/test-reports")
        val reportDir = baseReportDir.resolve(testClassName)

        try {
            // Create base directory if needed
            if (!baseReportDir.exists()) {
                Files.createDirectories(baseReportDir)
            }

            // Clean or create test-specific directory
            if (reportDir.exists()) {
                // Delete all contents (files and subdirectories)
                Files.walk(reportDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
                Files.createDirectory(reportDir)
            } else {
                Files.createDirectory(reportDir)
            }

            // Store in ExtensionContext for field injection
            context.getStore(NAMESPACE).put(REPORT_DIR_KEY, reportDir)
        } catch (e: Exception) {
            throw ExtensionConfigurationException(
                "Failed to create or clean report directory: $reportDir",
                e
            )
        }
    }

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        val reportDir = context.getStore(NAMESPACE).get(REPORT_DIR_KEY, Path::class.java)
            ?: throw ExtensionConfigurationException("Report directory not initialized")

        // Find all @ReportDirExtension fields
        val fields = testInstance.javaClass.declaredFields
            .filter { it.isAnnotationPresent(ReportDirExtension::class.java) }

        for (field in fields) {
            // Validate field type
            if (field.type != Path::class.java && field.type != File::class.java) {
                throw ExtensionConfigurationException(
                    "@ReportDirExtension must be applied to File or Path field, " +
                    "found ${field.type.name} on ${testInstance.javaClass.name}.${field.name}"
                )
            }

            // Inject value with type conversion if needed
            field.isAccessible = true
            when (field.type) {
                Path::class.java -> field.set(testInstance, reportDir)
                File::class.java -> field.set(testInstance, reportDir.toFile())
            }
        }
    }
}
