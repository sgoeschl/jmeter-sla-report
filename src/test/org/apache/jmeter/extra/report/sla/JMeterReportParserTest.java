package org.apache.jmeter.extra.report.sla;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class JMeterReportParserTest extends TestCase {

	public void testCanFilterFilesInSourceDirectory() {
		final JMeterReportParser parser = new JMeterReportParser(null);
		parser.setSourceFiles(Arrays.asList(new File("src/test/data")));
		final List<File> sourceFiles = parser.getSourceFiles();
		assertEquals(5, sourceFiles.size());
	}
	
}
