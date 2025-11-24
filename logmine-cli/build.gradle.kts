plugins {
    java
    application
}

group = "org.swengdev.logmine"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Core LogMine library
    implementation(project(":logmine-core"))

    // CLI framework
    implementation("info.picocli:picocli:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")

    // ANSI colors for terminal output
    implementation("org.fusesource.jansi:jansi:2.4.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

application {
    mainClass = "org.swengdev.logmine.cli.LogMineCLI"
}

tasks.test {
    useJUnitPlatform()
}

// Configure JAR manifest
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.swengdev.logmine.cli.LogMineCLI",
            "Implementation-Title" to "LogMine CLI",
            "Implementation-Version" to project.version
        )
    }
}

// Create fat JAR with all dependencies
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    manifest {
        attributes(
            "Main-Class" to "org.swengdev.logmine.cli.LogMineCLI"
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

// Custom task to create native executable script
tasks.register<Exec>("createExecutable") {
    dependsOn("jar")

    doFirst {
        val jarFile = tasks.jar.get().archiveFile.get().asFile
        val scriptFile = file("${project.rootDir}/logmine")

        scriptFile.writeText("""
            #!/usr/bin/env bash
            java -jar "${jarFile.absolutePath}" "$@"
        """.trimIndent())

        scriptFile.setExecutable(true)

        println("Created executable script: ${scriptFile.absolutePath}")
    }
}