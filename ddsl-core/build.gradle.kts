plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "DDSL core compiler/runtime module (parser, AST, analysis, codegen)"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    api("com.palantir.javapoet:javapoet:0.11.0")
    implementation("org.freemarker:freemarker:2.3.34")
    implementation("org.springframework:spring-context:6.2.10")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.11")
    }
}

tasks.withType<Test> {
    enabled = false
}