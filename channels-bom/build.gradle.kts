plugins {
    `java-platform`
    `maven-publish-conventions`
}

dependencies {
    constraints {
        project.rootProject.subprojects.forEach { subproject ->
            if (subproject.name != "channels-bom") {
                api(subproject)
            }
        }
    }
}