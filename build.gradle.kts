@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
    alias(libs.plugins.ksp)
    alias(libs.plugins.fletchingTable)
    id("fabric-loom")
}

val mc = stonecutter.current.version
version = "${property("mod.version")}+$mc"
base.archivesName = property("mod.id").toString()

repositories {
    fun strictMaven(url: String, vararg groups: String) = maven(url) { content { groups.forEach(::includeGroupAndSubgroups) } }

    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
}

fletchingTable {
    mixins.create("main", Action {
        mixin("default", "${project.property("mod.id")}.mixins.json") {
            env("CLIENT")
        }
    })
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    runtimeOnly(libs.devauth)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.language.kotlin)
    implementation("fabric-api".mc(mc))
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = rootProject.file("src/main/resources/${project.property("mod.id")}.accesswidener")

    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.addAll(
            arrayOf(
                "-Ddevauth.enabled=true",
                "-Ddevauth.account=main",
                "-XX:+AllowEnhancedClassRedefinition"
            )
        )
    }

    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)

        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    processResources {
        val r = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),
            "kotlin" to libs.versions.fabric.language.kotlin.get(),
            "accessWidener" to "${project.property("mod.id")}.accesswidener"
        )

        inputs.properties(r)
        filesMatching("fabric.mod.json") { expand(r) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

fun String.mc(mc: String): Provider<MinimalExternalModuleDependency> = project.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${mc.replace(".", "_")}").get()

fun DependencyHandler.shadow(dep: Any, config: ExternalModuleDependency.() -> Unit = {}) {
    val d = create((dep as? Provider<*>)?.get() ?: dep) as ExternalModuleDependency
    d.config()
    include(d)
    implementation(d)
}