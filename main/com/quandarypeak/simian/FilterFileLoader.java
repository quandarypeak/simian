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

import java.io.File;
import java.io.FileFilter;
import java.util.Objects;

final class FilterFileLoader {
    private final File _baseDirectory;
    private final FileFilter _filter;
    private final FileLoader _loader;

    FilterFileLoader(final File baseDirectory, final FileFilter fileFilter, final FileLoader fileLoader) {
        _baseDirectory = Objects.requireNonNull(baseDirectory, "baseDirectory");
        _filter = Objects.requireNonNull(fileFilter, "fileFilter");
        _loader = Objects.requireNonNull(fileLoader, "fileLoader");
    }

    /**
     * Load the files.
     */
    public final void load() {
        loadDirectory(_baseDirectory);
    }

    private void loadDirectory(final File directory) {
        final String[] filenames = directory.list();
        // This check is necessary because a file I/O error (such as no permissions) will cause list() to
        // return NULL!!!
        if (filenames == null) {
            return;
        }

        for (final String filename : filenames) {
            final File file = new File(directory, filename);
            if (_filter.accept(file)) {
                loadDirectoryOrFile(file);
            }
        }
    }

    private void loadDirectoryOrFile(final File directoryOrFile) {
        if (directoryOrFile.isDirectory()) {
            loadDirectory(directoryOrFile);
        } else {
            _loader.load(directoryOrFile);
        }
    }
}
