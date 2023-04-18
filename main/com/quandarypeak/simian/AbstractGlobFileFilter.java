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

abstract class AbstractGlobFileFilter implements FileFilter {
    private final Glob glob;

    AbstractGlobFileFilter(final Glob glob) {
        Objects.requireNonNull(glob, "glob");
        this.glob = glob;
    }

    public final File getBaseDirectory() {
        return glob.getBaseDirectory();
    }

    protected final boolean accept(final String path, final boolean directory) {
        return glob.accept(path, directory);
    }

    public final String toString() {
        return glob.toString();
    }
}
