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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.globus.HpcGlobusDirectoryListGenerator;
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

	protected boolean processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String userId, String password, String authToken) throws HpcException{
		boolean success = true;
		String globusUserId = null;
		String globusPassword = null;
		String globusToken = null;
		String globusEndpoint = null;
		String globusPath = null;
		String basePath = null;
		if(globusLoginFile != null)
		{
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(new FileReader(globusLoginFile));
				String line = bufferedReader.readLine();
				if (line.isEmpty())
					throw new HpcException("Invalid Globus Login credentials in " + globusLoginFile, HpcErrorType.INVALID_REQUEST_INPUT);
				else {
					if (line.indexOf(":") == -1)
						throw new HpcException("Invalid Globus Login credentials in " + globusLoginFile, HpcErrorType.INVALID_REQUEST_INPUT);
					else {
						globusUserId = line.substring(0, line.indexOf(":"));
						globusToken = line.substring(line.indexOf(":") + 1);
					}
				}
			} catch (FileNotFoundException e) {
				createErrorLog();
				String message = "Failed to process Globus login credentials file: " + e.getMessage();
				try {
					addErrorToLog(message, cmd);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				try {
					addErrorToLog(message, exceptionAsString);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return false;
			} catch (IOException e) {
				createErrorLog();
				String message = "Failed to process Globus login credentials file: " + e.getMessage();
				try {
					addErrorToLog(message, cmd);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				try {
					addErrorToLog(message, exceptionAsString);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return false;
			}
			finally
			{
				if(bufferedReader != null)
					try {
						bufferedReader.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
		}
		if(globusLoginFile == null)
		{
			try {
				jline.console.ConsoleReader reader = new jline.console.ConsoleReader();
				reader.setExpandEvents(false);
				System.out.println("Enter Globus UserId:");
				globusUserId = reader.readLine();
	
				System.out.println("Enter Globus password:");
				globusPassword = reader.readLine(new Character('*'));
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		try {
			createErrorLog();

			globusEndpoint = (String) criteria.get("globusEndpoint");
			globusPath = (String) criteria.get("globusPath");
			basePath = (String) criteria.get("basePath");
			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return false;
			}
			try {
				if(authToken == null)
					authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
						hpcCertPassword);
				HpcGlobusDirectoryListGenerator generator = new HpcGlobusDirectoryListGenerator(hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, authToken,
						hpcCertPath, hpcCertPassword);
				success = generator.run(globusNexusURL, globusURL, globusEndpoint, globusPath, globusUserId,
						globusPassword, globusToken, basePath, logFile, logRecordsFile);
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
			e.printStackTrace();
		}
		return success;
	}

	protected void addErrorToLog(String error, String cmd) throws IOException {
		fileLogWriter.write(cmd + ": " + error);
		fileLogWriter.write("\n");
	}

}
