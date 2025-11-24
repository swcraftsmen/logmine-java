plugins {
    id("com.diffplug.spotless") version "8.1.0" apply false
    id("org.ajoberstar.reckon") version "0.18.3"
}

group = "org.swengdev.logmine"

// Semantic versioning from git tags
reckon {
    setDefaultInferredScope("minor")
    setScopeCalc(calcScopeFromProp())
    setStageCalc(calcStageFromProp())
}

// Version is automatically calculated from git tags
// Use: ./gradlew reckonTagPush -Preckon.scope=patch|minor|major -Preckon.stage=final
// For snapshots: -Preckon.stage=snapshot

// Apply common configuration to all subprojects
subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Root project tasks
tasks.register("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

tasks.register("build") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}

tasks.register("test") {
    dependsOn(subprojects.map { it.tasks.named("test") })
}