import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.apache.jmeter.extra.report.sla.Main;

import com.jamonapi.MonitorFactory;

import junit.framework.Assert;
import junit.framework.TestCase;

public class RegressionTest extends TestCase {
 
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		MonitorFactory.reset();
	}
	
	private static final String MARKER="<h2>Report Properties</h2>";
	
	public void testSuccess() throws Exception {
		runReportAndCompare("src/test/data/success.jtl", "src/test/data/expected-success-result.html");
	}

	public void testError() throws Exception {
		runReportAndCompare("src/test/data/error.jtl", "src/test/data/expected-error-result.html");
	}

	public void testIncomplete() throws Exception {
		runReportAndCompare("src/test/data/incomplete.jtl", "src/test/data/expected-incomplete-result.html");
	}
	
	public void testSuccessCsv() throws Exception {
		runReportAndCompare("src/test/data/success.csv", "src/test/data/expected-success-csv-result.html");
	}
	
	private void runReportAndCompare(String inputFile, String expectedOutputFile) throws IOException, Exception {
		File tempFile = File.createTempFile("junit", ".html");
		tempFile.deleteOnExit();

		Main.main(new String[] { tempFile.getAbsolutePath(),
				inputFile });
		
		
		Assert.assertEquals(removeRunDependentParts(readAsString(new File(expectedOutputFile))),
				removeRunDependentParts(readAsString(tempFile)));
	}
	private String removeRunDependentParts(String input) {
		int indexOf = input.indexOf(MARKER);
		if(indexOf== -1){
			return input;
		}
		return input.substring(0, indexOf);
	}
	private String readAsString(File file) throws IOException {
		FileInputStream inputStream = new FileInputStream(file);
		try {
			FileChannel channel = inputStream.getChannel();

			MappedByteBuffer buffer = channel.map(
					FileChannel.MapMode.READ_ONLY, 0, channel.size());
			return Charset.defaultCharset().decode(buffer).toString();
		} finally {
			inputStream.close();
		}

	}

}
