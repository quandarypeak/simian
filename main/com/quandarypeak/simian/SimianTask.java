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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.FileSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Ant task.
 */
public final class SimianTask extends Task {
    private final List<FileSet> _fileSets = new LinkedList<>();
    private final List<Formatter> _formatters = new LinkedList<>();
    private final Options _options = new Options();
    private String _failureProperty;
    private OutputStream _out;

    public void setOutput(final OutputStream out) {
        _out = new IgnoreCloseOutputStream(out);
    }

    public void addFileSet(final FileSet fileSet) {
        Objects.requireNonNull(fileSet, "fileSet");
        _fileSets.add(fileSet);
    }

    public void setFailOnDuplication(final String value) {
        Object parsedValue;
        try {
            parsedValue = Integer.valueOf(value);
        } catch (final NumberFormatException e) {
            parsedValue = Boolean.valueOf(value);
        }

        _options.setOption(Option.FAIL_ON_DUPLICATION, parsedValue);
    }

    public void setFailureProperty(final String failureProperty) {
        _failureProperty = failureProperty;
    }

    public void setThreshold(final int threshold) {
        _options.setThreshold(threshold);
    }

    public void setIgnoreBlocks(final String markers) {
        _options.setOption(Option.IGNORE_BLOCKS, BlockMarkers.valueOf(markers));
    }

    public void setLanguage(final String language) {
        _options.setOption(Option.LANGUAGE, Language.valueOf(language));
    }

    public void setDefaultLanguage(final String language) {
        _options.setOption(Option.DEFAULT_LANGUAGE, Language.valueOf(language));
    }

    public void setIgnoreCurlyBraces(final boolean enable) {
        _options.setOption(Option.IGNORE_CURLY_BRACES, enable);
    }

    public void setIgnoreLiterals(final boolean enable) {
        _options.setOption(Option.IGNORE_LITERALS, enable);
    }

    public void setIgnoreCharacters(final boolean enable) {
        _options.setOption(Option.IGNORE_CHARACTERS, enable);
    }

    public void setIgnoreCharacterCase(final boolean enable) {
        _options.setOption(Option.IGNORE_CHARACTER_CASE, enable);
    }

    public void setIgnoreStrings(final boolean enable) {
        _options.setOption(Option.IGNORE_STRINGS, enable);
    }

    public void setIgnoreStringCase(final boolean enable) {
        _options.setOption(Option.IGNORE_STRING_CASE, enable);
    }

    public void setIgnoreNumbers(final boolean enable) {
        _options.setOption(Option.IGNORE_NUMBERS, enable);
    }

    public void setIgnoreSubtypeNames(final boolean enable) {
        _options.setOption(Option.IGNORE_SUBTYPE_NAMES, enable);
    }

    public void setIgnoreModifiers(final boolean enable) {
        _options.setOption(Option.IGNORE_MODIFIERS, enable);
    }

    public void setBalanceParentheses(final boolean enable) {
        _options.setOption(Option.BALANCE_PARENTHESES, enable);
    }

    public void setBalanceSquareBrackets(final boolean enable) {
        _options.setOption(Option.BALANCE_SQUARE_BRACKETS, enable);
    }

    public void setBalanceCurlyBraces(final boolean enable) {
        _options.setOption(Option.BALANCE_CURLY_BRACES, enable);
    }

    public void setIgnoreRegions(final boolean enable) {
        _options.setOption(Option.IGNORE_REGIONS, enable);
    }

    public void setIgnoreIdentifierCase(final boolean enable) {
        _options.setOption(Option.IGNORE_IDENTIFIER_CASE, enable);
    }

    public void setIgnoreVariableNames(final boolean enable) {
        _options.setOption(Option.IGNORE_VARIABLE_NAMES, enable);
    }

    public void setIgnoreIdentifiers(final boolean enable) {
        _options.setOption(Option.IGNORE_IDENTIFIERS, enable);
    }

    public void setReportDuplicateText(final boolean enable) {
        _options.setOption(Option.REPORT_DUPLICATE_TEXT, enable);
    }

    public void setIgnoreOverlappingBlocks(final boolean enable) {
        _options.setOption(Option.IGNORE_OVERLAPPING_BLOCKS, enable);
    }

    public Formatter createFormatter() {
        return new Formatter();
    }

    public final void addConfiguredFormatter(final Formatter formatter) {
        Objects.requireNonNull(formatter, "formatter");
        _formatters.add(formatter);
    }

    @Override
    public final void execute() throws BuildException {
        final Checker checker = new Checker(createAuditListener(), _options);

        loadFiles(checker);

        if (!checker.check()) {
            if (_failureProperty != null) {
                getProject().setProperty(_failureProperty, Boolean.TRUE.toString());
            } else {
                throw new BuildException("Found duplicate code blocks");
            }
        }
    }

    private AuditListener createAuditListener() {
        if (_formatters.size() < 2) {
            if (_formatters.isEmpty()) {
                addConfiguredFormatter(new Formatter());
            }
            return createSingleAuditListener();
        } else {
            return createCompositeAuditListener();
        }
    }

    private AuditListener createSingleAuditListener() {
        return _formatters.get(0).createAuditListener(this);
    }

    private AuditListener createCompositeAuditListener() {
        final CompositeAuditListener composite = new CompositeAuditListener();

        for (final Formatter formatter : _formatters) {
            composite.add(formatter.createAuditListener(this));
        }

        return composite;
    }

    private void loadFiles(final Checker checker) {
        final FileLoader loader = new FileLoader(new StreamLoader(checker));

        for (final FileSet fileSet : _fileSets) {
            final DirectoryScanner directoryScanner = fileSet.getDirectoryScanner(getProject());
            directoryScanner.scan();

            final String[] filenames = directoryScanner.getIncludedFiles();

            for (final String filename : filenames) {
                loader.load(directoryScanner.getBasedir() + File.separator + filename);
            }
        }
    }

    private OutputStream getDefaultOutputStream() {
        return _out != null ? _out : new LogOutputStream(this, _options.hasOption(Option.FAIL_ON_DUPLICATION) ? Project.MSG_ERR : Project.MSG_WARN);
    }

    /**
     * Configures the type of formatting produced by the Ant task.
     */
    public static final class Formatter {
        private String _type;
        private File _toFile;

        public Formatter() {
            setType(FormatterFactory.PLAIN);
        }

        public final void setType(final String type) {
            Objects.requireNonNull(type, "type");

            _type = type.toLowerCase();

            if (!FormatterFactory.isValidType(_type)) {
                throw new BuildException("Unsupported type: " + type);
            }
        }

        public void setToFile(final File toFile) {
            Objects.requireNonNull(toFile, "toFile");

            _toFile = toFile;
        }

        public final AuditListener createAuditListener(final SimianTask task) {
            return FormatterFactory.createFormatter(_type, getOutputStream(task), true);
        }

        private OutputStream getOutputStream(final SimianTask task) {
            if (_toFile != null) {
                try {
                    return new BufferedOutputStream(new FileOutputStream(_toFile));
                } catch (final FileNotFoundException fnfe) {
                    throw new BuildException(fnfe);
                }
            } else {
                return task.getDefaultOutputStream();
            }
        }
    }
}
