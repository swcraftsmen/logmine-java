plugins {
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "8.1.0"
    jacoco

}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.11"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // Generate report after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // Tests are required to run before generating the report
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                // Exclude generated classes if needed
                exclude("**/*Test*.class")
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // 80% coverage minimum
            }
        }
        
        rule {
            element = "CLASS"
            limit {
                counter = "BRANCH"
                minimum = "0.70".toBigDecimal() // 70% branch coverage
            }
        }
    }
}

tasks.withType<Javadoc> {
    options {
        (this as StandardJavadocDocletOptions).apply {
            addBooleanOption("Xdoclint:all", true)
            addStringOption("Xmaxwarns", "1000")
        }
    }
    isFailOnError = false
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.22.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Configure JAR for library use
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "LogMine Core",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "swengdev.org"
        )
    }
}

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("LogMine")
                description.set("Fast pattern recognition for log analytics - unsupervised log pattern extraction")
                url.set("https://github.com/swcraftsmen/logmine")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("zachhuan")
                        name.set("Zach Huan")
                        email.set("zach@swengdev.org")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/swcraftsmen/logmine.git")
                    developerConnection.set("scm:git:ssh://github.com/swcraftsmen/logmine.git")
                    url.set("https://github.com/swcraftsmen/logmine")
                }
            }
        }
    }
}