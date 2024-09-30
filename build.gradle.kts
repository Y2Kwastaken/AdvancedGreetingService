import org.gradle.crypto.checksum.Checksum
import java.nio.file.Files

plugins {
    id("java")
    alias(libs.plugins.checksum)
}

group = "sh.miles.ags"
version = "1.0.1-SNAPSHOT"

val bundled by project.configurations.registering
project.configurations.named("implementation") {
    extendsFrom(bundled.get())
}

repositories {
    mavenCentral()
}

dependencies {
    bundled(libs.gson)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

}

java {
    sourceSets.create("bootstrap") {
        java.srcDirs("src/bootstrap/java")
    }
}

val buildMainJar by tasks.register<Jar>("buildMainJar") {
    group = "serviceBuild"
    from(sourceSets.named("main").get().output)
}

val buildChecksums by tasks.register<Checksum>("buildChecksums") {
    group = "serviceBuild"
    val from = bundled.get().files.toMutableList()
    from.add(buildMainJar.outputs.files.singleFile)

    inputFiles.setFrom(from)
    outputDirectory.set(project.layout.buildDirectory.dir("checksums"))
    checksumAlgorithm.set(Checksum.Algorithm.SHA256)
    appendFileNameToChecksum.set(true)

    dependsOn(buildMainJar)
}

val generateVersionsList by tasks.register("generateVersionsList") {
    group = "serviceBuild"

    doFirst {
        val checksumDir = project.layout.buildDirectory.dir("checksums").get().asFile
        val jarFile = checksumDir.resolve("${buildMainJar.outputs.files.singleFile.name}.sha256")
        val versions = project.layout.buildDirectory.dir("serviceBuild").get().asFile.resolve("versions.list")
        if (!versions.exists()) {
            versions.parentFile.mkdirs()
            versions.createNewFile()
        }
        versions.writeText(jarFile.readText())
    }

    dependsOn(buildChecksums)
}

val generateLibrariesList by tasks.register("generateLibraryList") {
    group = "serviceBuild"

    doFirst {
        val checksumDir = project.layout.buildDirectory.dir("checksums").get().asFile
        val bundled = bundled.get()
        val libraries = project.layout.buildDirectory.dir("serviceBuild").get().asFile.resolve("libs.list")
        if (!libraries.exists()) {
            libraries.parentFile.mkdirs()
            libraries.createNewFile()
        }
        val lines = bundled.files.map { checksumDir.resolve("${it.name}.sha256").readText() }.toMutableList()
        libraries.writeText(lines.joinToString("\n"))
    }

    dependsOn(buildChecksums)
}

val assembleBootstrapJar by tasks.register("assemblyBootstrapJar", Jar::class) {
    archiveAppendix = "bootstrap"
    from(sourceSets.named("bootstrap").get().output)

    manifest {
        attributes["Main-Class"] = "sh.miles.ags.bootstrap.Bootstrap"
    }

    into("META-INF") {
        from(project.layout.buildDirectory.dir("serviceBuild").get().asFile.resolve("libs.list"))
    }

    into("META-INF") {
        from(project.layout.buildDirectory.dir("serviceBuild").get().asFile.resolve("versions.list"))
    }
    into("META-INF/libs") {
        from(bundled.get().files)
    }
    into("META-INF/versions") {
        from(buildMainJar.outputs.files.singleFile)
    }

    dependsOn(generateVersionsList, generateLibrariesList)
}

tasks.jar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn(assembleBootstrapJar)
}
