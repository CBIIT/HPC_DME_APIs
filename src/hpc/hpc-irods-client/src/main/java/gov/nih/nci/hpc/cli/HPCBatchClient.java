package gov.nih.nci.hpc.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.util.HpcConfigProperties;

@Component
public abstract class HPCBatchClient {
	@Autowired
	private HpcConfigProperties configProperties;
	String hpcServerURL;
	String hpcCertPath;
	String hpcCertPassword;
	String hpcDataService;
	String hpcCollectionService;
	String logDir;
	FileWriter fileLogWriter = null;
	FileWriter fileRecordWriter = null;
	CSVPrinter csvFilePrinter = null;
	String logFile = null;
	String logRecordsFile = null;
	boolean headerAdded = false;

	public HPCBatchClient() {

	}

	protected void preprocess()
	{
		hpcServerURL = configProperties.getProperty("hpc.server.url");
		hpcDataService = configProperties.getProperty("hpc.dataobject.service");
		hpcCertPath = configProperties.getProperty("hpc.ssl.keystore.path");
		hpcCertPassword = configProperties.getProperty("hpc.ssl.keystore.password");
		logDir = configProperties.getProperty("hpc.error-log.dir");
		hpcCollectionService = configProperties.getProperty("hpc.collection.service");
		logFile = logDir + File.separator + "errorLog" + new SimpleDateFormat("yyyyMMddhhmm'.csv'").format(new Date());
		logRecordsFile = logDir + File.separator + "errorRecords"
				+ new SimpleDateFormat("yyyyMMddhhmm'.csv'").format(new Date());
		File file1 = new File(logFile);
		File file2 = new File(logRecordsFile);
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
		try {
			if (!file1.exists()) {
				file1.createNewFile();
			}
			fileLogWriter = new FileWriter(file1, true);

			if (!file2.exists()) {
				file2.createNewFile();
			}
			fileRecordWriter = new FileWriter(file2, true);
			csvFilePrinter = new CSVPrinter(fileRecordWriter, csvFileFormat);
		} catch (IOException e) {
			System.out.println("Failed to initialize Batch process: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	public String process(String fileName) {
		try {
			jline.console.ConsoleReader reader = new jline.console.ConsoleReader();
			reader.setExpandEvents(false);
			System.out.println("Enter NCI Login UserId:");
			String userId = reader.readLine();

			System.out.println("Enter NCI Login password:");
			String password = reader.readLine(new Character('*'));
			System.out.println("Initiating batch process as NCI Login UserId:" + userId);
			preprocess();
			boolean success = processFile(fileName, userId, password);
			if (success)
				return "Batch process Successful";
			else
				return "Batch process is not Successful. Please error log for the records not processed.";
		} catch (IOException e) {
			e.printStackTrace();
			return "Failed to run batch registration";
		} finally {
			postProcess();
		}
	}

	protected abstract boolean processFile(String fileName, String userId, String password);

	protected void postProcess() {
		try {
			if (fileLogWriter != null) {
				fileLogWriter.flush();
				fileLogWriter.close();
			}

			if (fileRecordWriter != null) {
				fileRecordWriter.flush();
				fileRecordWriter.close();
			}

			if (csvFilePrinter != null)
				csvFilePrinter.close();

		} catch (IOException e) {
			System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
			e.printStackTrace();
		}

	}

	protected void addErrorToLog(String error, int recordLineNumber) throws IOException {
		fileLogWriter.write(recordLineNumber + ":" + error);
		fileLogWriter.write("\n");
	}

	protected void addRecordToLog(CSVRecord record, Map<String, Integer> headers) throws IOException {
		Object[] headerArray = new ArrayList<Object>(headers.keySet()).toArray();
		if (!headerAdded)
			csvFilePrinter.printRecord(headerArray);
		else
			csvFilePrinter.println();
		for (Entry<String, Integer> entry : headers.entrySet()) {
			csvFilePrinter.print(record.get(entry.getKey()));
		}
	}

}
