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

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Checkstyle Plugin.
 */
public final class SimianCheck extends AbstractFileSetCheck {
    private final Options _options = new Options();
    private Checker _checker;
    private FileLoader _loader;

    public SimianCheck() {
        setFailOnDuplication(true);
    }

    public void setFailOnDuplication(final boolean enable) {
        _options.setOption(Option.FAIL_ON_DUPLICATION, enable);
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

    @Override
    public final void beginProcessing(final String charset) {
        super.beginProcessing(charset);

        setSeverity((_options.hasOption(Option.FAIL_ON_DUPLICATION) ? SeverityLevel.ERROR : SeverityLevel.WARNING).getName());

        _checker = new Checker(new Logger(), _options);
        _loader = new FileLoader(new StreamLoader(_checker));
    }

    @Override
    protected final void processFiltered(final File file, final List<String> lines) {
        _loader.load(file);
    }

    @Override
    public final void finishProcessing() {
        _checker.check();
        _checker = null;
        _loader = null;
        super.finishProcessing();
    }

    private final class Logger implements AuditListener {
        private final MessageDispatcher _dispatcher = getMessageDispatcher();

        private Integer _lineCount;
        private int _startLineNumber;
        private Integer _endLineNumber;
        private String _filename;

        @Override
        public void startCheck(final Options options) {
            getMessageCollector().reset();
        }

        @Override
        public void fileProcessed(final SourceFile sourceFile) {
        }

        @Override
        public void startSet(final int lineCount, final String fingerprint) {
            if (_lineCount == null || _lineCount != lineCount) {
                _lineCount = lineCount;
            }
            _filename = null;
        }

        @Override
        public void block(final Block block) {
            Objects.requireNonNull(block, "block");

            final SourceFile sourceFile = block.getSourceFile();
            final int startLineNumber = block.getStartLineNumber();
            final int endLineNumber = block.getEndLineNumber();

            if (_filename != null) {
                log(_startLineNumber, "check", _lineCount, _endLineNumber, sourceFile.getFilename(), startLineNumber, endLineNumber);
                return;
            }

            _filename = sourceFile.getFilename();
            _startLineNumber = startLineNumber;
            _endLineNumber = endLineNumber;
            _dispatcher.fireFileStarted(_filename);
        }

        @Override
        public void endSet(final String text) {
            fireErrors(_filename);
            _dispatcher.fireFileFinished(_filename);
        }

        @Override
        public void endCheck(final CheckSummary summary) {
        }

        @Override
        public void error(final File file, final Throwable e) {
            System.err.println("Error processing file " + file.getPath() + ": " + e.getMessage());
        }
    }
}
