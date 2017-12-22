/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.globus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.HPCCmdClient;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

@Component
public class HPCCmdRegisterGlobusFile extends HPCCmdClient {

	public HPCCmdRegisterGlobusFile() {
		super();
	}

	protected void initializeLog() {
	}

	protected void createErrorLog() {
		if (logFile == null) {
			logFile = logDir + File.separator + "registerGlobusPath_errorLog"
					+ new SimpleDateFormat("yyyyMMdd'.txt'").format(new Date());
		}
	}

	protected void createRecordsLog(String fileName, String type) {
		if (fileName == null || fileName.isEmpty())
			fileName = logDir + File.separator + "registerGlobusPath_Records"
					+ new SimpleDateFormat("yyyyMMdd").format(new Date());

		logRecordsFile = fileName;

		if (type != null && type.equalsIgnoreCase("csv"))
			logRecordsFile = logRecordsFile + ".csv";
		else if (type != null && type.equalsIgnoreCase("json"))
			logRecordsFile = logRecordsFile + ".json";
		else
			logRecordsFile = logRecordsFile + ".txt";
	}

	protected String processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String userId, String password, String authToken) throws HpcException {
		String returnCode = null;
		String globusUserId = null;
		String globusPassword = null;
		String globusToken = null;
		String globusEndpoint = null;
		String globusPath = null;
		String basePath = null;

		try {
			createErrorLog();

			globusEndpoint = (String) criteria.get("globusEndpoint");
			globusPath = (String) criteria.get("globusPath");
			basePath = (String) criteria.get("basePath");
			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return Constants.CLI_2;
			}
			try {
				if (authToken == null)
					authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL,
							hpcServerProxyPort, hpcCertPath, hpcCertPassword);
				HpcServerConnection connection = new HpcServerConnection(hpcServerURL, hpcServerProxyURL, hpcServerProxyPort,
						authToken, hpcCertPath, hpcCertPassword);
				HpcGlobusDirectoryListProcessor generator = new HpcGlobusDirectoryListProcessor(connection);
				returnCode = generator.run(criteria, basePath, logFile, logRecordsFile);
				logRecordsFile = null;
			} catch (Exception e) {
				createErrorLog();
				returnCode = Constants.CLI_5;
				String message = "Failed to process cmd due to: " + e.getMessage();
				// System.out.println(message);
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return returnCode;
	}

	protected void addErrorToLog(String error, String cmd) throws IOException {
		fileLogWriter.write(cmd + ": " + error);
		fileLogWriter.write("\n");
	}

}
