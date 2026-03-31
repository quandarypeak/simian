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
