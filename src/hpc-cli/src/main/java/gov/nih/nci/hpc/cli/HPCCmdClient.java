/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.exception.HpcException;

@Component
public abstract class HPCCmdClient {
	@Autowired
	protected HpcConfigProperties configProperties;
	protected String hpcServerURL;
	protected String hpcServerProxyURL;
	protected String hpcServerProxyPort;
	protected String globusNexusURL;
	protected String globusURL;
	protected String hpcCertPath;
	protected String hpcCertPassword;
	protected String hpcDataService;
	protected String hpcCollectionService;
	protected String logDir;
	protected FileWriter fileLogWriter = null;
	protected FileWriter fileRecordWriter = null;
	protected String loginFile = null;
	protected String globusLoginFile = null;
	protected String logFile = null;
	protected String tokenFile = null;
	protected String logRecordsFile = null;
	protected boolean headerAdded = false;
	protected boolean inputCredentials = true;
	protected int bufferSize = 0;
	public HPCCmdClient() {

	}

	protected void preprocess() {
		hpcServerURL = configProperties.getProperty("hpc.server.url");
		hpcServerProxyURL = configProperties.getProperty("hpc.server.proxy.url");
		hpcServerProxyPort = configProperties.getProperty("hpc.server.proxy.port");
		globusNexusURL = configProperties.getProperty("globus.nexus.url");
		globusURL = configProperties.getProperty("globus.url");
		hpcServerURL = configProperties.getProperty("hpc.server.url");
		hpcDataService = configProperties.getProperty("hpc.dataobject.service");
		hpcCertPath = configProperties.getProperty("hpc.ssl.keystore.path");
		hpcCertPassword = configProperties.getProperty("hpc.ssl.keystore.password");
		logDir = configProperties.getProperty("hpc.error-log.dir");
		loginFile = configProperties.getProperty("hpc.login.credentials");
		tokenFile = configProperties.getProperty("hpc.login.token");
		globusLoginFile = configProperties.getProperty("hpc.globus.login.token");
		hpcCollectionService = configProperties.getProperty("hpc.collection.service");
		String bufferSizeStr = configProperties.getProperty("upload.buffer.size");
		bufferSize = Integer.parseInt(bufferSizeStr);
		
		String basePath = System.getProperty("HPC_DM_UTILS");
		if(basePath == null)
		{
			System.out.println("System Property HPC_DM_UTILS is missing");
			System.exit(1);
		}
		if(tokenFile != null)
			tokenFile = basePath + File.separator + tokenFile;
		if(loginFile != null)
			loginFile = basePath + File.separator + loginFile;
		globusLoginFile = basePath + File.separator + globusLoginFile;
		hpcCertPath = basePath + File.separator + hpcCertPath;
		
		initializeLog();
	}

	protected abstract void initializeLog();

	public String process(String cmd, Map<String, String> criteria, String outputFile, String format, String detail) {
		preprocess();
		BufferedReader bufferedReader = null;
		try {
			String userId = null;
			String password = null;
			String authToken = null;
			if (loginFile == null && tokenFile == null && inputCredentials) {
				jline.console.ConsoleReader reader = new jline.console.ConsoleReader();
				reader.setExpandEvents(false);
				System.out.println("Enter NCI Login UserId:");
				userId = reader.readLine();

				System.out.println("Enter NCI Login password:");
				password = reader.readLine(new Character('*'));
			} else if ( tokenFile != null){
				bufferedReader = new BufferedReader(new FileReader(tokenFile));
				String line = bufferedReader.readLine();
				if (line.isEmpty())
				{
					System.out.println("Invalid Login token in " + tokenFile);
					return Constants.CLI_1;
				}
				else {
					authToken = line;
				}
			} else if ( loginFile != null){
				bufferedReader = new BufferedReader(new FileReader(loginFile));
				String line = bufferedReader.readLine();
				if (line.indexOf(":") == -1)
				{
					System.out.println("Invalid Login credentials in " + loginFile);
					return Constants.CLI_1;
				}
				else {
					userId = line.substring(0, line.indexOf(":"));
					password = line.substring(line.indexOf(":") + 1);
				}
			}
			try
			{
				String returnCode = processCmd(cmd, criteria, outputFile, format, detail, userId, password, authToken);

			if (returnCode == null || returnCode.equals(Constants.CLI_SUCCESS))
			{
				System.out.println("Cmd process Successful");
				return Constants.CLI_SUCCESS;
			}
			else
			{
				System.out.println("Cmd process is not Successful. Please refer to error log for details.");
				return returnCode;
			}
			}
			catch(HpcException e)
			{
				addErrorToLog("Faile to process: " +e.getMessage(), cmd);
				System.out.println("Cmd process is not Successful. Please refer to error log for details.");
				return Constants.CLI_1;
			}
		} catch (IOException e) {
			System.out.println("Failed to run command: "+e.getMessage());
			return Constants.CLI_1;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
				}
			}
			postProcess();
		}
	}

	protected abstract String processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String userId, String password, String authToken) throws HpcException;

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
		} catch (IOException e) {
		}

	}

	private void writeException(Exception e, String message, String exceptionAsString) {
		HpcLogWriter.getInstance().WriteLog(logFile, message);
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		if (exceptionAsString == null)
			exceptionAsString = sw.toString();
		HpcLogWriter.getInstance().WriteLog(logFile, exceptionAsString);
	}

	private void writeRecord(String filePath) {
		HpcLogWriter.getInstance().WriteLog(logRecordsFile, filePath);
	}
	
	protected void addErrorToLog(String error, int recordLineNumber) throws IOException {
		fileLogWriter.write(recordLineNumber + ": " + error);
		fileLogWriter.write("\n");
	}

	protected void addRecordToLog(String record) throws IOException {
		fileRecordWriter.write(record);
		fileRecordWriter.write("\n");
	}

	protected void addErrorToLog(String error, String cmd) throws IOException {
		fileLogWriter.write(cmd + ": " + error);
		fileLogWriter.write("\n");
	}
}
