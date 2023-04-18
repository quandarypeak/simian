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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Simply logs sets of duplicate blocks to an output stream.
 */
final class XmlFormatter extends AbstractFormatter {
    private static final Pattern END_CDATA = Pattern.compile("]]>", Pattern.LITERAL);
    private static final Pattern AMPERSAND = Pattern.compile("&");

    /**
     * Constructor.
     *
     * @param out         The output stream to which messages are logged
     * @param printBanner Should the copyright banner be printed
     */
    XmlFormatter(final OutputStream out, final boolean printBanner) {
        super(out);

        println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        println("<?xml-stylesheet href=\"simian.xsl\" type=\"text/xsl\"?>");

        if (printBanner) {
            println("<!--");
            println(Version.BANNER);
            println("-->");
        }

        println("<simian version=\"" + Version.VERSION + "\">");
    }

    @Override
    public void startCheck(final Options options) {
        final StringBuilder buf = new StringBuilder();

        buf.append("    <check");

        for (final Entry<Option, Object> entry : options.getOptions().entrySet()) {
            buf.append(' ').append(entry.getKey()).append("=\"").append(entry.getValue()).append('"');
        }

        buf.append('>');

        println(buf.toString());
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
        println("        <set lineCount=\"" + lineCount + "\" fingerprint=\"" + fingerprint + "\">");
    }

    @Override
    public void block(final Block block) {
        Objects.requireNonNull(block, "block");
        println("            <block sourceFile=\"" + escape(block.getSourceFile().getFilename()) + "\" startLineNumber=\"" + block.getStartLineNumber() + "\" endLineNumber=\"" + block.getEndLineNumber() + "\"/>");
    }

    @Override
    public void endSet(final String text) {
        if (text != null) {
            println("            <text>");
            print("<![CDATA[");
            print(END_CDATA.matcher(text).replaceAll("]]]]><![CDATA[>"));
            println("]]>");
            println("            </text>");
        }
        println("        </set>");
    }

    @Override
    public void endCheck(final CheckSummary summary) {
        println("        <summary duplicateFileCount=\"" + summary.getDuplicateFileCount() + "\" duplicateLineCount=\"" + summary.getDuplicateLineCount() + "\" duplicateBlockCount=\"" + summary.getDuplicateBlockCount() + "\" totalFileCount=\"" + summary.getTotalFileCount() + "\" totalRawLineCount=\"" + summary.getTotalRawLineCount() + "\" totalSignificantLineCount=\"" + summary.getTotalSignificantLineCount() + "\" processingTime=\"" + summary.getProcessingTime() + "\"/>");
        println("    </check>");
        println("</simian>");

        super.endCheck(summary);
    }

    private static String escape(final String text) {
        return AMPERSAND.matcher(text).replaceAll("&amp;");
    }
}
