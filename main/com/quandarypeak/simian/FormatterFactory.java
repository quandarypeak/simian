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
