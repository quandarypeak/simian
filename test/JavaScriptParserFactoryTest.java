package test;

import org.junit.Test;
import org.junit.Assert;

import com.quandarypeak.simian.JavaScriptParserFactory;
import com.quandarypeak.simian.LineListener;
import com.quandarypeak.simian.LineBuffer;
import com.quandarypeak.simian.Options;
import com.quandarypeak.simian.Parser;

import java.io.FileReader;
import java.io.File;

public class JavaScriptParserFactoryTest {
    @Test
    public void testParse() throws Exception {
        LineListener lineListener = new TestLineListener();
        Parser parser = new JavaScriptParserFactory().createParser(lineListener, new Options());
        int ret = parser.parse(new FileReader(new File("test/mockups/test.js")));
        
        Assert.assertTrue("Asserting the parsing return value: " + ret, ret == 47);        
    }
}
