import org.spongepowered.asm.gradle.plugins.MixinExtension

plugins {
    id("eclipse")
    `maven-publish`
    kotlin("jvm")
    id("forge-gradle-kts")
}

apply(plugin = "net.minecraftforge.gradle")
apply(plugin = "org.spongepowered.mixin")

configure<MixinExtension> {
    add(sourceSets["main"], "lightland-magic.refmap.json")
}

repositories {
    flatDir {
        dirs("libs")
    }
}

configureForge {
    runs {
        createClient("clientMagic", project) {
            arg("-mixin.config=lightland-magic.mixins.json")
        }
        createServer("serverMagic", project) {
            arg("-mixin.config=lightland-magic.mixins.json")
        }
    }
}

useGeneratedResources()

dependencies {
    minecraft(project)
    core
    lombok
    jei(project)
    compileOnly(fg.deobf("net.darkhax.gamestages:GameStages-$mcVersion:7.2.8"))
    junit
    mixin
    implementation(fg.deobf("twilightforest:twilightforest-1.16.5:4.0.546-universal"))
}

// Example for how to get properties into the manifest for reading by the runtime..
jar {
    defaultManifest(project)
    manifest {
        attributes(
                attributes + mapOf(
                        "MixinConfigs" to "lightland-magic.mixins.json"
                )
        )
    }
    finalizedBy("reobfJar")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.getByName("jar"))
        }
    }
    repositories {
        maven {
            url = uri("file:///${project.projectDir}/mcmodsrepo")
        }
    }
}

disableTests()
excludeReobfJar()

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
