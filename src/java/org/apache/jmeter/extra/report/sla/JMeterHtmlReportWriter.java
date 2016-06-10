package org.apache.jmeter.extra.report.sla;

import com.jamonapi.MonKeyImp;
import com.jamonapi.MonitorComposite;
import com.jamonapi.utils.Misc;
import org.apache.commons.lang3.StringEscapeUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Generates a JMeter style HTML report based on the JAMon
 * performance monitor.
 */
public class JMeterHtmlReportWriter {

    // the hardcoded header index
    public static final int DISPLAY_HEADER_LABEL_INDEX = 1;
    public static final int DISPLAY_HEADER_UNITS_INDEX = 2;
    public static final int DISPLAY_HEADER_HITS_INDEX = 3;
    public static final int DISPLAY_HEADER_FIRSTACCESS_INDEX = 13;
    public static final int DISPLAY_HEADER_LASTACCESS_INDEX = 14;

    enum BasicDataColumns {
        LABEL(1, "Label", true),
        UNITS(2, "Units", false),
        HITS(3, "Requests", true),
        AVG(4, "Avg", true),
        TOTAL(5, "Total", true),
        STDEV(6, "StdDev", true),
        MIN_TIME(8, "Min Time", true),
        MAX_TIME(9, "Max Time", true),
        FIRST_ACCESS(13, "First Access", true),
        LAST_ACCESS(14, "Last Acccess", true);

        final int index;
        final String label;
        final boolean isReported;

        BasicDataColumns(int index, String label, boolean isReported) {
            this.index = index;
            this.label = label;
            this.isReported = isReported;
        }

        static List<BasicDataColumns> getReportedColumns() {
            final List<BasicDataColumns> result = new ArrayList<>();
            for (BasicDataColumns value : BasicDataColumns.values()) {
                if (value.isReported) {
                    result.add(value);
                }
            }
            return result;
        }
    }

    /**
     * the JAMON column used for sorting
     */
    private final int sortColumn;

    /**
     * the sort order, e.g. "asc" or "desc"
     */
    private final String sortOrder;

    /**
     * the set of suppressed display headers
     */
    private final Map<String, Double> failureMap;

    /**
     * the date of the first request
     */
    private Date firstAccessDate;

    /**
     * the date of the last request
     */
    private Date lastAccessDate;

    /**
     * the title of the HTML report as HTML fragment, e.g. "Load Test Results"
     */
    private String reportTitle;

    /**
     * the subtitle of the HTML report as HTML fragment, e.g. "Designed for use with JMeter and Ant"
     */
    private String reportSubtitle;

    /**
     * the locale being used to format numbers and dates
     */
    private final Locale locale;

    private final JMeterReportModel model;

    public JMeterHtmlReportWriter(JMeterReportModel model, int sortColumn, String sortOrder, Locale locale) {

        this.model = model;
        this.sortColumn = sortColumn;
        this.sortOrder = sortOrder;
        this.locale = (locale != null ? locale : Locale.getDefault());
        this.firstAccessDate = new Date();
        this.lastAccessDate = new Date(0);
        this.reportTitle = "Load Test Report";
        this.reportSubtitle = "Designed for use with <a href=\"http://jakarta.apache.org/jmeter\">JMeter</a> and <a href=\"http://ant.apache.org\">Ant</a>.";

        this.failureMap = new HashMap<>();
    }

    /**
     * Create a HTML JMeter report.
     *
     * @return the textual performance report
     */
    public String createReport() {

        final MonitorComposite monitor = model.getProvider().getRoot();

        if (!monitor.hasData())
            return "";

        final StringBuffer html = new StringBuffer(100000);// guess on report size
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
        writeErrorMessagesTable(html);
        html.append("<hr size=\"1\">");
        writePropertyTable(html, System.getProperties());
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getReportSubtitle() {
        return reportSubtitle;
    }

    public void setReportSubtitle(String reportSubtitle) {
        this.reportSubtitle = reportSubtitle;
    }

    /**
     * Helper method to correctly format the JAMon report fields.
     *
     * @param data the data field
     * @return the formatted field
     */
    private String format(Object data) {
        if (data instanceof Date) {
            return String.format(locale, "%1$tT", (Date) data);
        } else if (data instanceof Double) {
            return String.format(locale, "%,10.0f", (Double) data);
        } else if (data instanceof Integer) {
            return String.format(locale, "%,d", (Integer) data);
        } else if (data instanceof String) {
            return escapeHtml(data.toString().replace("(0/0/0)", ""));
        } else if (data instanceof Object) {
            return data.toString();
        } else {
            return null;
        }
    }

    private void writeSummaryTable(StringBuffer html, MonitorComposite monitor) {

        final String[] header = monitor.getDisplayHeader();
        final Object[][] data = getDisplayData(monitor, JMeterReportModel.UNIT_MS);
        final Object[][] exceptionData = getDisplayData(monitor, JMeterReportModel.UNIT_EXCEPTION);

        final int rows = data.length;

        // determine the timestamp of the first request
        final int firstAccessIndex = getHeaderIndex(header, "FirstAccess");
        for (int i = 0; i < rows; i++) {
            final Date currFirstAccessDate = (Date) data[i][firstAccessIndex];
            if (this.firstAccessDate.after(currFirstAccessDate)) {
                this.firstAccessDate = currFirstAccessDate;
            }
        }

        // determine the timestamp of the last request
        final int lastAccessIndex = getHeaderIndex(header, "LastAccess");
        for (int i = 0; i < rows; i++) {
            final Date currLastAccessDate = (Date) data[i][lastAccessIndex];
            if (this.lastAccessDate.before(currLastAccessDate)) {
                this.lastAccessDate = currLastAccessDate;
            }
        }

        // determine the "requests"
        Double nrOfRequests = 0.0;
        final int hitsHeaderIndex = getHeaderIndex(header, "Hits");
        for (int i = 0; i < rows; i++) {
            nrOfRequests += (Double) data[i][hitsHeaderIndex];
        }

        // determine the "Failures"
        Double nrOfFailures = 0.0;
        for (Object[] anExceptionData : exceptionData) {
            final String currLLabel = anExceptionData[BasicDataColumns.LABEL.index].toString();
            final Double currHits = (Double) anExceptionData[BasicDataColumns.TOTAL.index];
            this.failureMap.put(currLLabel, currHits);
            nrOfFailures += currHits;
        }

        // determine "Success Rate"
        final double successRate = 100.0 - (nrOfFailures * 100.0 / nrOfRequests);

        // determine "Average Time"
        double overallTime = 0;
        final int totalHeaderIndex = getHeaderIndex(header, "Total");
        for (int i = 0; i < rows; i++) {
            overallTime += (Double) data[i][totalHeaderIndex];
        }
        final double averageTime = overallTime / nrOfRequests;

        // determine "Min Time"
        Double minTime = (double) Long.MAX_VALUE;
        final int minHeaderIndex = getHeaderIndex(header, "Min");
        for (int i = 0; i < rows; i++) {
            minTime = Math.min(minTime, (Double) data[i][minHeaderIndex]);
        }

        // determine "Max Time"
        Double maxTime = (double) Long.MIN_VALUE;
        final int maxHeaderIndex = getHeaderIndex(header, "Max");
        for (int i = 0; i < rows; i++) {
            maxTime = Math.max(maxTime, (Double) data[i][maxHeaderIndex]);
        }

        html.append("<h2>Summary</h2>");
        html.append("<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr valign=\"top\">\n");
        html.append("<th>Requests</th><th>Failures</th><th>Success Rate</th><th>Average Time</th><th>Min Time</th><th>Max Time</th>\n");
        html.append("</tr>\n");
        if (nrOfFailures > 0.0) {
            html.append("<tr valign=\"top\" class=\"Failure\">\n");
        } else {
            html.append("<tr valign=\"top\" class=\"\">\n");
        }
        html.append("<td>").append(format(nrOfRequests.intValue())).append("</td>");
        html.append("<td>").append(format(nrOfFailures.intValue())).append("</td>");
        html.append("<td>").append(String.format(locale, "%3.4f", successRate)).append(" %</td>");
        html.append("<td>").append(format(averageTime)).append(" ms</td>");
        html.append("<td>").append(format(minTime)).append(" ms</td>");
        html.append("<td>").append(format(maxTime)).append(" ms</td>");
        html.append("</tr>");
        html.append("</table>\n");
    }

    private void writePagesOverviewTable(StringBuffer html, MonitorComposite monitor, int sortCol, String sortOrder) {

        final List<BasicDataColumns> reportColumns = BasicDataColumns.getReportedColumns();
        final Object[][] data = Misc.sort(getBasicData(monitor, JMeterReportModel.UNIT_MS), sortCol, sortOrder);
        final int rows = data.length;
        final int cols = reportColumns.size();

        html.append("<h2>Pages Overview (ms)</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        for (BasicDataColumns column : reportColumns) {
            html.append("<th>").append(column.label).append("</th>");
        }
        html.append("<th>").append("Failures").append("</th>");
        html.append("</tr>\n");

        for (int i = 0; i < rows; i++) {
            final String label = data[i][BasicDataColumns.LABEL.index].toString();
            final double nrOfFailures = getNrOfFailures(label);
            final String failureClass = (nrOfFailures > 0.0 ? "Failure" : "");
            html.append("<tr valign=\"top\" class=\"" + failureClass + "\">");
            html.append("<td>").append(label).append("</td>");// first column
            for (int j = 1; j < cols; j++) {
                html.append("<td align='right'>");
                html.append(format(data[i][reportColumns.get(j).index]));
                html.append("</td>");
            }
            html.append("<td align='right'>").append(format(nrOfFailures)).append("</td>");
            html.append("</tr>\n");
        }

        html.append("</table>\n");
    }

    private void writePagesDetailTable(StringBuffer html, MonitorComposite monitor, int sortCol, String sortOrder) {

        final String[] header = {"Label", "Requests", "0-10", "10-20", "20-40", "40-80", "80-160", "160-320", "320-640", "640-1280", "1280-2560", "2560-5120", "5120-10240", "10240-20480", ">20480ms"};
        final int[] headerIndex = {DISPLAY_HEADER_LABEL_INDEX, DISPLAY_HEADER_HITS_INDEX, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};

        final Object[][] rawData = getDisplayData(monitor, JMeterReportModel.UNIT_MS);
        final Object[][] data = Misc.sort(rawData, sortCol, sortOrder);
        final int rows = data.length;
        final int cols = header.length;

        html.append("<h2>Pages Detail Table (ms)</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        for (String headerName : header) {
            html.append("<th>").append(headerName).append("</th>");
        }
        html.append("</tr>");

        for (int i = 0; i < rows; i++) {
            final String label = data[i][DISPLAY_HEADER_LABEL_INDEX].toString();
            final double nrOfFailures = getNrOfFailures(label);
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

    private void writeErrorSummaryTable(StringBuffer html, MonitorComposite monitor, int sortColumn, String sortOrder) {

        final Object[][] rawData = getBasicData(monitor, JMeterReportModel.UNIT_EXCEPTION);

        if (!hasFailures(rawData)) {
            return;
        }

        final Object[][] data = Misc.sort(rawData, sortColumn, sortOrder);
        final int rows = data.length;

        html.append("<h2>Error Summary</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        html.append("<th>").append("Label").append("</th>");
        html.append("<th>").append("Errors").append("</th>");
        html.append("</tr>");

        for (int i = 0; i < rows; i++) {
            final double hits = (Double) data[i][DISPLAY_HEADER_HITS_INDEX];

            if (hits > 0d) {
                html.append("<tr valign=\"top\" class=\"\">");
                html.append("<td>").append(data[i][DISPLAY_HEADER_LABEL_INDEX]).append("</td>");// first column
                html.append("<td align='right'>").append(format(hits)).append("</td>");
                html.append("</tr>\n");
            }
        }

        html.append("</table>\n");
    }

    private void writeErrorDetailTable(StringBuffer html, MonitorComposite monitor, int sortCol, String sortOrder) {

        final Object[][] rawData = getDisplayData(monitor, JMeterReportModel.UNIT_JMETER_ERRORS);

        if (!hasFailures(rawData)) {
            return;
        }

        final Object[][] data = Misc.sort(getDisplayData(monitor, JMeterReportModel.UNIT_JMETER_ERRORS), sortCol, sortOrder);

        html.append("<h2>Error Details</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        html.append("<th>").append("Label").append("</th>");
        html.append("<th>").append("Errors").append("</th>");
        html.append("<th>").append("First").append("</th>");
        html.append("<th>").append("Last").append("</th>");
        html.append("</tr>\n");

        for (int i = 0; i < data.length; i++) {
            final double hits = (Double) data[i][DISPLAY_HEADER_HITS_INDEX];

            if (hits > 0d) {
                final String label = (String) data[i][DISPLAY_HEADER_LABEL_INDEX];
                final Date firstAccess = (Date) data[i][DISPLAY_HEADER_FIRSTACCESS_INDEX];
                final Date lastAccess = (Date) data[i][DISPLAY_HEADER_LASTACCESS_INDEX];
                html.append("<tr valign=\"top\" class=\"\">");
                html.append("<td>").append(format(label)).append("</td>");
                html.append("<td align='right'>").append(format(hits)).append("</td>");
                html.append("<td align='right'>").append(format(firstAccess)).append("</td>");
                html.append("<td align='right'>").append(format(lastAccess)).append("</td>");
                html.append("</tr>\n");
            }
        }

        html.append("</table>\n");
    }

    private void writeErrorMessagesTable(StringBuffer html) {

        final Map<String, List<MonKeyImp>> errorDetailsMap = model.getErrorMessagesMap();

        if (errorDetailsMap.isEmpty()) {
            return;
        }

        html.append("<hr size=\"1\">");

        html.append("<h2>Error Messages</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        html.append("<th>").append("Label").append("</th>");
        html.append("<th>").append("Date").append("</th>");
        html.append("<th>").append("Error Message").append("</th>");
        html.append("</tr>\n");

        for (String key : errorDetailsMap.keySet()) {

            final List<MonKeyImp> monKeyImpList = errorDetailsMap.get(key);

            for (MonKeyImp monKey : monKeyImpList) {

                final Object[] details = (Object[]) monKey.getDetails();
                final String label = (String) details[0];
                final String errorLabel = (String) details[1];
                final String errorCode = (String) details[2];
                final String errorMessage = (String) details[3];
                final Date timeStamp = (Date) details[4];

                html.append("<tr valign=\"top\" class=\"\">");
                html.append("<td>").append(format(errorLabel)).append("</td>");
                html.append("<td>").append(format(timeStamp)).append("</td>");
                html.append("<td>").append(format(errorMessage)).append("</td>");
                html.append("</tr>\n");
            }
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

        html.append("<h2>Report Properties</h2>");
        html.append("\n<table width=\"95%\" cellspacing=\"2\" cellpadding=\"5\" border=\"0\" class=\"details\">\n");
        html.append("<tr>");
        html.append("<th>").append("Key").append("</th>");
        html.append("<th>").append("Value").append("</th>");
        html.append("</tr>");

        // report first request timestamp
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("First Request").append("</td>");
        html.append("<td>").append(this.firstAccessDate).append("</td>");
        html.append("</tr>\n");

        // report last request timestamp
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("Last Request").append("</td>");
        html.append("<td>").append(this.lastAccessDate).append("</td>");
        html.append("</tr>\n");

        // report duration
        final Double reportDuration = (this.lastAccessDate.getTime() - this.firstAccessDate.getTime()) / 1000.0;
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("Report Duration (sec)").append("</td>");
        html.append("<td>").append(format(reportDuration)).append("</td>");
        html.append("</tr>\n");

        // current date
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("Report Creation Date").append("</td>");
        html.append("<td>").append(new Date()).append("</td>");
        html.append("</tr>\n");

        // host name
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("host.name").append("</td>");
        html.append("<td>").append(hostName).append("</td>");
        html.append("</tr>\n");

        // host address
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("host.address").append("</td>");
        html.append("<td>").append(hostAddress).append("</td>");
        html.append("</tr>\n");

        // user name
        html.append("<tr valign=\"top\" class=\"\">");
        html.append("<td>").append("user.name").append("</td>");
        html.append("<td>").append(properties.getProperty("user.name")).append("</td>");
        html.append("</tr>\n");

        final Enumeration keys = properties.keys();
        while (keys.hasMoreElements()) {
            final String key = (String) keys.nextElement();
            final String value = escapeHtml((String) properties.get(key));
            if (key.contains("jmeter.") || key.contains("report.")) {
                html.append("<tr valign=\"top\" class=\"\">");
                html.append("<td>").append(key).append("</td>");
                html.append("<td>").append(value).append("</td>");
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
                "<title>" + getReportTitle() + "</title>\n" +
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
        return "<h1>" + getReportTitle() + "</h1>\n" +
                "<table width=\"100%\">\n" +
                "<tr>\n" +
                "<td align=\"left\"></td><td align=\"right\">" + getReportSubtitle() + "</td>\n" +
                "</tr>\n" +
                "</table>";
    }

    private Object[][] getDisplayData(MonitorComposite monitor, String unit) {
        final List<Object[]> result = new ArrayList<>();
        for (Object[] currData : monitor.getDisplayData()) {
            if (currData[DISPLAY_HEADER_UNITS_INDEX].toString().equals(unit)) {
                result.add(currData);
            }
        }
        return result.toArray(new Object[result.size()][]);
    }

    private Object[][] getBasicData(MonitorComposite monitor, String unit) {
        final List<Object[]> result = new ArrayList<>();
        for (Object[] currData : monitor.getDisplayData()) {
            if (currData[DISPLAY_HEADER_UNITS_INDEX].toString().equals(unit)) {
                result.add(currData);
            }
        }
        return result.toArray(new Object[result.size()][]);
    }

    private Double getNrOfFailures(String label) {
        final Double result = failureMap.get(label);
        return (result != null ? result : 0.0);
    }

    private boolean hasFailures(Object[][] data) {

        if (data == null || data.length == 0) {
            return false;
        }

        final int rows = data.length;
        final double nrOfFirstError = (Double) (data[0][DISPLAY_HEADER_HITS_INDEX]);

        if (rows == 1 && nrOfFirstError == 0d) {
            return false;
        }

        return true;
    }

    private String escapeHtml(String input) {
        return StringEscapeUtils.escapeHtml4(input);
    }
}
