plugins {
    `project-conventions`
    `maven-publish-conventions`
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
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
