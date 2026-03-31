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
