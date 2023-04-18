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
import java.util.Objects;

final class IncludesFileFilter extends AbstractGlobFileFilter {
    IncludesFileFilter(final Glob glob) {
        super(glob);
    }

    @Override
    public boolean accept(final File file) {
        Objects.requireNonNull(file);
        return accept(file.getPath(), file.isDirectory());
    }
}
