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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/**
 * Loads files into a checker. Instances can be re-used to load multiple successive files into the same checker.
 */
public final class FileLoader {
    private final StreamLoader _sourceLoader;

    /**
     * Constructor.
     *
     * @param streamLoader The stream to use.
     */
    public FileLoader(final StreamLoader streamLoader) {
        _sourceLoader = Objects.requireNonNull(streamLoader, "streamLoader");
    }

    /**
     * Loads a single file.
     *
     * @param file The file.
     */
    public void load(final File file) {
        try (final Reader reader = new BufferedReader(new UnicodeBOMAwareReader(file))) {
            _sourceLoader.load(file.getCanonicalPath(), reader);
        } catch (final IOException e) {
            _sourceLoader.getAuditListener().error(file, e);
        }
    }

    /**
     * Loads an array of files.
     *
     * @param files The files.
     */
    public void load(final File[] files) {
        for (final File file : files) {
            load(file);
        }
    }

    /**
     * Loads an array of files.
     *
     * @param filenames The file names.
     */
    public void load(final String[] filenames) {
        for (final String filename : filenames) {
            load(filename);
        }
    }

    /**
     * Loads a single file.
     *
     * @param filename The file name.
     */
    public void load(final String filename) {
        load(new File(filename));
    }
}
