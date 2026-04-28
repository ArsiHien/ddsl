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

    // Spring AI - OpenRouter (OpenAI-compatible API)
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-qdrant")
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    // Agent orchestration
    implementation("org.bsc.langgraph4j:langgraph4j-core:1.8.3")

    // SnakeYAML for knowledge base parsing
    implementation("org.yaml:snakeyaml:2.2")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
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
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Tests require external services (Qdrant + OpenRouter API)
    // Enable when running: ./gradlew test -PenableTests=true
    enabled = true
}