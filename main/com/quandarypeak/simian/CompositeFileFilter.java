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
import java.util.Collection;
import java.util.Objects;

final class CompositeFileFilter implements FileFilter {
    private final Collection<? extends FileFilter> _filters;

    CompositeFileFilter(final Collection<? extends FileFilter> filters) {
        _filters = Objects.requireNonNull(filters, "filters");
    }

    @Override
    public final boolean accept(final File file) {
        for (final FileFilter _filter : _filters) {
            if (_filter.accept(file)) {
                return true;
            }
        }
        return false;
    }
}
