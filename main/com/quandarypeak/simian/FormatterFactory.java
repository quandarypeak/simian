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

import java.io.OutputStream;
import java.util.Objects;

final class FormatterFactory {
    public static final String PLAIN = "plain";
    public static final String XML = "xml";
    public static final String EMACS = "emacs";
    public static final String VISUAL_STUDIO = "vs";
    public static final String YAML = "yaml";
    public static final String NULL = "null";

    private FormatterFactory() {
        throw new UnsupportedOperationException("Constructor should not be called");
    }

    public static AuditListener createFormatter(final String type, final OutputStream out, final boolean printBanner) {
        final AuditListener formatter;

        switch (type.toLowerCase()) {
            case PLAIN:
                formatter = new PlainFormatter(out, printBanner);
                break;
            case XML:
                formatter = new XmlFormatter(out, printBanner);
                break;
            case EMACS:
                formatter = new EmacsFormatter(out, printBanner);
                break;
            case VISUAL_STUDIO:
                formatter = new VisualStudioFormatter(out, printBanner);
                break;
            case YAML:
                formatter = new YamlFormatter(out, printBanner);
                break;
            case NULL:
                formatter = NullFormatter.INSTANCE;
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }

        return formatter;
    }

    public static boolean isValidType(final String type) {
        return type.equalsIgnoreCase(PLAIN)
                || type.equalsIgnoreCase(XML)
                || type.equalsIgnoreCase(EMACS)
                || type.equalsIgnoreCase(VISUAL_STUDIO)
                || type.equalsIgnoreCase(YAML)
                || type.equalsIgnoreCase(NULL);
    }
}
