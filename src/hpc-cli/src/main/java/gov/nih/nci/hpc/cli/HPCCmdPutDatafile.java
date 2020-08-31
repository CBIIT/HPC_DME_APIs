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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.easybatch.core.processor.RecordProcessingException;
import org.springframework.stereotype.Component;
import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.local.HpcLocalFileProcessor;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;

@Component
public class HPCCmdPutDatafile extends HPCCmdClient {

	public HPCCmdPutDatafile() {
		super();
	}

	protected void initializeLog() {
		logFile = logDir + File.separator + "putDatafile_errorLog"
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
		logRecordsFile = logDir + File.separator + "putDatafile_errorRecords"
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
	}

	protected void createErrorLog() {
		if (logFile == null) {
			File file1 = new File(logFile);
			try {
				if (!file1.exists()) {
					file1.createNewFile();
				}
				fileLogWriter = new FileWriter(file1, true);
			} catch (IOException e) {
				System.out.println("Failed to initialize Batch process: " + e.getMessage());
			}
		}
	}

	protected String processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String userId, String password, String authToken) {
		String returnCode = null;

		initializeLog();
		try {

			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return Constants.CLI_2;
			}

			try {
				if(authToken == null)
					authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
						hpcCertPassword);

				if (cmd.equals("putDatafile")) {
					
					String sourcePath = (String) criteria.get("sourcePath");
					String destinationPath = (String) criteria.get("destinationPath");
					String metadataFile = (String) criteria.get("metadataFile");
					String archiveType = (String) criteria.get("archiveType");
					Path path = Paths.get(sourcePath);
					
					HpcPathAttributes filePathAttr = new HpcPathAttributes();
					String fullPath = path.toAbsolutePath().toString();
					fullPath = fullPath.replace("\\", "/");
					filePathAttr.setAbsolutePath(path.toString());
					filePathAttr.setPath(path.getFileName().toString());
					String name = path.getFileName().toString();
					filePathAttr.setName(name);
					File fileToCheckDir = new File(sourcePath);
					filePathAttr.setIsDirectory(fileToCheckDir.isDirectory());
					filePathAttr.setSize(fileToCheckDir.length());
					
					if(fileToCheckDir.isDirectory()) {
						System.out.println("Source file is a directory");
						return Constants.CLI_2;
					} else {
						System.out.println("Registering file: " + sourcePath);
						System.out.println("Destination archive path: " + destinationPath);
					}
					HpcServerConnection connection = new HpcServerConnection(hpcServerURL, hpcServerProxyURL,
							hpcServerProxyPort, authToken, hpcCertPath, hpcCertPassword);
					
					HPCDataObject dataObject = new HPCDataObject();
					dataObject.setAuthToken(authToken);
					dataObject.setConnection(connection);
					dataObject.setMaxAttempts(maxAttempts);
					dataObject.setBackOffPeriod(backOffPeriod);
					dataObject.setMultipartPoolSize(multipartPoolSize);
					dataObject.setMultipartThreshold(multipartThreshold);
					dataObject.setMultipartChunksize(multipartChunksize);
					
					HpcLocalFileProcessor fileProcess;
					fileProcess = new HpcLocalFileProcessor(dataObject);
					fileProcess.process(filePathAttr, path.toString(), null, destinationPath, logFile,
							logRecordsFile, false, false, (archiveType != null && archiveType.equalsIgnoreCase("POSIX") ? false : true), false, metadataFile);
				}
			}  catch (RecordProcessingException e) {
				createErrorLog();
				returnCode = Constants.CLI_5;
				String message = "Failed to process cmd due to: " + e.getMessage();
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			} catch (IOException e) {
				createErrorLog();
				returnCode = Constants.CLI_5;
				String message = "Failed to process cmd due to: " + e.getMessage();
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			} catch (Exception e) {
				createErrorLog();
				returnCode = Constants.CLI_5;
				String message = "Failed to process cmd due to: " + e.getMessage();
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
}
