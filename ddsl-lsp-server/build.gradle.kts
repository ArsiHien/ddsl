plugins {
    java
    application
    id("org.graalvm.buildtools.native") version "0.10.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "uet.ndh"
version = "0.0.1-SNAPSHOT"

description = "DDSL LSP server (STDIO/WebSocket adapters) with GraalVM native image support"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ddsl-core"))

    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.23.1")

    // Gson is used by websocket JSON-RPC bridge.
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.mockito:mockito-core:5.16.1")
}

application {
    mainClass.set("uet.ndh.ddsl.lsp.stdio.DdslStdioLanguageServerMain")
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.matching("Oracle"))
            })
            imageName.set("ddsl-lsp")
            mainClass.set("uet.ndh.ddsl.lsp.stdio.DdslStdioLanguageServerMain")
            buildArgs.add("--no-fallback")
            buildArgs.add("--install-exit-handlers")
            buildArgs.add("--initialize-at-build-time=org.eclipse.lsp4j")
        }
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("ddsl-lsp")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

tasks.withType<Test> {
    enabled = false
}
