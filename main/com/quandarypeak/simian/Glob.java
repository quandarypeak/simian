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
