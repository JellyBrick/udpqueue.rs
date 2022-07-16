plugins {
    `java-library`
    signing
    `maven-publish`
}

dependencies {
    // Explicit dependency to avoid having to republish api each time
    api("club.minnced:udpqueue-api:0.1.1")
}

val processResources: Copy by tasks
val target = ext["target"]?.toString() ?: ""
val platform = ext["platform"] as String
val artifactName = "udpqueue-native-$platform"

tasks.withType<Jar> {
    archiveBaseName.set(artifactName)
}

tasks.create<Copy>("moveResources") {
    group = "build"

    from("target/$target/release/")

    include {
        it.name == "release" || it.name.endsWith(".so") || it.name.endsWith(".dll") || it.name.endsWith(".dylib")
    }

    into("src/main/resources/natives/$platform")

    processResources.dependsOn(this)
}

tasks.create<Delete>("cleanNatives") {
    group = "build"
    delete(fileTree("src/main/resources/natives"))
    tasks["clean"].dependsOn(this)
}

processResources.include {
    it.isDirectory || it.file.parentFile.name == platform
}


publishing.publications {
    create<MavenPublication>("Release") {
        from(components["java"])

        groupId = group.toString()
        artifactId = artifactName
        version = version.toString()

        pom.apply(ext["generatePom"] as MavenPom.() -> Unit)
        pom.name.set(artifactName)
    }
}

@Suppress("UNCHECKED_CAST")
val keyId = if (ext.has("signingKeyId")) ext["signingKeyId"] as? (() -> String) else null
@Suppress("UNCHECKED_CAST")
val keyArmor = if (ext.has("signingKey")) ext["signingKey"] as? (() -> String) else null

keyId?.let {
    signing {
        keyArmor?.let { key ->
            useInMemoryPgpKeys(key(), null)
        }

        sign(publishing.publications["Release"])
    }
}