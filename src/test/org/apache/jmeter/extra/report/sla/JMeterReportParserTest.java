package org.apache.jmeter.extra.report.sla;

import junit.framework.TestCase;

import java.io.File;
import java.util.List;

import static java.util.Collections.singletonList;

public class JMeterReportParserTest extends TestCase {

    public void testCanFilterFilesInSourceDirectory() {
        final JMeterReportParser parser = new JMeterReportParser(null);
        parser.setSourceFiles(singletonList(new File("src/test/data")));
        final List<File> sourceFiles = parser.getSourceFiles();
        assertEquals(6, sourceFiles.size());
    }

}
