package gov.nih.nci.hpc.cli.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MultipartUtil {
	private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;
    private final int maxBufferSize = 4096;
    private long contentLength = 0;
    private URL url;

    private List<FormField> fields;
    private List<FilePart> files;

    private class FormField {
        public String name;
        public String value;

        public FormField(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private class FilePart {
        public String fieldName;
        public File uploadFile;

        public FilePart(String fieldName, File uploadFile) {
            this.fieldName = fieldName;
            this.uploadFile = uploadFile;
        }
    }

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param charset
     * @throws IOException
     */
    public MultipartUtil(String requestURL, String charset)
            throws IOException {
        this.charset = charset;

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";
        url = new URL(requestURL);
        fields = new ArrayList<>();
        files = new ArrayList<>();
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value)
            throws UnsupportedEncodingException {
        String fieldContent = "--" + boundary + LINE_FEED;
        fieldContent += "Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED;
        fieldContent += "Content-Type: text/plain; charset=" + charset + LINE_FEED;
        fieldContent += LINE_FEED;
        fieldContent += value + LINE_FEED;
        contentLength += fieldContent.getBytes(charset).length;
        fields.add(new FormField(name, value));
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();

        String fieldContent = "--" + boundary + LINE_FEED;
        fieldContent += "Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + fileName + "\"" + LINE_FEED;
//        fieldContent += "Content-Type: "
//                + URLConnection.guessContentTypeFromName(fileName) + LINE_FEED;
        fieldContent += LINE_FEED;
        // file content would go here
        fieldContent += LINE_FEED;
        contentLength += fieldContent.getBytes(charset).length;
        contentLength += uploadFile.length();
        files.add(new FilePart(fieldName, uploadFile));
    }

    /**
     * Adds a header field to the request.
     *
     * @param name  - name of the header field
     * @param value - value of the header field
     */
    //public void addHeaderField(String name, String value) {
    //    writer.append(name + ": " + value).append(LINE_FEED);
    //    writer.flush();
    //}

    /**
     * Completes the request and receives response from the server.
     *
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();
        String content = "--" + boundary + "--" + LINE_FEED;
        contentLength += content.getBytes(charset).length;

        if (!openConnection()) {
            return response;
        }

        writeContent();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }
        return response;
    }

      private boolean openConnection()
            throws IOException {
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true);    // indicates POST method
        httpConn.setDoInput(true);
        //httpConn.setRequestProperty("Accept-Encoding", "identity");
        httpConn.setFixedLengthStreamingMode(contentLength);
        httpConn.setRequestProperty("Connection", "Keep-Alive");
        httpConn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
        outputStream = new BufferedOutputStream(httpConn.getOutputStream());
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
        return true;
    }

    private void writeContent()
            throws IOException {

        for (FormField field : fields) {
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + field.name + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + charset).append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(field.value).append(LINE_FEED);
            writer.flush();
        }

        for (FilePart filePart : files) {
            String fileName = filePart.uploadFile.getName();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + filePart.fieldName
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(filePart.uploadFile);
            int bufferSize = Math.min(inputStream.available(), maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer, 0, bufferSize)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();
            writer.append(LINE_FEED);
            writer.flush();
        }

        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();
    }
}
