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
import java.io.IOException;
import java.util.Objects;

final class FileSetFileFilter implements FileFilter {
    private final FileFilter _includes;
    private final FileFilter _excludes;

    FileSetFileFilter(final FileFilter includes, final FileFilter excludes) {
        _includes = Objects.requireNonNull(includes, "includes");
        _excludes = Objects.requireNonNull(excludes, "excludes");
    }

    @Override
    public final boolean accept(final File file) {
        return !excludes(file) && includes(file);
    }

    private boolean includes(final File file) {
        return _includes.accept(file);
    }

    private boolean excludes(final File file) {
        try {
            return _excludes.accept(file) || _excludes.accept(file.getCanonicalFile());
        } catch (final IOException e) {
            return true;
        }
    }
}
