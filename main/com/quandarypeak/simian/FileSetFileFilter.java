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
