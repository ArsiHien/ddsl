plugins {
    java
    id("org.springframework.boot") version "3.5.11"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "uet.ndh"
version = "0.0.1-SNAPSHOT"

description = "DDSL AI backend API for VSCode extension and MCP agent calls"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.1.2"

dependencies {
    implementation(project(":ddsl-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-qdrant")
    implementation("org.springframework.ai:spring-ai-google-genai-embedding")
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    // Agent orchestration
    implementation("org.bsc.langgraph4j:langgraph4j-core:1.8.3")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

springBoot {
    mainClass.set("uet.ndh.ddsl.ai.AiAgentApplication")
}

tasks.withType<Test> {
    enabled = false
}