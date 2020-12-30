package gov.nih.nci.hpc.web.util;


import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ExcelExportProc {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportProc.class);

    private List<String> headers;
    private List<List<String>> data;
    private String template;
    private String reportName;
    private String fileName;
    private String mimeType;
    private String extension;
    private String fieldSeparator = "\t";

    public static final String XLSX_MIMETYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String XLS_MIMETYPE = "application/vnd.ms-excel";

    public void doExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Workbook wb = new HSSFWorkbook();
        Sheet s = wb.createSheet();
        Row r = null;
        String filename = this.fileName
            + MessageFormat.format("{0, date, MM_dd_yyyy}", new Date()).trim() + this.extension;

        String characterEncoding = response.getCharacterEncoding();
        if (characterEncoding != null) {
            mimeType += "; charset=" + characterEncoding;
        }
        response.setContentType(mimeType);
        response.addHeader("content-disposition", "inline; filename=" + filename);
        int rownum = 0;
        r = s.createRow(rownum++); // header row
        int cellnum = 0;
        for(String h: headers) {
            r.createCell(cellnum++).setCellValue(StringUtils.defaultString(h));
        }

        for(List<String> row: data) {
            cellnum = 0;
            r = s.createRow(rownum++);
            for(String cell: row) {
                r.createCell(cellnum++).setCellValue(cell);
            }
        }

        wb.write(response.getOutputStream());
    }

    /**
     * Do export.
     *
     * @param request  the request
     * @param response the response
     * @throws IOException the io exception
     */
    public void doCSVExport(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String filename = this.fileName
            + MessageFormat.format("{0, date, MM_dd_yyyy}", new Date()).trim() + this.extension;

        String characterEncoding = response.getCharacterEncoding();
        if (characterEncoding != null) {
            mimeType += "; charset=" + characterEncoding;
        }
        response.setContentType(mimeType);
        response.addHeader("content-disposition", "inline; filename=" + filename);

        Writer w = response.getWriter();

        StringBuffer sb = new StringBuffer();

        try {
            // write headers
            if (headers != null && !headers.isEmpty()) {
                Iterator<String> it = headers.iterator();
                while (it.hasNext()) {
                    sb.append(escapeCell(it.next()));
                    if (it.hasNext()) {
                        sb.append(fieldSeparator);
                    }
                }
                sb.append("\n");
            }
            write(w, sb.toString());

            // write rows data
            Iterator<List<String>> itRows = data.iterator();
            while (itRows.hasNext()) {
                Iterator<String> itCells = itRows.next().iterator();
                sb = new StringBuffer();
                while (itCells.hasNext()) {
                    sb.append(escapeCell(itCells.next()));
                    if (itCells.hasNext()) {
                        sb.append(fieldSeparator);
                    }
                }
                if (itRows.hasNext()) {
                    sb.append("\n");
                }
                write(w, sb.toString());
            }

        } finally {
            w.flush();
            w.close();
        }
    }

    /**
     * Escape cell string.
     *
     * @param value the value
     * @return the string
     */
    protected String escapeCell(Object value) {
        if (value != null) {
            return "\"" + StringUtils.replace(StringUtils.trim(value.toString()), "\"", "\"\"") + "\"";
        }

        return null;
    }

    public String getExtension() {
        return extension;
    }

    public String getFieldSeparator() {
        return fieldSeparator;
    }

    /**
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets report name.
     *
     * @return the report name
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * Gets template.
     *
     * @return the template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Sets data.
     *
     * @param data the data
     */
    public void setData(List<List<String>> data) {
        this.data = data;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setFieldSeparator(String fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

    /**
     * @param fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Sets headers.
     *
     * @param headers the headers
     */
    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Sets report name.
     *
     * @param reportName the report name
     */
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    /**
     * Sets template.
     *
     * @param template the template
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    private void write(Writer out, String string) throws IOException {
        if (string != null) {
            out.write(string);
        }
    }
}