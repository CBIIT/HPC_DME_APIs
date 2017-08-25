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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
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
			if (loginFile == null && tokenFile == null) {
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
					return "Invalid Login token in " + tokenFile;
				else {
					authToken = line;
				}
			} else if ( loginFile != null){
				bufferedReader = new BufferedReader(new FileReader(loginFile));
				String line = bufferedReader.readLine();
				if (line.indexOf(":") == -1)
					return "Invalid Login credentials in " + loginFile;
				else {
					userId = line.substring(0, line.indexOf(":"));
					password = line.substring(line.indexOf(":") + 1);
				}
			}
			try
			{
			boolean success = processCmd(cmd, criteria, outputFile, format, detail, userId, password, authToken);

			if (success)
				return "Cmd process Successful";
			else
				return "Cmd process is not Successful. Please error log for details.";
			}
			catch(HpcException e)
			{
				addErrorToLog("Faile to process: " +e.getMessage(), cmd);
				return "Cmd process is not Successful. Please error log for details.";
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "Failed to run command";
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			postProcess();
		}
	}

	protected abstract boolean processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
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

	protected void addErrorToLog(String error, String cmd) throws IOException {
		fileLogWriter.write(cmd + ": " + error);
		fileLogWriter.write("\n");
	}
}
