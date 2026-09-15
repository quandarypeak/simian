package com.quandarypeak.simian;

import org.junit.Test;
import org.junit.Assert;

import java.io.FileReader;
import java.io.File;

public class JavaScriptParserTest {
    @Test
    public void testParse() throws Exception {
        LineListener lineListener = new TestLineListener();
        Parser parser = new JavaScriptParserFactory().createParser(lineListener, new Options());
        int ret = parser.parse(new FileReader(new File("test/data/javascript/test.js")));

        Assert.assertTrue("Asserting the parsing return value: " + ret, ret == 47);
    }
}
