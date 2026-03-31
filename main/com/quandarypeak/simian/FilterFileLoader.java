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
