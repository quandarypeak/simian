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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Provides file path filtering based on standard file globbing patterns.
 */
final class Glob {
    // ******** This really should be [\\\\/] but for a bug? in IKVM so we worked around it :) ********
    private static final String SLASH = "(\\\\|/)";

    private final File _baseDirectory;
    private final Pattern _pattern;
    private final boolean _recursive;

    Glob(final String glob) {
        Objects.requireNonNull(glob, "glob");

        boolean recursive = false;

        final StringBuilder pattern = new StringBuilder();

        boolean lookingForBaseDirectory = true;
        int baseDirectoryEnd = -1;

        pattern.append('^');

        final CharacterIterator i = new StringCharacterIterator(glob);
        for (char c = i.first(); c != CharacterIterator.DONE; c = i.next()) {
            switch (c) {
                case '?':
                    pattern.append("[^\\\\/]");
                    lookingForBaseDirectory = false;
                    break;
                case '*':
                    if (i.next() == '*') {
                        recursive = true;
                        pattern.append(".*");
                    } else {
                        i.previous();
                        pattern.append("[^\\\\/]*");
                    }
                    lookingForBaseDirectory = false;
                    break;
                case '\\':
                case '/':
                    if (lookingForBaseDirectory) {
                        baseDirectoryEnd = i.getIndex();
                    }
                    pattern.append(SLASH);
                    break;
                case '(':
                case ')':
                case '{':
                case '}':
                case '.':
                case '^':
                case '$':
                case '[':
                case ']':
                case '+':
                case '|':
                    pattern.append('\\');
                    pattern.append(c);
                    break;
                default:
                    pattern.append(c);
            }
        }

        pattern.append('$');

        _baseDirectory = new File(glob.substring(0, baseDirectoryEnd + 1));
        _pattern = Pattern.compile(pattern.toString(), Pattern.CASE_INSENSITIVE);
        _recursive = recursive;
    }

    public boolean accept(final String path, final boolean directory) {
        Objects.requireNonNull(path, "path");
        return _pattern.matcher(path).matches() || directory && _recursive;
    }

    public File getBaseDirectory() {
        return _baseDirectory;
    }
}
