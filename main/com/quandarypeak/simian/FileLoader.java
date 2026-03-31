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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/**
 * Loads files into a checker. Instances can be re-used to load multiple successive files into the same checker.
 */
public final class FileLoader {
    private final StreamLoader _sourceLoader;

    /**
     * Constructor.
     *
     * @param streamLoader The stream to use.
     */
    public FileLoader(final StreamLoader streamLoader) {
        _sourceLoader = Objects.requireNonNull(streamLoader, "streamLoader");
    }

    /**
     * Loads a single file.
     *
     * @param file The file.
     */
    public void load(final File file) {
        try (final Reader reader = new BufferedReader(new UnicodeBOMAwareReader(file))) {
            _sourceLoader.load(file.getCanonicalPath(), reader);
        } catch (final IOException e) {
            _sourceLoader.getAuditListener().error(file, e);
        }
    }

    /**
     * Loads an array of files.
     *
     * @param files The files.
     */
    public void load(final File[] files) {
        for (final File file : files) {
            load(file);
        }
    }

    /**
     * Loads an array of files.
     *
     * @param filenames The file names.
     */
    public void load(final String[] filenames) {
        for (final String filename : filenames) {
            load(filename);
        }
    }

    /**
     * Loads a single file.
     *
     * @param filename The file name.
     */
    public void load(final String filename) {
        load(new File(filename));
    }
}
