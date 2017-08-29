/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.local.HpcLocalDirectoryListGenerator;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;

@Component
public class HPCCmdRegisterLocalFile extends HPCCmdClient {

	public HPCCmdRegisterLocalFile() {
		super();
	}

	protected void initializeLog() {
	}

	protected void createErrorLog() {
		if (logFile == null) {
			logFile = logDir + File.separator + "registerLocalPath_errorLog"
					+ new SimpleDateFormat("yyyyMMdd'.txt'").format(new Date());
		}
		if (fileLogWriter == null) {
			File file1 = new File(logFile);
			try {
				if (!file1.exists()) {
					file1.createNewFile();
				}
				fileLogWriter = new FileWriter(file1, true);
			} catch (IOException e) {
				System.out.println("Failed to initialize Batch process: " + e.getMessage());
				//e.printStackTrace();
			}
		}
	}

	protected void createRecordsLog(String fileName, String type) {
		if (fileName == null || fileName.isEmpty())
			fileName = logDir + File.separator + "registerLocalPath_Records"
					+ new SimpleDateFormat("yyyyMMdd").format(new Date());

		logRecordsFile = fileName;

		if (type != null && type.equalsIgnoreCase("csv"))
			logRecordsFile = logRecordsFile + ".csv";
		else if (type != null && type.equalsIgnoreCase("json"))
			logRecordsFile = logRecordsFile + ".json";
		else
			logRecordsFile = logRecordsFile + ".txt";
	}

	protected boolean processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String userId, String password, String authToken) {
		boolean success = true;
		String localPath = null;
		String excludePattern = null;
		String includePattern = null;
		String filePathBaseName = null;
		String destinationBasePath = null;
		boolean testRun = false;
		try {
			createErrorLog();

			localPath = (String) criteria.get("filePath");
			excludePattern = (String) criteria.get("excludePatternFile");
			includePattern = (String) criteria.get("includePatternFile");
			filePathBaseName = (String) criteria.get("filePathBaseName");
			destinationBasePath = (String) criteria.get("destinationBasePath");
			if(criteria.get("test") != null && criteria.get("test").equalsIgnoreCase("true"))
				testRun = true;
			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return false;
			}
			try {
				if(authToken == null)
					authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
						hpcCertPassword);
				
				if(authToken == null)
				{
					System.out.println("Failed to get authentication token. Aborting!");
					return false;
				}
				HpcLocalDirectoryListGenerator generator = new HpcLocalDirectoryListGenerator(hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, authToken,
						hpcCertPath, hpcCertPassword, validateMD5);
				success = generator.run(localPath, excludePattern, includePattern, filePathBaseName, destinationBasePath, logFile, logRecordsFile, testRun);
				logRecordsFile = null;
			} catch (Exception e) {
				createErrorLog();
				success = false;
				String message = "Failed to process cmd due to: " + e.getMessage();
				// System.out.println(message);
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			}

		} catch (Exception e) {
			System.out.println("Cannot read the input file");
			// e.printStackTrace();
		}
		return success;
	}

	protected void addErrorToLog(String error, String cmd) throws IOException {
		fileLogWriter.write(cmd + ": " + error);
		fileLogWriter.write("\n");
	}

}
