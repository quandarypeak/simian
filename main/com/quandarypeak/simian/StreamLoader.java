/*
 * Copyright 2022-2026 Quandary Peak Research, Inc.
 * Original authorship by Simon Harris.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
