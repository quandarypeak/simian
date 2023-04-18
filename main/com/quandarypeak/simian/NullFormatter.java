package com.quandarypeak.simian;

import java.io.File;

final class NullFormatter implements AuditListener {
    public static final NullFormatter INSTANCE = new NullFormatter();

    @Override
    public void startCheck(final Options options) {
    }

    @Override
    public void fileProcessed(final SourceFile sourceFile) {
    }

    @Override
    public void startSet(final int lineCount, final String fingerprint) {
    }

    @Override
    public void block(final Block block) {
    }

    @Override
    public void endSet(final String text) {
    }

    @Override
    public void endCheck(final CheckSummary summary) {
    }

    @Override
    public void error(final File file, final Throwable e) {
    }
}
