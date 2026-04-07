plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.personal.brunohelper"
version = "1.0.0"

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
                <li>Export Spring Boot 2 controllers or single endpoints to Bruno-compatible OpenCollection YAML files.</li>
                <li>Generate and reuse <code>workspace.yml</code>, <code>opencollection.yml</code>, and <code>folder.yml</code>.</li>
                <li>Include endpoints declared in inherited base controllers during controller export.</li>
                <li>Show export summaries and endpoint details in the IDE Run tool window.</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }
}
