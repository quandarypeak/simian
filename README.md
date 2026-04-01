<a name="readme-top"></a>

# Simian Similarity Analyzer

Simian detects duplicate code across large codebases in seconds. It is a fast, language-agnostic similarity analyzer that scans source files and reports duplicated blocks of code.

Simian works across a wide range of languages including Java, C#, C, C++, SQL, COBOL, Ruby, JSP, ASP, HTML, XML, Visual Basic, Groovy, and even plain text. It can be used on any human-readable files such as configuration files, deployment descriptors, and more.

More information: https://simian.quandarypeak.com

---

## Quick Start

```bash
java -jar simian.jar "**/*.java"
```

Example with options:

```bash
java -jar simian.jar -threshold=3 "**/*.java"
```

---

## Features

- Language-agnostic duplication detection
- Works on any human-readable file
- Fast analysis for large codebases
- Flexible CLI with powerful filtering options
- Multiple output formats (plain, XML, YAML, etc.)

---

## Installation & Build

### Requirements
- JDK 8+ (tested with JDK 21)

### Build with Gradle

```bash
./gradlew jar
```

Other available tasks:
- `build`
- `dist`
- `deploy`

The generated JAR can be run on any JDK 8 or newer.

---

## Command Line Usage

```bash
java -jar simian.jar [options] [files]
```

### Common Examples

Find all Java files recursively:

```bash
"**/*.java"
```

Find Java files with a lower duplication threshold:

```bash
-threshold=3 "*.java"
```

Scan multiple directories:

```bash
"/dir1/*.java" "/dir2/*.java"
```

Exclude test files:

```bash
-includes=**/*.java -excludes=**/*Test.java
```

Ignore numeric differences:

```bash
-ignoreNumbers "*.java"
```

Output results as XML:

```bash
-formatter=xml "*.rb"
```

Write output to file:

```bash
-formatter=emacs:c:\temp\simian.log "*.rb"
```

Use config file:

```bash
-config=simian.config
```

### Memory Issues

If you encounter:

```
java.lang.OutOfMemoryError
```

Increase heap size:

```bash
java -Xmx1024m -jar simian.jar ...
```

---

## Gradle Integration

Add dependency:

```kotlin
dependencies {
    implementation("simian:simian:4.0.0")
}
```

### Example Task

```kotlin
tasks.register<JavaExec>("simianCheck") {
    val inclFiles = sourceSets["main"].java.srcDirs.map { "-includes=${it.path}/**/*.java" }

    mainClass.set("-jar")
    args(
        layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
        *inclFiles.toTypedArray()
    )
}
```

### Common Customizations

Exclude test files:

```kotlin
val exclFiles = sourceSets["main"].java.srcDirs.map { "-excludes=${it.path}/**/*Test.java" }
```

Change duplication threshold:

```kotlin
"-threshold=6"
```

Force language:

```kotlin
"-language=java"
```

Fail build on duplication:

```kotlin
"-failureProperty=test.failure"
```

Output to file:

```kotlin
"-formatter=plain:simian-log.txt"
```

---

## Simian Configuration Options
| Option                | Languages                                              | Default | Possible values                                                                                                           | Description                                                                                                                                       |
|-----------------------|--------------------------------------------------------|---------|---------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| formatter             | all                                                    | none    | plain, xml, emacs, vs (visual studio), yaml, null                                                                         | Specifies the format in which processing results will be produced.                                                                                 |
| threshold             | all                                                    | 6       | integer >= 2                                                                                                              | Matches will contain at least the specified number of lines.                                                                                       |
| language              | n/a                                                    | none    | java, c#, cs, csharp, c, c++, cpp, cplusplus, js, javascript, cobol, abap, rb, ruby, vb, jsp, html, xml, groovy, asm390   | Assumes all files are in the specified language.                                                                                                   |
| defaultLanguage       | n/a                                                    | none    | java, c#, cs, csharp, c, c++, cpp, cplusplus, js, javascript, cobol, abap, rb, ruby, vb, jsp, html, xml, groovy, asm390   | Assumes files are in the specified language if none can be inferred.                                                                              |
| failOnDuplication     | all                                                    | true    | boolean                                                                                                                   | Causes the checker to fail the current process if duplication is detected.                                                                         |
| reportDuplicateText   | all                                                    | false   | boolean                                                                                                                   | Prints the duplicate text in reports.                                                                                                              |
| ignoreBlocks          | all                                                    | none    | string                                                                                                                    | Ignores all lines between specified START/END markers.                                                                                             |
| ignoreCurlyBraces     | Java, C#, C, C++, JavaScript, Ruby, Groovy             | false   | boolean                                                                                                                   | Curly braces are ignored.                                                                                                                          |
| ignoreIdentifiers     | Java, C#, C, C++, JavaScript, COBOL, Ruby, Groovy      | false   | boolean                                                                                                                   | Completely ignores all identifiers.                                                                                                                |
| ignoreIdentifierCase  | Java, C#, C, C++, JavaScript, COBOL, Ruby, Groovy      | true    | boolean                                                                                                                   | Matches identifiers irrespective of case, e.g. `MyVariableName` and `myvariablename` would both match.                                             |
| ignoreRegions         | C#                                                     | false   | boolean                                                                                                                   | Ignore lines between `#region`/`#endregion`.                                                                                                       |
| ignoreStrings         | Java, C#, C, C++, JavaScript, COBOL, Ruby, SQL, Groovy | false   | boolean                                                                                                                   | `MyVariable` and `myvariable` would both match.                                                                                                    |
| ignoreStringCase      | Java, C#, C, C++, JavaScript, COBOL, Ruby, SQL, Groovy | true    | boolean                                                                                                                   | `"Hello, World"` and `"HELLO, WORLD"` would both match.                                                                                            |
| ignoreNumbers         | Java, C#, C, C++, JavaScript, COBOL, Ruby, SQL, Groovy | false   | boolean                                                                                                                   | `int x = 1;` and `int x = 576;` would both match.                                                                                                  |
| ignoreCharacters      | Java, C#, C, C++, JavaScript, COBOL, Ruby, Groovy      | false   | boolean                                                                                                                   | `'A'` and `'Z'` would both match.                                                                                                                  |
| ignoreCharacterCase   | Java, C#, C, C++, JavaScript, COBOL, Ruby, Groovy      | true    | boolean                                                                                                                   | `'A'` and `'a'` would both match.                                                                                                                  |
| ignoreLiterals        | Java, C#, C, C++, JavaScript, COBOL, Ruby, SQL, Groovy | false   | boolean                                                                                                                   | `'A'`, `"one"` and `27.8` would all match.                                                                                                         |
| ignoreSubtypeNames    | Java, C, Groovy                                       | false   | boolean                                                                                                                   | `BufferedReader`, `StringReader` and `Reader` would all match.                                                                                     |
| ignoreModifiers       | Java, C#, C, C++, JavaScript, Groovy                   | true    | boolean                                                                                                                   | Ignores modifiers such as `public`, `protected`, `static`, etc.                                                                                    |
| ignoreVariableNames   | Java, C, Groovy                                       | false   | boolean                                                                                                                   | Completely ignores variable names (field, parameter and local), e.g. `int foo = 1;` and `int bar = 1` would both match.                            |
| balanceParentheses    | Java, C#, C, C++, JavaScript, COBOL, Ruby, SQL, Groovy | false   | boolean                                                                                                                   | Ensures that expressions inside parentheses split across multiple physical lines are considered as one.                                            |
| balanceCurlyBraces    | Ruby                                                  | false   | boolean                                                                                                                   | Ensures that expressions inside curly braces split across multiple physical lines are considered as one.                                           |
| balanceSquareBrackets | Java, C#, C, C++, JavaScript, Ruby, Groovy            | false   | boolean                                                                                                                   | Ensures that expressions inside square brackets split across multiple physical lines are considered as one; defaults to false.                      |

---

## Supported Languages

Simian recognizes file extensions for:

- Java, JSP
- C, C++, C#
- Ruby
- COBOL
- ABAP
- ASP
- Python
- PHP
- JavaScript, Typescript
- XML, HTML, CSS
- Sass, Less
- Markdown
- Visual Basic
- Groovy
- Plain text (fallback)

---

## License
Simian Similarity Analyzer is licensed under the [Apache Software License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).

---

## Contributing

Contributions are welcome.

- Submit a pull request
- Follow [Git Flow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

Contact: simian@quandarypeak.com
