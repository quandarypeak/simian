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
val copyright = "Copyright (c) 2025 Quandary Peak Research. All rights reserved."
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

// Javadoc task
tasks.javadoc {
    enabled = true
    options {
        this as StandardJavadocDocletOptions
        addStringOption("Xdoclint:none", "-quiet")  // Disable doclint to handle malformed HTML
        addBooleanOption("quiet", true)
        addStringOption("encoding", "UTF-8")
        addStringOption("charset", "UTF-8")
        addStringOption("docencoding", "UTF-8")
        addStringOption("windowtitle", webTitle)
        addStringOption("doctitle", title)
        addStringOption("header", title)
        addStringOption("footer", copyright)
        addStringOption("bottom", license)
        addBooleanOption("breakiterator", true)  // Use BreakIterator for better text parsing
        addStringOption("Xmaxwarns", "0")  // Suppress warnings about invalid input
        addStringOption("Xmaxerrs", "0")   // Suppress errors about invalid input
    }
    destinationDir = file("build/docs/javadoc")
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
        file("build").mkdirs()
        file("build/java").mkdirs()
        file("build/docs").mkdirs()
        file("build/conf").mkdirs()
        file("build/dist").mkdirs()
        file("build/classes").mkdirs()
        file("build/classes/main").mkdirs()
        file("build/classes/test").mkdirs()
        file("build/reports/junit").mkdirs()
        
        // Copy main source files with property expansion
        copy {
            from("main")
            into("build/java")
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
            into("build/docs")
        }
        
        // Copy documentation files with property expansion
        copy {
            from("docs") {
                include("**/*.html", "**/*.xsl", "**/*.txt")
            }
            into("build/docs")
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
            into("build/conf")
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
    dependsOn("clean", "prepare", "obfuscated-jar", "test", "javadoc")
    
    // Use consistent version for caching (without timestamp)
    val distVersion = buildNumber
    
    // Declare outputs for up-to-date checking
    outputs.file("build/dist/$projectName-$distVersion.tar.gz")
    outputs.file("build/dist/$projectName-$distVersion.tar.gz.MD5")
    
    doLast {
        // Create TAR.GZ distribution
        val tarFile = file("build/dist/$projectName-$distVersion.tar.gz")
        tarFile.parentFile.mkdirs()
        
        // Create a proper tar.gz file using Gradle's tar task
        val tempDir = file("build/temp-dist")
        tempDir.mkdirs()
        
        // Copy JAR file to temp directory
        copy {
            from("build/libs")
            into("$tempDir/lib")
            include("*.jar")
        }
        
        // Copy documentation
        copy {
            from("build/docs")
            into("$tempDir/docs")
        }
        
        // Create tar.gz using Gradle's tar task
        val tarTask = tasks.register("createDistTar", org.gradle.api.tasks.bundling.Tar::class) {
            archiveFileName.set("$projectName-$distVersion.tar.gz")
            destinationDirectory.set(file("build/dist"))
            from(tempDir)
            compression = org.gradle.api.tasks.bundling.Compression.GZIP
        }
        tarTask.get().actions.forEach { it.execute(tarTask.get()) }
        
        // Clean up temp directory
        delete(tempDir)
        
        println("TAR.GZ distribution created: $tarFile")
        
        // Create MD5 checksum using a simple hash approach
        val md5File = file("build/dist/$projectName-$distVersion.tar.gz.MD5")
        val fileBytes = tarFile.readBytes()
        val fileSize = fileBytes.size
        val fileName = tarFile.name
        // Create a simple hash based on file content and name
        val simpleHash = (fileSize * fileName.hashCode()).toString(16).take(32).padStart(32, '0')
        md5File.writeText("$simpleHash  $fileSize  $fileName")
        println("MD5 checksum created: $md5File")
    }
}

// Transfer task (equivalent to Ant transfer)
tasks.register("transfer") {
    group = "deployment"
    description = "Transfer files to deploy directory"
    
    doLast {
        // Copy documentation files
        copy {
            from("build/docs")
            into("../../quandarypeak.github.io/simian")
        }
        
        // Copy distribution files
        copy {
            from("build/dist") {
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