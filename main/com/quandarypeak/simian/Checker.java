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

/**
 * Performs the duplicate checking.
 */
public final class Checker {
    private final Datastore datastore;
    private final AuditListener auditListener;
    private final SummarisingAuditListener summarisingAuditListener;
    private final Options options;

    /**
     * Constructor.
     *
     * @param auditListener The event auditListener to notify
     * @param options       The options controlling behaviour of the checker.
     */
    public Checker(final AuditListener auditListener, final Options options) {
        AuditListener l = auditListener;
        if (options.hasOption(Option.REPORT_DUPLICATE_TEXT)) {
            l = new BlockTextRetrievingAuditListener(l);
        }

        summarisingAuditListener = new SummarisingAuditListener(l);
        l = summarisingAuditListener;

        if (options.hasOption(Option.IGNORE_OVERLAPPING_BLOCKS)) {
            l = new IgnoreOverlappingBlocksAuditListener(l);
        }

        this.datastore = new Datastore(options.getThreshold());
        this.auditListener = l;
        this.options = options;

        this.auditListener.startCheck(this.options);
    }

    /**
     * Obtains the options controlling behaviour of the checker.
     *
     * @return The options controlling behaviour of the checker.
     */
    public Options getOptions() {
        return options;
    }

    /**
     * Obtains the event auditListener to notify.
     *
     * @return The event auditListener to notify.
     */
    public AuditListener getAuditListener() {
        return auditListener;
    }

    /**
     * Performs the checking once all source files have been loaded.
     *
     * @return <code>true</code> on success; otherwise <code>false</code>.
     */
    public boolean check() {
        datastore.check(auditListener);
        auditListener.endCheck(null);   // TODO: Hack due to the crappy way summarising was done

        if (options.hasOption(Option.FAIL_ON_DUPLICATION)) {
            final Object failOnDuplication = options.getOption(Option.FAIL_ON_DUPLICATION);

            if (failOnDuplication instanceof Integer) {
                return summarisingAuditListener.getDuplicateLinePercentage() < (Integer) failOnDuplication;
            } else if (Boolean.TRUE.equals(failOnDuplication)) {
                return summarisingAuditListener.getDuplicateLineCount() == 0;
            }
        }

        return true;
    }

    /**
     * Gets a source file given a filename.
     */
    SourceFile getSourceFile(final String filename) {
        return datastore.getSourceFile(filename);
    }

    /**
     * Adds a a block for checking.
     */
    Block addBlock(final SourceFile sourceFile, final int start, final int end, final long blockHash, final Block previous) {
        return datastore.addBlock(sourceFile, start, end, blockHash, previous);
    }
}
