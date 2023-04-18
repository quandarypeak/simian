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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Enumerates the various options that control the behaviour of a {@link Checker}.
 */
public final class Option implements Comparable<Option> {
    private static final Map<String, Option> VALID_OPTIONS_BY_NAME = new HashMap<>();

    static final String PARAMETER_SEP = "=";

    public static final Option LANGUAGE = new Option("language", PARAMETER_SEP + "LANG", "Assumes ALL files are in the specified language");

    public static final Option DEFAULT_LANGUAGE = new Option("defaultLanguage", PARAMETER_SEP + "LANG", "Assumes files are in the specified language if none can be inferred");

    public static final Option THRESHOLD = new Option("threshold", PARAMETER_SEP + "COUNT", "Matches will contain at least the specified number of lines");

    public static final Option FAIL_ON_DUPLICATION = new Option("failOnDuplication", "[+|-|%]", "Exits with a failure return code if duplication detected");

    public static final Option BALANCE_CURLY_BRACES = new Option("balanceCurlyBraces", "[+|-]", "Accounts for curly braces when breaking lines");

    public static final Option BALANCE_PARENTHESES = new Option("balanceParentheses", "[+|-]", "Accounts for parentheses when breaking lines");

    public static final Option BALANCE_SQUARE_BRACKETS = new Option("balanceSquareBrackets", "[+|-]", "Accounts for square brackets when breaking lines");

    public static final Option IGNORE_CURLY_BRACES = new Option("ignoreCurlyBraces", "[+|-]", "Completely ignores curly braces");

    public static final Option IGNORE_MODIFIERS = new Option("ignoreModifiers", "[+|-]", "Ignores modifiers (public, private, static, etc.)");

    public static final Option IGNORE_NUMBERS = new Option("ignoreNumbers", "[+|-]", "Completely ignores numbers");

    public static final Option IGNORE_STRINGS = new Option("ignoreStrings", "[+|-]", "Completely ignores the contents of strings");

    public static final Option IGNORE_STRING_CASE = new Option("ignoreStringCase", "[+|-]", "Matches string literals irrespective of case");

    public static final Option IGNORE_CHARACTER_CASE = new Option("ignoreCharacterCase", "[+|-]", "Matches character literals irrespective of case");

    public static final Option IGNORE_SUBTYPE_NAMES = new Option("ignoreSubtypeNames", "[+|-]", "Matches on similar type names (eg. Reader and FilterReader)");

    public static final Option IGNORE_CHARACTERS = new Option("ignoreCharacters", "[+|-]", "Completely ignores character literals");

    public static final Option IGNORE_LITERALS = new Option("ignoreLiterals", "[+|-]", "Completely ignores all literals (strings, numbers and characters)");

    public static final Option IGNORE_REGIONS = new Option("ignoreRegions", "[+|-]", "Ignores all lines between #region/#endregion");

    public static final Option IGNORE_IDENTIFIER_CASE = new Option("ignoreIdentifierCase", "[+|-]", "Matches identifiers irresepctive of case");

    public static final Option IGNORE_VARIABLE_NAMES = new Option("ignoreVariableNames", "[+|-]", "Completely ignores variable names (fields, parameters and locals)");

    public static final Option IGNORE_IDENTIFIERS = new Option("ignoreIdentifiers", "[+|-]", "Completely ignores identifiers");

    public static final Option IGNORE_BLOCKS = new Option("ignoreBlocks", PARAMETER_SEP + "START:END", "Ignores all lines between START/END", true);

    public static final Option REPORT_DUPLICATE_TEXT = new Option("reportDuplicateText", "[+|-]", "Prints the duplicate text in reports");

    public static final Option IGNORE_OVERLAPPING_BLOCKS = new Option("ignoreOverlappingBlocks", "[+|-]", "Ignores blocks that wholly or partially overlap");

    private final String name;
    private final String parameters;
    private final String description;
    private final boolean multiValued;

    /**
     * Constructor.
     *
     * @param name        The name of the option.
     * @param parameters  Printable description of the possible parameters.
     * @param description Description of the option and how it affects overall behaviour.
     */
    public Option(final String name, final String parameters, final String description) {
        this(name, parameters, description, false);
    }

    /**
     * Constructor.
     *
     * @param name        The name of the option.
     * @param parameters  Printable description of the possible parameters.
     * @param description Description of the option and how it affects overall behaviour.
     * @param multiValued Allow multiple instances.
     */
    public Option(final String name, final String parameters, final String description, final boolean multiValued) {
        this.name = Objects.requireNonNull(name);
        this.parameters = Objects.requireNonNull(parameters);
        this.description = Objects.requireNonNull(description);
        this.multiValued = multiValued;

        VALID_OPTIONS_BY_NAME.put(name, this);
    }

    /**
     * Obtains the name of the option.
     *
     * @return The name of the option.
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains a printable description of the possible parameters to the option.
     *
     * @return A printable description of the possible parameters to the option.
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Obtains a description of the option and how it affects overall behaviour.
     *
     * @return a description of the option and how it affects overall behaviour.
     */
    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(final Option other) {
        Objects.requireNonNull(other, "other");
        return name.compareTo(other.name);
    }

    /**
     * Obtains the set of all valid options.
     *
     * @return The valid options.
     */
    public static SortedSet<Option> values() {
        return new TreeSet<>(VALID_OPTIONS_BY_NAME.values());
    }

    /**
     * Determines if a specified option is valid or not.
     *
     * @param name The name of the option to check for.
     * @return <code>true</code> if the specified option is valid; otherwise <code>false</code>.
     */
    public static boolean isValidOption(final String name) {
        return VALID_OPTIONS_BY_NAME.containsKey(name);
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    /**
     * Obtains an option by name.
     *
     * @param name The name of the option.
     * @return The option identified by the specified name.
     */
    public static Option valueOf(final String name) {
        final Option option = VALID_OPTIONS_BY_NAME.get(name);
        Objects.requireNonNull(option, "invalid name");
        return option;
    }

    public String toString() {
        return name;
    }
}
