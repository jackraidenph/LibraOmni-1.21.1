import org.slf4j.event.Level
import java.text.SimpleDateFormat
import java.util.*

val modId = project.property("modId") as String
val modVersion = project.property("modVersion") as String
val modGroup = project.property("modGroup") as String
val modName = project.property("modName") as String
val modLicense = project.property("modLicense") as String
val modAuthors = project.property("modAuthors") as String
val modDescription = project.property("modDescription") as String
val neoVersion = project.property("neoVersion") as String
val neoVersionRange = project.property("neoVersionRange") as String
val parchmentMappingsVersion = project.property("parchmentMappingsVersion") as String
val parchmentMinecraftVersion = project.property("parchmentMinecraftVersion") as String
val minecraftVersion = project.property("minecraftVersion") as String
val minecraftVersionRange = project.property("minecraftVersionRange") as String
val loaderVersionRange = project.property("loaderVersionRange") as String
val uuid = project.property("uuid") as String
val username = project.property("username") as String

plugins {
    id("idea")
    id("java-library")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.141"
    id("java-gradle-plugin")
}

tasks.named<Wrapper>("wrapper") {
    // Switching this to Wrapper.DistributionType.ALL will download the full gradle sources that comes with
    // documentation attached on cursor hover of gradle classes and methods. However, this comes with increased
    // file size for Gradle. If you do switch this to ALL, run the Gradle wrapper task twice afterwards.
    // (Verify by checking gradle/wrapper/gradle-wrapper.properties to see if distributionUrl now points to `-all`)
    distributionType = Wrapper.DistributionType.BIN
}

repositories {
    mavenLocal()
}

fun date(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH-mm'Z'")
    return df.format(Date())
}

base {
    archivesName.set(modId)
    version = modVersion
    group = modGroup
}

listOf("jar").forEach { taskName ->
    tasks.named<AbstractArchiveTask>(taskName) {
        destinationDirectory.set(file("$rootDir/artifacts"))
        archiveClassifier.set("[${date()}]")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion.set(parchmentMappingsVersion)
        minecraftVersion.set(parchmentMinecraftVersion)
    }

    // accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")

    runs {
        create("client") {
            client()

            programArguments.set(listOf(
                "--uuid", uuid,
                "--username", username
            ))

            systemProperties.set(mapOf(
                "neoforge.enabledGameTestNamespaces" to modId
            ))
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("gameTestServer") {
            type.set("gameTestServer")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("data") {
            data()

            // example of overriding the workingDirectory set in configureEach above, uncomment if you want to use it
            // gameDirectory.set(project.file("run-data"))

            programArguments.addAll(
                "--mod", modId, "--all", "--output",
                file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            // "SCAN": For mods scan.
            // "REGISTRIES": For firing of registry events.
            // "REGISTRYDUMP": For getting the contents of all registries.
            systemProperty("forge.logging.markers", "REGISTRIES")

            logLevel.set(Level.DEBUG)
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
    }
}

// Sets up a dependency configuration called 'localRuntime'.
// This configuration should be used instead of 'runtimeOnly' to declare
// a dependency that will be present for runtime testing but that is
// "optional", meaning it will not be pulled by dependents of this mod.
val localRuntime: Configuration = configurations.create("localRuntime")


configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    compileOnly(gradleApi())
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    description = "Interpolated properties of neoforge.mods.toml and copies it to generated sources"
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}
// Include the output of "generateModMetadata" as an input directory for the build
// this works with both building through Gradle and the IDE.
sourceSets.main.get().resources.srcDir(generateModMetadata)

neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

val sourceJar = tasks.register<Jar>("sourceJar") {
    description = "Creates a Jar with sources"
    from(sourceSets.main.get().allSource)
}

tasks.withType<PublishToMavenLocal>().configureEach {
    dependsOn(tasks.jar)
    dependsOn(sourceJar)
}

gradlePlugin {
    plugins {
        create("libraomniBootstrap") {
            id = "dev.jackraidenph.libraomni-bootstrap"
            displayName = "LibraOmni Bootstrap Plugin"
            implementationClass = "dev.jackraidenph.libraomni.gradle.BootstrapPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = modGroup
            artifactId = modId
            version = modVersion

            artifact(tasks.named("jar")) {
                classifier = ""
            }
            artifact(sourceJar) {
                classifier = "sources"
            }
        }
    }
}