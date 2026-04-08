plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

fun optionalSetting(vararg names: String): String? =
    names.asSequence()
        .mapNotNull { name ->
            providers.environmentVariable(name).orNull?.trim()?.takeIf { it.isNotEmpty() }
                ?: providers.gradleProperty(name).orNull?.trim()?.takeIf { it.isNotEmpty() }
        }
        .firstOrNull()

val marketplaceToken = optionalSetting("JETBRAINS_MARKETPLACE_TOKEN", "intellijPlatformPublishingToken")
val marketplaceChannel = optionalSetting("JETBRAINS_MARKETPLACE_CHANNEL", "intellijPlatformPublishingChannel")
val marketplaceHidden = optionalSetting("JETBRAINS_MARKETPLACE_HIDDEN", "intellijPlatformPublishingHidden")
val signingCertificateChain = optionalSetting("JETBRAINS_CERTIFICATE_CHAIN", "intellijPlatformSigningCertificateChain")
val signingPrivateKey = optionalSetting("JETBRAINS_PRIVATE_KEY", "intellijPlatformSigningPrivateKey")
val signingPrivateKeyPassword = optionalSetting("JETBRAINS_PRIVATE_KEY_PASSWORD", "intellijPlatformSigningPrivateKeyPassword")

group = "io.github.zerojehovah.endpointcollectionexport"
version = "1.0.1"
val projectVersion = version.toString()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("io.swagger.core.v3:swagger-core:2.2.43")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    intellijPlatform {
        create("IC", "2023.3.8")
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
        }

        changeNotes = """
            <p>Initial public release.</p>
            <ul>
                <li>Export Spring Boot 2 controllers or single endpoints as API collection files.</li>
                <li>Generate Bruno-compatible OpenCollection YAML for downstream API workflows.</li>
                <li>Generate and reuse <code>workspace.yml</code>, <code>opencollection.yml</code>, and <code>folder.yml</code>.</li>
                <li>Include endpoints declared in inherited base controllers during controller export.</li>
                <li>Show export summaries and endpoint details in the IDE Run tool window.</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        marketplaceToken?.let { token = it }
        marketplaceChannel?.let { channels = listOf(it) }
        marketplaceHidden?.let { hidden = it.toBoolean() }
    }

    signing {
        signingCertificateChain?.let { certificateChain = it }
        signingPrivateKey?.let { privateKey = it }
        signingPrivateKeyPassword?.let { password = it }
    }
}

tasks {
    register("printVersion") {
        doLast {
            println(projectVersion)
        }
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }
}
