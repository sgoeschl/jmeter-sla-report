import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.jmeter.extra.report.sla.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class RegressionTest extends TestCase {

    static {
        System.setProperty("user.timezone", "Europe/Vienna");
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");
    }

    private static final String MARKER = "<h2>Report Properties</h2>";

    public void testSuccess() throws Exception {
        runReportAndCompare("src/test/data/success.jtl", "src/test/data/expected-success-result.html");
    }

    public void testError() throws Exception {
        runReportAndCompare("src/test/data/error.jtl",
                "src/test/data/expected-error-result.html");
    }

    public void testFailure() throws Exception {
        runReportAndCompare("src/test/data/failure.jtl",
                "src/test/data/expected-failure-result.html");
    }

    public void testIncomplete() throws Exception {
        runReportAndCompare("src/test/data/incomplete.jtl",
                "src/test/data/expected-incomplete-result.html");
    }

    public void testSuccessCsv() throws Exception {
        runReportAndCompare("src/test/data/success.csv",
                "src/test/data/expected-success-csv-result.html");
    }

    private void runReportAndCompare(String inputFile, String expectedOutputFileName)
            throws Exception {

        final File expectedOutputFile = new File(expectedOutputFileName);
        final File actualReportDirectory = new File("./target/actual");
        final File actualOutputFile = new File(actualReportDirectory, expectedOutputFile.getName());

        Main.main(new String[] {actualOutputFile.getAbsolutePath(), inputFile});

        final String expectedReportContent = removeRunDependentParts(readAsString(expectedOutputFile));
        final String actualReportContent = removeRunDependentParts(readAsString(actualOutputFile));

        Assert.assertEquals(expectedReportContent, actualReportContent);
    }

    private String removeRunDependentParts(String input) {
        final int indexOf = input.indexOf(MARKER);
        if (indexOf == -1) {
            return input;
        }
        return input.substring(0, indexOf);
    }

    private String readAsString(File file) throws IOException {
        final FileInputStream inputStream = new FileInputStream(file);
        try {
            final FileChannel channel = inputStream.getChannel();

            final MappedByteBuffer buffer = channel.map(
                    FileChannel.MapMode.READ_ONLY, 0, channel.size());
            return Charset.defaultCharset().decode(buffer).toString();
        } finally {
            inputStream.close();
        }

    }

}
