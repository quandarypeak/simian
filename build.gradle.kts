plugins {
    java
}

// Repositories
repositories {
    mavenCentral()
}

// Project properties from conf/build.properties
val buildNumber = "4.0.0"
val title = "Simian Similarity Analyzer"
val copyright = "Copyright (c) 2023 Quandary Peak Research. All rights reserved."
val license = "Subject to the Quandary Peak Academic Software License."
val webTitle = "$title | Similar Code Detector"

// Build properties
val timestamp = System.currentTimeMillis().toString()
val projectName = "simian"
val buildVersion = "$buildNumber-$timestamp"

// Main classes
val mainClass = "com.quandarypeak.simian.SimianMain"

// Java configuration
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Configure source sets
sourceSets {
    main {
        java {
            srcDirs("main")
            exclude("**/SimianTask.java")  // Exclude Ant-specific task
        }
        resources {
            srcDirs("main")
            exclude("**/*.java")
        }
    }
    test {
        java {
            srcDirs("test")
        }
        resources {
            srcDirs("test")
            exclude("**/*.java")
        }
    }
}

// Dependencies - using Maven Central where possible
dependencies {
    // Build-time dependencies from Maven Central
    implementation("org.ow2.asm:asm:9.2")
    implementation("com.puppycrawl.tools:checkstyle:5.6")
    implementation("com.yworks:yguard:4.1.1")
    
    // Test dependencies from Maven Central
    testImplementation("junit:junit:4.11")
    testImplementation("org.hamcrest:hamcrest-core:1.3")
    
    // Dependencies that must remain as local JARs (not available in Maven Central)
    //implementation(files("lib/build/ObfuscationAnnotation-3.1.0.jar"))
}

// Compilation configuration
tasks.compileJava {
    options.compilerArgs.addAll(listOf("-Xlint"))
    options.isDeprecation = true
    options.isVerbose = true
}

tasks.compileTestJava {
    options.isDeprecation = true
}

// Test configuration
tasks.test {
    useJUnit()
    testLogging {
        events(org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED, 
               org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED, 
               org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
}

// JAR task
tasks.jar {
    archiveBaseName.set(projectName)
    archiveVersion.set(buildNumber)  // Use build number without timestamp
    archiveClassifier.set("")
    
    manifest {
        attributes(
            "Main-Class" to mainClass,
            "Implementation-Title" to title,
            "Implementation-Version" to buildNumber,  // Use build number without timestamp
            "Implementation-Vendor" to "Quandary Peak Research"
        )
    }
}

// Javadoc task - disabled due to malformed HTML in source comments
// This can be re-enabled once the source code documentation is cleaned up
tasks.javadoc {
    enabled = false
}

// Custom tasks to replicate Ant functionality

// Clean task (override the default clean task)
tasks.named("clean") {
    doLast {
        delete("out")
    }
}

// Prepare task (equivalent to Ant prepare)
tasks.register("prepare") {
    group = "build"
    description = "Prepare build directories and copy resources"
    dependsOn("clean")
    
    doLast {
        // Create directories
        file("out").mkdirs()
        file("out/java").mkdirs()
        file("out/docs").mkdirs()
        file("out/conf").mkdirs()
        file("out/dist").mkdirs()
        file("out/classes").mkdirs()
        file("out/classes/main").mkdirs()
        file("out/classes/test").mkdirs()
        file("out/reports/junit").mkdirs()
        
        // Copy main source files with property expansion
        copy {
            from("main")
            into("out/java")
            filter { line ->
                line.replace("\${build.number}", buildNumber)
                    .replace("\${title}", title)
                    .replace("\${copyright}", copyright)
                    .replace("\${license}", license)
                    .replace("\${web.title}", webTitle)
            }
        }
        
        // Copy documentation files
        copy {
            from("docs")
            into("out/docs")
        }
        
        // Copy documentation files with property expansion
        copy {
            from("docs") {
                include("**/*.html", "**/*.xsl", "**/*.txt")
            }
            into("out/docs")
            filter { line ->
                line.replace("\${build.number}", buildNumber)
                    .replace("\${title}", title)
                    .replace("\${copyright}", copyright)
                    .replace("\${license}", license)
                    .replace("\${web.title}", webTitle)
            }
        }
        
        // Copy configuration files with property expansion
        copy {
            from("conf")
            into("out/conf")
            filter { line ->
                line.replace("\${build.number}", buildNumber)
                    .replace("\${title}", title)
                    .replace("\${copyright}", copyright)
                    .replace("\${license}", license)
                    .replace("\${web.title}", webTitle)
            }
        }
    }
}

// Obfuscated JAR task (equivalent to Ant obfuscated-jar)
tasks.register("obfuscated-jar") {
    group = "build"
    description = "Create obfuscated JAR file"
    dependsOn("jar")
    
    doLast {
        // YGuard obfuscation would be implemented here
        // For now, we'll copy the regular JAR
        copy {
            from("build/libs/$projectName-$buildVersion.jar")
            into("out/dist")
            rename { "$it" }
        }
    }
}

// Distribution task (equivalent to Ant dist)
tasks.register("dist") {
    group = "distribution"
    description = "Create distribution package"
    dependsOn("clean", "obfuscated-jar", "test", "javadoc")
    
    doLast {
        // Create TAR.GZ distribution
        val tarFile = file("out/dist/$projectName-$buildVersion.tar.gz")
        tarFile.parentFile.mkdirs()
        
        // This would need a custom task to create TAR.GZ
        // For now, we'll create a placeholder
        println("TAR.GZ distribution would be created here: $tarFile")
        
        // Create MD5 checksum
        val md5File = file("out/dist/$projectName-$buildVersion.tar.gz.MD5")
        // This would need MD5 calculation
        println("MD5 checksum would be created here: $md5File")
    }
}

// Transfer task (equivalent to Ant transfer)
tasks.register("transfer") {
    group = "deployment"
    description = "Transfer files to deploy directory"
    
    doLast {
        // Copy documentation files
        copy {
            from("out/docs")
            into("../../quandarypeak.github.io/simian")
        }
        
        // Copy distribution files
        copy {
            from("out/dist") {
                include("$projectName-$buildVersion.tar.gz", "$projectName-$buildVersion.tar.gz.MD5")
            }
            into("../../quandarypeak.github.io/simian")
        }
    }
}

// Deploy task (equivalent to Ant deploy)
tasks.register("deploy") {
    group = "deployment"
    description = "Deploy application"
    dependsOn("dist", "test", "transfer")
}

// Default task
tasks.register("default") {
    group = "build"
    description = "Default build task"
    dependsOn("test")
}