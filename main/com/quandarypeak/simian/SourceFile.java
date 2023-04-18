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

import java.util.Objects;

/**
 * Represents a source file that was processed.
 */
public final class SourceFile {
    private final String _filename;
    private int _rawLineCount;
    private int _significantLineCount;
    private boolean _audited;

    public SourceFile(final String filename) {
        Objects.requireNonNull(filename, "filename");

        _filename = filename;
        _rawLineCount = -1;
        _significantLineCount = -1;
        _audited = false;
    }

    /**
     * Obtains the name of the file.
     *
     * @return The file name.
     */
    public String getFilename() {
        return _filename;
    }

    /**
     * Obtains the extension (eg "java", "cs", "cpp", etc).
     *
     * @return The extension.
     */
    String getExtension() {
        final int index = _filename.lastIndexOf('.');
        return index != -1 ? _filename.substring(index + 1).toLowerCase() : "";
    }

    boolean isLoaded() {
        return _rawLineCount != -1;
    }

    void setLineCounts(final int rawLineCount, final int significantLineCount) {
        _rawLineCount = rawLineCount;
        _significantLineCount = significantLineCount;
    }

    /**
     * Obtains the number of lines in the file including comments, ignored lines, etc.
     *
     * @return The raw line count.
     */
    public int getRawLineCount() {
        return _rawLineCount;
    }

    /**
     * Obtains the number of lines in the file excluding comments, ignored lines, etc.
     *
     * @return The significant line count.
     */
    public int getSignificantLineCount() {
        return _significantLineCount;
    }

    void markAudited() {
        _audited = true;
    }

    /**
     * Determines if this file has been presented to an audit listener.
     *
     * @return <code>true</code> if the file has already been audited; otherwise <code>false</code>.
     */
    boolean isAudited() {
        return _audited;
    }
}
