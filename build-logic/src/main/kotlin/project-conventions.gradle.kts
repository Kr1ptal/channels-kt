import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    idea
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.dokka")
    id("kotlin-project-conventions")
}

ktlint {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    version = libs.findVersion("ktlint-tool").get().requiredVersion

    reporters {
        reporter(ReporterType.HTML)
        reporter(ReporterType.SARIF)
    }

    filter {
        exclude { it.file.toPath().startsWith(layout.buildDirectory.asFile.get().toPath()) }
    }

    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "intellij_idea",
            "ktlint_standard_comment-spacing" to "disabled",
            "ktlint_standard_discouraged-comment-location" to "disabled",
            "ktlint_standard_property-naming" to "disabled",
            "ktlint_standard_spacing-between-declarations-with-annotations" to "disabled",
            "ktlint_standard_multiline-if-else" to "disabled",
            "ktlint_standard_value-argument-comment" to "disabled",
            "ktlint_standard_value-parameter-comment" to "disabled",
            "ktlint_standard_backing-property-naming" to "disabled",
            "ktlint_standard_function-expression-body" to "disabled",
            "ktlint_standard_class-signature" to "disabled",
        ),
    )
}
