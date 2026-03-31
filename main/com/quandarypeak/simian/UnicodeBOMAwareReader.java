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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;

final class UnicodeBOMAwareReader extends InputStreamReader {
    UnicodeBOMAwareReader(final String filename) throws IOException {
        this(new FileInputStream(filename));
    }

    UnicodeBOMAwareReader(final File file) throws IOException {
        this(new FileInputStream(file));
    }

    UnicodeBOMAwareReader(final InputStream inputStream) throws IOException {
        this(new PushbackInputStream(inputStream, 4));
    }

    private UnicodeBOMAwareReader(final PushbackInputStream inputStream) throws IOException {
        super(inputStream, getEncoding(inputStream));
    }

    private static String getEncoding(final PushbackInputStream inputStream) throws IOException {
        final String encoding;
        final byte[] bom = new byte[4];
        final int read = inputStream.read(bom, 0, 4);
        int skip;

        switch (read) {
            case 4:
                if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == (byte) 0x00 && bom[3] == (byte) 0x00) {
                    encoding = "UTF-32LE";
                    skip = 4;
                    break;
                } else if (bom[0] == (byte) 0x00 && bom[1] == (byte) 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) {
                    encoding = "UTF-32BE";
                    skip = 4;
                    break;
                }
            case 3:
                if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                    encoding = "UTF-8";
                    skip = 3;
                    break;
                }
            case 2:
                if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
                    encoding = "UTF-16BE";
                    skip = 2;
                    break;
                } else if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
                    encoding = "UTF-16LE";
                    skip = 2;
                    break;
                }
            default:
                encoding = System.getProperty("file.encoding");
                skip = 0;
        }

        if (read > 0) {
            inputStream.unread(bom, skip, read - skip);
        }

        return encoding;
    }
}
