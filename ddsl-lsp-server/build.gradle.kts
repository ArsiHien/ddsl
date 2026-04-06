plugins {
    java
    application
    id("org.graalvm.buildtools.native") version "0.10.6"
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

    // Optional embedded WebSocket adapter compatibility with Monaco.
    implementation("org.springframework.boot:spring-boot-starter-websocket:3.5.11")
    implementation("org.springframework.boot:spring-boot-starter-web:3.5.11")

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

tasks.withType<Test> {
    enabled = false
}
