import java.security.MessageDigest
import com.github.jk1.license.render.TextReportRenderer
import com.github.jk1.license.filter.LicenseBundleNormalizer

plugins {
    java
    `maven-publish`
    id("com.github.jk1.dependency-license-report") version "2.9"
}

// Repositories
repositories {
    mavenLocal()
    mavenCentral()
}

// Project properties from gradle.properties
val buildNumber: String by project
val title: String by project
val copyright: String by project
val license: String by project

// Build properties
val timestamp = System.currentTimeMillis().toString()
val projectName = "simian"
val buildVersion = "$buildNumber"

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
            srcDirs("build/java")
        }
        resources {
            srcDirs("build/java")
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
    
    // Test dependencies from Maven Central
    testImplementation("junit:junit:4.11")
    testImplementation("org.hamcrest:hamcrest-core:1.3")
}

// License report configuration
licenseReport {
    outputDir = "$projectDir/build/reports/dependency-license"
    renderers = arrayOf(TextReportRenderer("NOTICES"))
    filters = arrayOf(LicenseBundleNormalizer())
    excludeGroups = arrayOf("com.quandarypeak")
}

// Task to copy NOTICES file to root with corrected header
tasks.register("refreshNotices") {
    group = "documentation"
    description = "Copy NOTICES file to root with corrected header"
    dependsOn("generateLicenseReport")
    
    doLast {
        val sourceFile = file("build/reports/dependency-license/NOTICES")
        val destFile = file("NOTICES")
        
        if (sourceFile.exists()) {
            val content = sourceFile.readText()
            val updatedContent = content.replace(
                "Dependency License Report for simian",
                "Dependency License Report for $title"
            )
            destFile.writeText(updatedContent)
            println("NOTICES file updated at: ${destFile.absolutePath}")
        } else {
            throw GradleException("Source NOTICES file not found: ${sourceFile.absolutePath}")
        }
    }
}

// Compilation configuration
tasks.compileJava {
    dependsOn("prepare")
    options.compilerArgs.addAll(listOf("-Xlint"))
    options.isDeprecation = true
    options.isVerbose = true
}

tasks.processResources {
    dependsOn("prepare")
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

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.quandarypeak"
            artifactId = projectName
            version = buildNumber
            
            from(components["java"])
            
            pom {
                name.set(title)
                description.set("Simian Similarity Analyzer - Similar Code Detector")
                url.set("https://simian.quandarypeak.com/")
                
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }
                
                developers {
                    developer {
                        name.set("Quandary Peak Research")
                        organization.set("Quandary Peak Research")
                        organizationUrl.set("https://www.quandarypeak.com")
                    }
                }
            }
        }
    }
    
    repositories {
        mavenLocal()
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
        addStringOption("windowtitle", "$title $buildNumber")
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
                    .replace("\${web.title}", "$title $buildNumber")
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
                    .replace("\${web.title}", "$title $buildNumber")
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
                    .replace("\${web.title}", "$title $buildNumber")
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
        
        // Create MD5 checksum based on actual file content
        val md5File = file("build/dist/$projectName-$distVersion.tar.gz.MD5")
        val fileBytes = tarFile.readBytes()
        val fileSize = fileBytes.size
        val fileName = tarFile.name
        // Compute MD5 hash from file bytes
        val md5Digest = MessageDigest.getInstance("MD5")
        val md5Hash = md5Digest.digest(fileBytes).joinToString("") { byte -> "%02x".format(byte) }
        md5File.writeText("$md5Hash  $fileName")
        println("MD5 checksum created: $md5File")
    }
}

// Transfer task (equivalent to Ant transfer)
tasks.register("transfer") {
    group = "deployment"
    description = "Transfer files to deploy directory"
    
    doLast {
        // Define source and destination paths
        val docsSource = file("build/docs")
        val distSource = file("build/dist")
        val destination = file("../../quandarypeak.github.io/simian")
        val tarGzFile = file("build/dist/$projectName-$buildVersion.tar.gz")
        val md5File = file("build/dist/$projectName-$buildVersion.tar.gz.MD5")
        
        // Check if source directories exist
        if (!docsSource.exists()) {
            throw GradleException("Documentation source directory does not exist: ${docsSource.absolutePath}")
        }
        if (!docsSource.isDirectory) {
            throw GradleException("Documentation source is not a directory: ${docsSource.absolutePath}")
        }
        
        if (!distSource.exists()) {
            throw GradleException("Distribution source directory does not exist: ${distSource.absolutePath}")
        }
        if (!distSource.isDirectory) {
            throw GradleException("Distribution source is not a directory: ${distSource.absolutePath}")
        }
        
        // Check if required distribution files exist
        if (!tarGzFile.exists()) {
            throw GradleException("Distribution tar.gz file does not exist: ${tarGzFile.absolutePath}")
        }
        if (!md5File.exists()) {
            throw GradleException("MD5 checksum file does not exist: ${md5File.absolutePath}")
        }
        
        // Check if destination directory exists, create if necessary
        if (!destination.exists()) {
            println("Destination directory does not exist. Creating: ${destination.absolutePath}")
            if (!destination.mkdirs()) {
                throw GradleException("Failed to create destination directory: ${destination.absolutePath}")
            }
        }
        if (!destination.isDirectory) {
            throw GradleException("Destination is not a directory: ${destination.absolutePath}")
        }
        
        println("Transferring files to: ${destination.absolutePath}")
        
        // Copy documentation files
        copy {
            from(docsSource)
            into(destination)
        }
        println("Documentation files transferred successfully")
        
        // Copy distribution files
        copy {
            from(distSource) {
                include("$projectName-$buildVersion.tar.gz", "$projectName-$buildVersion.tar.gz.MD5")
            }
            into(destination)
        }
        println("Distribution files transferred successfully")
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
