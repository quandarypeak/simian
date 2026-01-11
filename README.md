<a name="readme-top"></a>

# Simian
Simian Similarity Analyzer identifies duplication in Java, C#, C, C++, SQL, COBOL, Ruby, JSP, ASP, HTML, XML, Visual Basic, Groovy source code and even plain text files. In fact, simian can be used on any human readable files such as ini files, deployment descriptors, you name it.

Visit https://simian.quandarypeak.com for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Build
Simian can be built with a recent version of Java (tested with JDK 21). However, the produced jar
can be used with any JDK 8 or newer.

In order to build Simian, run [Gradle](https://gradle.org/).

For example, run `gradlew jar` to create a JAR package.

There are other tasks: dist, build, and deploy.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Command Line Interface
Simian's command line interface allows you to run it from a shell, shell script, or batch file, scanning a directory for all files matching a pattern.

The general form for the Java version is: java -jar simian.jar [options] [files]

The files can be specified as any regular shell glob or simply a list of files and can be mixed with the -includes option. (See below for examples.)

For example, to find all java files in all sub-directories of the current directory: "**/*.java"

To find all java files in the current directory and set the threshold to 3: -threshold=3 "*.java"

To find all C# files in the current directory: "*.cs"

To find all C and header in all sub-directories of the current directory: **/*.c **/*.h

To find all java files in two different directories: "/dir1/*.java" "/dir2/*.java"

To find all java files in all sub-directories, excluding Test classes: -includes=**/*.java -excludes=**/*Test.java

To find all java files in the current directory and ignore numbers: -ignoreNumbers "*.java"

To find all Ruby files and display the results in xml format: -formatter=xml "*.rb"

To find all Ruby files and sends the results in emacs compatible format to a file: -formatter=emacs:c:\temp\simian.log "*.rb"

To read configuration from a file (where each line of the file specifies at most one of any of the valid command-line arguments): -config=simian.config

Notes
The default VM size seems to be adequate for most projects. If you encounter the following error you will need to increase the VM heap size using the -mx JVM option.

Exception in thread "main" java.lang.OutOfMemoryError

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Gradle Task
This method allows you to integrate Simian with Gradle Build Tool, an open source build system for Java, Android, and Kotlin development environments.

The following examples show neccessary steps to use Simian for checking code duplication in a Java project.
Note: Adjust the path of Simian jar to suit your environment. It is recommended to host the jar in a Maven repo instead of a local folder shown here.

Add Simian to the list of dependencies in your build.gradle.kts file (or similar like pom.xml):

```
dependencies {
    ...
    implementation("simian:simian:4.0.0")
    ...
}
```

Define a simianCheck task to run the checker. For all defaults:
```
tasks.register<JavaExec>("simianCheck") {

    val inclFiles = sourceSets["main"].java.srcDirs.map { "-includes=${it.path}/**/*.java" }

    mainClass.set("-jar")
    args(layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
         *inclFiles.toTypedArray(),
    )
}
```

To exclude test classes if they exists in the same tree as the source:
```
    val exclFiles = sourceSets["main"].java.srcDirs.map { "-excludes=${it.path}/**/*Test.java" }
    args(layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
         *inclFiles.toTypedArray(),
         *exclFiles.toTypedArray(),
    )
```

To change the minimum number of lines that is considered a match:
```
    ...
    args(layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
         *inclFiles.toTypedArray(),
         "-threshold=6",
    )
```

To force the language used for processing:
```
    ...
    args(layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
         "-includes=path/to/sources/**/*.*",
         "-language=java",
    )
```

To have the build fail one or more matches are found:
```
    ...
    args(layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
         "-includes=path/to/sources/**/*.*",
         "-failureProperty=test.failure",
    )
```

By default, Simian produces output in plain text. You can override this by using the nested formatter element. The formatter takes a type (either "plain"; "xml"; "emacs"; "vs"; or "yaml") and an optional filename (toFile). For example, to send output to a file:
```
    ...
    args(layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
         "-includes=path/to/sources/**/*.*",
         "-formatter=plain:simian-log.txt",
    )
```

To produce XML output:
```
    ...
    args(layout.projectDirectory.file("libs/simian-4.0.0.jar").asFile.absolutePath,
         "-includes=path/to/sources/**/*.*",
         "-formatter=xml:simian-log.xml",
    )
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Simian Processing Options
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

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Simian recognizes the following file extensions:
| Language               | Extensions                                   |
|------------------------|----------------------------------------------|
| Java                   | java                                         |
| C Sharp                | cs, c#, csharp                               |
| C                      | c, h, m                                      |
| C++                    | cpp, hpp, cplusplus, inl                     |
| Ruby                   | rb, ruby                                     |
| COBOL                  | cobol                                        |
| ABAP                   | abap                                         |
| XML                    | xml, xsl, xsd                                |
| Jakarta Server Pages   | jsp                                          |
| ASP                    | asp                                          |
| JavaScript             | js, javascript                               |
| HTML                   | html, htm                                    |
| Visual Basic           | vb, bas, cls, frm                            |
| Lisp                   | lisp, lsp                                    |
| Groovy                 | groovy                                       |
| Plain Text             | default when language cannot be determined   |


<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- LICENSE -->
## License
Simian has its own licensing model described in the [LICENSE.txt](LICENSE.txt) file.

For OSS Notice, refer to the [OSS Notice](docs/OSS_Notice.md) document.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Contributing
You are welcome to make contributions to the Simian source code. 

Contact simian@quandarypeak.com for any contribution requests.

Please be mindful of the Simian license which limits distribution of Simian.

### Git Branching
Follow [Git Flow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).

### Java Coding Style
[*Best effort*] Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact
Simian development support: simian@quandarypeak.com

<p align="right">(<a href="#readme-top">back to top</a>)</p>
