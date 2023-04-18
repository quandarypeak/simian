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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads files into a checker. Instances can be re-used to load multiple successive files into the same checker.
 */
public final class StreamLoader {
    private final Listener listener = new Listener();
    private final Map<Language, Parser> parsers = new HashMap<>();
    private final Checker checker;
    private final BoundedFifoBuffer<BlockBuilder> buffer;
    private SourceFile sourceFile;
    private Block currentBlock;
    private int significantLineCount;

    /**
     * Constructor.
     *
     * @param checker The checker to use.
     */
    public StreamLoader(final Checker checker) {
        Objects.requireNonNull(checker, "checker");
        this.checker = checker;
        this.buffer = new BoundedFifoBuffer<>(Math.max(this.checker.getOptions().getThreshold() - 1, 1));
    }

    /**
     * Loads a specified stream.
     *
     * @param filename The name of the file this stream represents.
     * @param stream   The stream.
     * @throws IOException if an error occurs loading the file.
     */
    public void load(final String filename, final Reader stream) throws IOException {
        this.sourceFile = checker.getSourceFile(filename);
        if (sourceFile.isLoaded()) {
            return;
        }

        this.significantLineCount = 0;
        try {
            final Parser parser = getParser(getLanguage());
            final int rawLineCount = parser.parse(stream);
            sourceFile.setLineCounts(rawLineCount, significantLineCount);
            checker.getAuditListener().fileProcessed(sourceFile);
        } finally {
            buffer.clear();
            this.sourceFile = null;
            this.currentBlock = null;
        }
    }

    /**
     * Loads a specified stream.
     *
     * @param filename The name of the file this stream represents.
     * @param stream   The stream.
     * @throws IOException if an error occurs loading the file.
     */
    public void load(final String filename, final InputStream stream) throws IOException {
        load(filename, new InputStreamReader(stream));
    }

    private Language getLanguage() {
        final Language language = checker.getOptions().getOption(Option.LANGUAGE);
        return language != null ? language : getInferredLanguage(sourceFile.getExtension());
    }

    private Language getInferredLanguage(final String extension) {
        return Language.isValidLanguage(extension) ? Language.valueOf(extension) : getDefaultLanguage();
    }

    private Language getDefaultLanguage() {
        final Language language = checker.getOptions().getOption(Option.DEFAULT_LANGUAGE);
        return language != null ? language : Language.DEFAULT;
    }

    private Parser getParser(final Language language) {
        return parsers.computeIfAbsent(language, l -> l.getParserFactory().createParser(listener, checker.getOptions()));
    }

    public AuditListener getAuditListener() {
        return checker.getAuditListener();
    }

    private final class Listener implements LineListener {
        @Override
        public void file() {
        }

        @Override
        public void line(final int lineNumber, final LineBuffer line) {
            ++significantLineCount;

            for (final BlockBuilder blockBuilder : buffer) {
                blockBuilder.addLine(lineNumber, line);
            }

            final BlockBuilder builder;

            if (buffer.size() == buffer.capacity()) {
                builder = buffer.remove();
                builder.build();
            } else {
                builder = new BlockBuilder();
            }

            builder.reset(lineNumber, line);

            buffer.add(builder);
        }
    }

    private final class BlockBuilder {
        private int start;
        private int end;
        private long blockHash;

        public void reset(final int lineNumber, final LineBuffer line) {
            this.start = lineNumber;
            this.end = lineNumber;
            this.blockHash = 0L;
            addLine(lineNumber, line);
        }

        public void addLine(final int lineNumber, final LineBuffer line) {
            this.blockHash = Hasher.hash(blockHash, line.getLineHash());
            this.end = lineNumber;
        }

        public void build() {
            currentBlock = checker.addBlock(sourceFile, start, end, blockHash, currentBlock);
        }
    }
}
