package org.apache.jmeter.extra.report.sla;

import com.jamonapi.MonitorComposite;
import com.jamonapi.MonitorFactory;
import com.jamonapi.utils.Misc;

import java.net.InetAddress;
import java.util.*;

/**
 * Generates a JMeter style HTML report based on the JAMON
 * performance monitor.
 */
public class JMeterHtmlReportWriter {

    /**
     * the JAMON column used for sorting
     */
    private int sortColumn;

    /**
     * the sort order, e.g. "asc" or "desc"
     */
    private String sortOrder;

    /**
     * the set of suppressed display headers
     */
    private Map<String, Double> failureMap;

    /**
     * the date of the first test invocation
     */
    private Date firstAccessDate;

    /**
     * the date of the last test invocation
     */
    private Date lastAccessDate;

    public JMeterHtmlReportWriter(int sortColumn, String sortOrder) {

        this.sortColumn = sortColumn;
        this.sortOrder = sortOrder;
        this.firstAccessDate = new Date();
        this.lastAccessDate = new Date(0);

        this.failureMap = new HashMap<String, Double>();
    }

    /**
     * Create a HTML JMeter report.
     *
     * @return the textual performance report
     */
    public String createReport() {

        MonitorComposite monitor = MonitorFactory.getRootMonitor();

        if (!monitor.hasData())
            return "";

        StringBuffer html = new StringBuffer(100000);// guess on report size
        html.append("<html>\n");
        html.append(getHtmlHeadFragment());
        html.append("<body>\n");
        html.append(getHeaderFragment());
        html.append("<hr size=\"1\">");
        writeSummaryTable(html, monitor);
        html.append("<hr size=\"1\">");
        writePagesOverviewTable(html, monitor, this.sortColumn, this.sortOrder);
        html.append("<hr size=\"1\">");
        writePagesDetailTable(html, monitor, this.sortColumn, this.sortOrder);
        html.append("<hr size=\"1\">");
        writeErrorSummaryTable(html, monitor, 0, "asc");
        html.append("<hr size=\"1\">");
        writeErrorDetailTable(html, monitor, 0, "asc");
        html.append("<hr size=\"1\">");
        writePropertyTable(html, System.getProperties());
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Helper method to correctly format the JAMON report fields.
     *
     * @param data the data field
     * @return the formatted field
     */
    private static String format(Object data) {
        if (data instanceof Date) {
            return String.format("%1$tT", (Date) data);
        } else if (data instanceof Double) {
            return String.format("%,10.0f", (Double) data);
        } else if (data instanceof Integer) {
            return String.format("%,d", (Integer) data);
        } else if (data instanceof String) {
            return data.toString().replace("(0/0/0)", "");
        } else {
            return data.toString();
        }

    }

    private void writeSummaryTable(StringBuffer html, MonitorComposite monitor) {

        String[] header = monitor.getDisplayHeader();
        Object[][] data = getDisplayData(monitor, JMeterReportModel.UNIT_MS);
        Object[][] exceptionData = getDisplayData(monitor, JMeterReportModel.UNIT_EXCEPTION);

        int rows = data.length;

        // determine the first access date of the test
        int firstAccessIndex = getHeaderIndex(header, "FirstAccess");
        for (int i = 0; i < rows; i++) {
            Date currFirstAccessDate = (Date) data[i][firstAccessIndex];
            if(this.firstAccessDate.after(currFirstAccessDate)) {
                this.firstAccessDate = currFirstAccessDate;
            }
        }

        // determine the last access date of the test
        int lastAccessIndex = getHeaderIndex(header, "LastAccess");
        for (int i = 0; i < rows; i++) {
            Date currLastAccessDate = (Date) data[i][lastAccessIndex];
            if(this.lastAccessDate.before(currLastAccessDate)) {
                this.lastAccessDate = currLastAccessDate;
            }
        }

        // determine the "Tests"
        Double nrOfTests = 0.0;
        int hitsHeaderIndex = getHeaderIndex(header, "Hits");
        for (int i = 0; i < rows; i++) {
            nrOfTests += (Double) data[i][hitsHeaderIndex];
        }

        // determine the "Failures"
        Double nrOfFailures = 0.0;
        for (Object[] anExceptionData : exceptionData) {
            String currLLabel = anExceptionData[0].toString();
            Double currHits = (Double) anExceptionData[2];
            this.failureMap.put(currLLabel, currHits);
            nrOfFailures += currHits;
        }

        // determine "Success Rate"
        double successRate = 100.0 - (nrOfFailures * 100.0 / nrOfTests);

        // determine "Average Time"
        double overallTime = 0;
        int totalHeaderIndex = getHeaderIndex(header, "Total");
        for (int i = 0; i < rows; i++) {
            overallTime += (Double) data[i][totalHeaderIndex];
        }
        double averageTime = overallTime / nrOfTests;

        // determine "Min Time"
        Double minTime = (double) Long.MAX_VALUE;
        int minHeaderIndex = getHeaderIndex(header, "Min");
        for (int i = 0; i < rows; i++) {
            minTime = Math.min(minTime, (Double) data[i][minHeaderIndex]);
        }

        // determine "Max Time"
        Double maxTime = (double) Long.MIN_VALUE;
        int maxHeaderIndex = getHeaderIndex(header, "Max");
        for (int i = 0; i < rows; i++) {
            maxTime = Math.max(maxTime, (Double) data[i][maxHeaderIndex]);
        }

        html.append("<h2>Summary</h2>");
        html.append("<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr valign=\"top\">\n");
        html.append("<th>Tests</th><th>Failures</th><th>Success Rate</th><th>Average Time</th><th>Min Time</th><th>Max Time</th>\n");
        html.append("</tr>\n");
        if (nrOfFailures > 0.0) {
            html.append("<tr valign=\"top\" class=\"Failure\">\n");
        } else {
            html.append("<tr valign=\"top\" class=\"\">\n");
        }
        html.append("<td>").append(format(nrOfTests.intValue())).append("</td>");
        html.append("<td>").append(format(nrOfFailures.intValue())).append("</td>");
        html.append("<td>").append(String.format("%3.4f", successRate)).append(" %</td>");
        html.append("<td>").append(format(averageTime)).append(" ms</td>");
        html.append("<td>").append(format(minTime)).append(" ms</td>");
        html.append("<td>").append(format(maxTime)).append(" ms</td>");
        html.append("</tr>");
        html.append("</table>\n");
    }

    private void writePagesOverviewTable(StringBuffer html, MonitorComposite monitor, int sortCol, String sortOrder) {

        String[] header = {"Label", "Tests", "Avg", "Total", "StdDev", "Min Time", "Max Time", "First Access", "Last Acccess"};
        int[] headerIndex = {0, 2, 3, 4, 5, 7, 8, 12, 13};

        Object[][] data = Misc.sort(getBasicData(monitor, JMeterReportModel.UNIT_MS), sortCol, sortOrder);
        int rows = data.length;
        int cols = header.length;

        html.append("<h2>Pages Overview (ms)</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        for (String headerName : header) {
            html.append("<th>").append(headerName).append("</th>");
        }
        html.append("<th>").append("Failures").append("</th>");
        html.append("</tr>\n");

        for (int i = 0; i < rows; i++) {
            String label = data[i][0].toString();
            double nrOfFailures = getNrOfFailures(label);
            String failureClass = (nrOfFailures > 0.0 ? "Failure" : "");
            html.append("<tr valign=\"top\" class=\"" + failureClass + "\">");
            html.append("<td>").append(label).append("</td>");// first column
            for (int j = 1; j < cols; j++) {
                html.append("<td align='right'>").append(format(data[i][headerIndex[j]])).append("</td>");
            }
            html.append("<td align='right'>").append(format(nrOfFailures)).append("</td>");
            html.append("</tr>\n");
        }

        html.append("</table>\n");
    }

    private void writePagesDetailTable(StringBuffer html, MonitorComposite monitor, int sortCol, String sortOrder) {

        String foo[] = monitor.getDisplayHeader();
        String[] header = {"Label", "Tests", "0-10", "10-20", "20-40", "40-80", "80-160", "160-320", "320-640", "640-1280", "1280-2560", "2560-5120", "5120-10240", "10240-20480", ">20480ms"};
        int[] headerIndex = {0, 2, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29};

        Object[][] rawData = getDisplayData(monitor, JMeterReportModel.UNIT_MS);
        Object[][] data = Misc.sort(rawData, sortCol, sortOrder);
        int rows = data.length;
        int cols = header.length;

        html.append("<h2>Pages Detail Table (ms)</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        for (String headerName : header) {
            html.append("<th>").append(headerName).append("</th>");
        }
        html.append("</tr>");

        for (int i = 0; i < rows; i++) {
            String label = data[i][0].toString();
            double nrOfFailures = getNrOfFailures(label);
            if (nrOfFailures > 0.0) {
                html.append("<tr valign=\"top\" class=\"Failure\">");
            } else {
                html.append("<tr valign=\"top\">");
            }
            html.append("<td>").append(label).append("</td>");// first column
            for (int j = 1; j < cols; j++) {
                html.append("<td align='right'>").append(format(data[i][headerIndex[j]])).append("</td>");
            }
            html.append("</tr>\n");
        }

        html.append("</table>\n");
    }

    private void writeErrorDetailTable(StringBuffer html, MonitorComposite monitor, int sortCol, String sortOrder) {

        Object[][] rawData = getDisplayData(monitor, JMeterReportModel.UNIT_JMETER_ERRORS);
        int rows = rawData.length;
        if (rows == 0) {
            return;
        }

        Object[][] data = Misc.sort(getDisplayData(monitor, JMeterReportModel.UNIT_JMETER_ERRORS), sortCol, sortOrder);

        html.append("<h2>Error Details</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        html.append("<th>").append("Label").append("</th>");
        html.append("<th>").append("Errors").append("</th>");
        html.append("<th>").append("First").append("</th>");
        html.append("<th>").append("Last").append("</th>");
        html.append("</tr>\n");

        for (int i = 0; i < rows; i++) {
            String label = (String) data[i][0];
            Double hits = (Double) data[i][2];
            Date firstAccess = (Date) data[i][12];
            Date lastAccess = (Date) data[i][13];
            html.append("<tr valign=\"top\" class=\"\">");
            html.append("<td>").append(format(label)).append("</td>");
            html.append("<td align='right'>").append(format(hits)).append("</td>");
            html.append("<td align='right'>").append(format(firstAccess)).append("</td>");
            html.append("<td align='right'>").append(format(lastAccess)).append("</td>");
            html.append("</tr>\n");
        }

        html.append("</table>\n");
    }

    private void writeErrorSummaryTable(StringBuffer html, MonitorComposite monitor, int sortColumn, String sortOrder) {

        Object[][] rawData = getBasicData(monitor, JMeterReportModel.UNIT_EXCEPTION);

        int rows = rawData.length;
        if (rows == 0) {
            return;
        }

        Object[][] data = Misc.sort(rawData, sortColumn, sortOrder);

        if (rows == 0) {
            return;
        }

        html.append("<h2>Error Summary</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        html.append("<th>").append("Label").append("</th>");
        html.append("<th>").append("Errors").append("</th>");
        html.append("</tr>");

        for (int i = 0; i < rows; i++) {
            html.append("<tr valign=\"top\" class=\"\">");
            html.append("<td>").append(data[i][0]).append("</td>");// first column
            html.append("<td align='right'>").append(format(data[i][2])).append("</td>");
            html.append("</tr>\n");
        }

        html.append("</table>\n");
    }

    private void writePropertyTable(StringBuffer html, Properties properties) {

        String hostName = "localhost";
        String hostAddress = "127.0.0.1";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            // nothing to do
        }


        html.append("<h2>JMeter Report Properties</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        html.append("<th>").append("Key").append("</th>");
        html.append("<th>").append("Value").append("</th>");
        html.append("</tr>");

        // report first access date
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("Test Start Date").append("</td>");
        html.append("<td>").append(this.firstAccessDate).append("</td>");// first column
        html.append("</tr>\n");

        // report last access date
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("Test End Date").append("</td>");
        html.append("<td>").append(this.lastAccessDate).append("</td>");// first column
        html.append("</tr>\n");

        // test duration
        Double testDuration = (this.lastAccessDate.getTime() - this.firstAccessDate.getTime()) / 1000.0;
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("Test Duration (sec)").append("</td>");
        html.append("<td>").append(format(testDuration)).append("</td>");// first column
        html.append("</tr>\n");

        // current date
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("Report Creation Date").append("</td>");
        html.append("<td>").append(new Date()).append("</td>");// first column
        html.append("</tr>\n");

        // host name
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("host.name").append("</td>");
        html.append("<td>").append(hostName).append("</td>");// first column
        html.append("</tr>\n");

        // host address
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("host.address").append("</td>");
        html.append("<td>").append(hostAddress).append("</td>");// first column
        html.append("</tr>\n");

        // user name
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("user.name").append("</td>");
        html.append("<td>").append(properties.getProperty("user.name")).append("</td>");// first column
        html.append("</tr>\n");

        Enumeration keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) properties.get(key);
            if (key.contains("jmeter")) {
                html.append("<tr valign=\"top\" class=\"\">");
                html.append("<td>").append(key).append("</td>");// first column
                html.append("<td>").append(value).append("</td>");// first column
                html.append("</tr>\n");
            }
        }

        html.append("</table>\n");
    }

    private static int getHeaderIndex(String[] headers, String name) {

        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unable to find the following header" + name);
    }

    private String getHtmlHeadFragment() {

        return "<head>\n" +
                "<META http-equiv=\"Content-Type\" content=\"text/html; charset=US-ASCII\">\n" +
                "<title>JMeter Load Test Results</title>\n" +
                "<style type=\"text/css\">\n" +
                "\t\t\t\tbody {\n" +
                "\t\t\t\t\tfont:normal 68% verdana,arial,helvetica;\n" +
                "\t\t\t\t\tcolor:#000000;\n" +
                "\t\t\t\t}\n" +
                "\t\t\t\ttable tr td, table tr th {\n" +
                "\t\t\t\t\tfont-size: 68%;\n" +
                "\t\t\t\t}\n" +
                "\t\t\t\ttable.details tr th{\n" +
                "\t\t\t\t\tfont-weight: bold;\n" +
                "\t\t\t\t\ttext-align:left;\n" +
                "\t\t\t\t\tbackground:#a6caf0;\n" +
                "\t\t\t\t}\n" +
                "\t\t\t\ttable.details tr td{\n" +
                "\t\t\t\t\tbackground:#eeeee0;\n" +
                "\t\t\t\t}\n" +
                "\t\t\t\th1 {\n" +
                "\t\t\t\t\tmargin: 0px 0px 5px; font: 165% verdana,arial,helvetica\n" +
                "\t\t\t\t}\n" +
                "\t\t\t\th2 {\n" +
                "\t\t\t\t\tmargin-top: 1em; margin-bottom: 0.5em; font: bold 125% verdana,arial,helvetica\n" +
                "\t\t\t\t}\n" +
                "\t\t\t\th3 {\n" +
                "\t\t\t\t\tmargin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica\n" +
                "\t\t\t\t}\n" +
                "\t\t\t\t.Failure {\n" +
                "\t\t\t\t\tfont-weight:bold; color:red;\n" +
                "\t\t\t\t}\n" +
                "\t\t\t</style>\n" +
                "</head>\n";
    }

    private String getHeaderFragment() {
        return "<h1>Load Test Results</h1>\n" +
                "<table width=\"100%\">\n" +
                "<tr>\n" +
                "<td align=\"left\"></td><td align=\"right\">Designed for use with <a href=\"http://jakarta.apache.org/jmeter\">JMeter</a> and <a href=\"http://ant.apache.org\">Ant</a>.</td>\n" +
                "</tr>\n" +
                "</table>";
    }

    private Object[][] getDisplayData(MonitorComposite monitor, String unit) {
        List<Object[]> result = new ArrayList<Object[]>();
        for (Object[] currData : monitor.getDisplayData()) {
            if (currData[1].toString().equals(unit)) {
                result.add(currData);
            }
        }
        return result.toArray(new Object[result.size()][]);
    }

    private Object[][] getBasicData(MonitorComposite monitor, String unit) {
        List<Object[]> result = new ArrayList<Object[]>();
        for (Object[] currData : monitor.getDisplayData()) {
            if (currData[1].toString().equals(unit)) {
                result.add(currData);
            }
        }
        return result.toArray(new Object[result.size()][]);
    }

    private Double getNrOfFailures(String label) {
        Double result = failureMap.get(label);
        return (result != null ? result : 0.0);
    }
}
