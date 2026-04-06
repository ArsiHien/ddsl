plugins {
	base
	id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "uet.ndh"
version = "0.0.1-SNAPSHOT"
description = "Domain specific language for domain driven design"

allprojects {
	repositories {
		mavenCentral()
	}
}

subprojects {
	group = rootProject.group
	version = rootProject.version
}