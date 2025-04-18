/*
 * Simian Similarity Analyzer
 * 
 * Copyright (c) 2023 Quandary Peak Research.
 * Original authorship by Simon Harris.
 * 
 * Use of this software is permitted for educational or academic research
 * purposes only and is subject to the Quandary Peak Academic Software License.
 * See docs/license.txt for details.
 * 
 * Redistribution of this software in source or binary form is not permitted.
 * 
 * For non-academic or commercial use, please contact simian@quandarypeak.com.
 */
 
package com.quandarypeak.simian;

import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Enumerates the various languages that control the behaviour of a {@link Checker}.
 */
public final class Language implements Comparable<Language> {
    private static final Map<String, Language> VALID_LANGUAGES_BY_EXTENSION = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * The Java language.
     */
    public static final Language JAVA = new Language("Java", new JavaParserFactory(), "java");

    /**
     * The Groovy language.
     */
    public static final Language GROOVY = new Language("Groovy", new GroovyParserFactory(), "groovy");

    /**
     * The JavaScript (ECMAScript) language.
     */
    public static final Language JAVA_SCRIPT = new Language("JavaScript", new JavaScriptParserFactory(), "javascript", "js");

    /**
     * The TypeScript language.
     */
    public static final Language TYPESCRIPT = new Language("TypeScript", new TypeScriptParserFactory(), "typescript", "ts");

    /**
     * The C# language.
     */
    public static final Language C_SHARP = new Language("C#", new CSharpParserFactory(), "c#", "cs", "csharp");

    /**
     * The C++ language.
     */
    public static final Language CPP = new Language("C++", new CppParserFactory(), "c++", "h++", "cpp", "hpp", "cplusplus", "inl", "mm");

    /**
     * The C language.
     */
    public static final Language C = new Language("C", new CParserFactory(), "c", "h", "m");

    /**
     * The COBOL/ABAP language.
     */
    public static final Language COBOL_ABAP = new Language("COBOL/ABAP", new CobolParserFactory(), "cobol", "abap");

    /**
     * The Ruby language.
     */
    public static final Language RUBY = new Language("Ruby", new RubyParserFactory(), "rb", "ruby", "rjs", "rake", "gemspec");

    /**
     * The eXtensible Markup Language.
     */
    public static final Language XML = new Language("XML", new DefaultParserFactory(), "xml", "xsl", "xslt", "xsd", "jspx", "tagx", "tld");

    /**
     * The HyperText Markup Language.
     */
    public static final Language HTML = new Language("HTML", new DefaultParserFactory(), "html", "htm", "shtml", "xhtml", "sht", "shtm");

    /**
     * The Java Server Pages Language.
     */
    public static final Language JSP = new Language("JSP", new DefaultParserFactory(), "jsp", "jsf", "jspf", "tag", "tagf");

    /**
     * The Active Server Pages Language.
     */
    public static final Language ASP = new Language("ASP", new DefaultParserFactory(), "asp");

    /**
     * The Visual Basic Language
     */
    public static final Language VB = new Language("VB", new VbParserFactory(), "vb", "frm", "bas", "cls");

    /**
     * The Structured Query Language.
     */
    public static final Language SQL = new Language("SQL", new SqlParserFactory(), "sql");

    /**
     * IBM System/360 Family Assembler Language.
     */
    public static final Language ASM390 = new Language("ASM390", new IbmSystem390AssemblerParserFactory(), "asm390");

    /**
     * Text files.
     */
    public static final Language TEXT = new Language("Text", new DefaultParserFactory(), "txt", "text");

    /**
     * The Swift language.
     */
    public static final Language SWIFT = new Language("Swift", new DefaultParserFactory(), "swift");

    /**
     * The Python language.
     */
    public static final Language PYTHON = new Language("Python", new PythonParserFactory(), "py");

    /* The default language. */
    public static final Language DEFAULT = TEXT;

    private final String name;
    private final ParserFactory parserFactory;

    /**
     * Constructor.
     *
     * @param name          The name of the language.
     * @param parserFactory The parser factory class.
     * @param extensions    The extensions supported by this language.
     */
    private Language(final String name, final ParserFactory parserFactory, final String... extensions) {
        Objects.requireNonNull(extensions);

        this.name = Objects.requireNonNull(name);
        this.parserFactory = Objects.requireNonNull(parserFactory);

        for (final String extension : extensions) {
            Objects.requireNonNull(extension, "extension");
            if (VALID_LANGUAGES_BY_EXTENSION.put(extension, this) != null) {
                throw new IllegalArgumentException(String.format("Duplicate extension: %s", extension));
            }
        }
    }

    /**
     * Obtains the name of the language.
     *
     * @return The name of the language.
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains the parser factory for the language.
     *
     * @return The parser factory.
     */
    ParserFactory getParserFactory() {
        return parserFactory;
    }

    @Override
    public int compareTo(final Language other) {
        Objects.requireNonNull(other);
        return name.compareTo(other.name);
    }

    /**
     * Obtains the set of all valid options.
     *
     * @return The valid options.
     */
    public static SortedSet<Language> values() {
        return new TreeSet<>(VALID_LANGUAGES_BY_EXTENSION.values());
    }

    /**
     * Determines if a specified language is valid or not.
     *
     * @param extension The extension of the language to check for.
     * @return <code>true</code> if the specified language is valid; otherwise <code>false</code>.
     */
    public static boolean isValidLanguage(final String extension) {
        return VALID_LANGUAGES_BY_EXTENSION.containsKey(extension);
    }

    /**
     * Obtains a language by extension.
     *
     * @param extension The extension of the file.
     * @return The language identified by the specified extension.
     */
    public static Language valueOf(final String extension) {
        final Language language = VALID_LANGUAGES_BY_EXTENSION.get(extension);
        Objects.requireNonNull(language);
        return language;
    }

    @Override
    public String toString() {
        return name;
    }
}
