plugins {
    `project-conventions`
    `maven-publish-conventions`
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.atomicfu)
                implementation(libs.stately.collections)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.jctools)
            }
        }
    }
}
