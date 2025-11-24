plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    implementation(project(":logmine-core"))
    
    // JMH dependencies (handled by plugin, but listed for clarity)
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    // JMH Configuration
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(2)
    resultFormat.set("JSON")
    resultsFile.set(project.layout.buildDirectory.file("reports/jmh/results.json"))
    
    // Benchmark inclusion patterns
    includes.add(".*Benchmark.*")
}

tasks.register("benchmarkReport") {
    dependsOn("jmh")
    doLast {
        val buildDir = project.layout.buildDirectory.get().asFile
        println("Benchmark results available at: $buildDir/reports/jmh/results.json")
        println("Human-readable results at: $buildDir/results/jmh/")
    }
}

