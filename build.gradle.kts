plugins {
	java
	id("org.springframework.boot") version "3.5.11"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "uet.ndh"
version = "0.0.1-SNAPSHOT"
description = "Domain specific language for domain driven design"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "1.1.2"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.ai:spring-ai-advisors-vector-store")
	implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
	implementation("org.springframework.ai:spring-ai-starter-vector-store-qdrant")

	// JavaPoet (Palantir fork) for type-safe Java code generation
	implementation("com.palantir.javapoet:javapoet:0.11.0")
	
	// LSP4J for Language Server Protocol implementation
	implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")
	implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.23.1")

	// FreeMarker for template-based code generation
	implementation("org.freemarker:freemarker:2.3.34")

	// LangGraph4j for Agentic Workflow orchestration
	implementation("org.bsc.langgraph4j:langgraph4j-core:1.8.3")

	// Spring AI MCP Server support
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	developmentOnly("org.springframework.ai:spring-ai-spring-boot-docker-compose")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

