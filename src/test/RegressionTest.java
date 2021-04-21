import com.github.sgoeschl.jmeter.report.sla.Main;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class RegressionTest {

    static {
        System.setProperty("user.timezone", "Europe/Vienna");
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");
    }

    private static final String MARKER = "<h2>Report Properties</h2>";

    @Test
    public void testSuccessJtlReport() throws Exception {
        runReportAndCompare("src/test/data/success.jtl", "src/test/data/expected-success-result.html");
    }

    @Test
    public void testErrorJtlReport() throws Exception {
        runReportAndCompare("src/test/data/error.jtl",
                "src/test/data/expected-error-result.html");
    }

    @Test
    public void testFailureJtlReport() throws Exception {
        runReportAndCompare("src/test/data/failure.jtl",
                "src/test/data/expected-failure-result.html");
    }

    @Test
    public void testIncompleteJtlReport() throws Exception {
        runReportAndCompare("src/test/data/incomplete.jtl",
                "src/test/data/expected-incomplete-result.html");
    }

    @Test
    public void testSuccessCsvReport() throws Exception {
        runReportAndCompare("src/test/data/success.csv",
                "src/test/data/expected-success-csv-result.html");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenSourceFileIsNotFound() throws Exception {
        Main.onMain(new String[] { "./target/report.html", "does-not-exist.jtl" });
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailIfNoSourceFilesAreFound() throws Exception {
        Main.onMain(new String[] { "./target/report.html" });
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailForNoJtlXmlFile() throws Exception {
        Main.onMain(new String[] { "./target/empty.html", "pom.xml" });
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForNoEmptyCsvFile() throws Exception {
        Main.onMain(new String[] { "./target/empty.html", "src/test/data/empty.csv" });
    }

    private void runReportAndCompare(String inputFile, String expectedOutputFileName)
            throws Exception {

        final File expectedOutputFile = new File(expectedOutputFileName);
        final File actualReportDirectory = new File("./target/actual");
        final File actualOutputFile = new File(actualReportDirectory, expectedOutputFile.getName());

        Main.onMain(new String[] { actualOutputFile.getAbsolutePath(), inputFile });

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
