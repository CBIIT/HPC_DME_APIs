/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.exception.HpcException;

@Component
public class HPCCmdUploadToS3 extends HPCCmdClient {

	public HPCCmdUploadToS3() {
		super();
		inputCredentials = false;
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

	@Override
	protected boolean processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String accessKey, String secretKey, String authToken) throws HpcException {
		long start = System.currentTimeMillis();
		String filePath = (String) criteria.get("filepath");
		String presignedURL = (String) criteria.get("signedURL");
		try {
			System.out.println("Registering " + filePath);
			uploadToUrl(presignedURL, new File(filePath));
		} catch (Exception e) {
			String message = "Failed to process input file. Message: " + e.getMessage();
			try {
				addErrorToLog(filePath, message);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(filePath, exceptionAsString);
				addRecordToLog(filePath);
			} catch (IOException e1) {
				System.out.println("Failed to write log file: " + e.getMessage());
			}
		}
		long stop = System.currentTimeMillis();
		System.out.println("Execution time: " + ((stop - start) / 1000));
		return true;
	}

	protected boolean processCmdWorking(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String accessKey, String secretKey, String authToken) throws HpcException {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		String objectPath = (String) criteria.get("path");
		String filePath = (String) criteria.get("filepath");
		// String expiration = (String) criteria.get("expiration");
		String bucketName = "DSE-TestVault1";

		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3 s3Client = new AmazonS3Client(awsCredentials);
		s3Client.setEndpoint("http://fr-s-clvrsf-01.ncifcrf.gov");

		java.util.Date expiration = new java.util.Date();
		long msec = expiration.getTime();
		msec += 1000 * 60 * 60 * 24; // Add 1 hour.
		expiration.setTime(msec);

		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName,
				objectPath);
		generatePresignedUrlRequest.setMethod(HttpMethod.PUT);
		generatePresignedUrlRequest.setExpiration(expiration);
		try {
			URL s = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
			// uploadObject(s.toString(), filePath);
		} catch (Exception e) {
			System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
		}
		long stop = System.currentTimeMillis();
		System.out.print(" | Execution time (seconds): " + ((stop - start) / 1000));
		return true;
	}

	// Upload service
	public void uploadToUrl(String urlStr, File file) throws HpcBatchException {

		HttpURLConnection connection;
		try {
			URL url = new URL(urlStr);
			InputStream inputStream = new FileInputStream(file);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");
			connection.setChunkedStreamingMode(bufferSize);
			OutputStream out = connection.getOutputStream();

			byte[] buf = new byte[1024];
			int count;
			int total = 0;
			long fileSize = file.length();

			while ((count = inputStream.read(buf)) != -1) {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				out.write(buf, 0, count);
				total += count;
				int pctComplete = new Double(new Double(total) / new Double(fileSize) * 100).intValue();
				System.out.print("\r");
				System.out.print(String.format("PCT Complete: %d of " + fileSize + " bytes", pctComplete));
			}
			out.close();
			inputStream.close();

			int responseCode = connection.getResponseCode();

			if (responseCode == 200) {
				System.out.println("Successfully uploaded.");
			}
		} catch (IOException e) {
			throw new HpcBatchException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new HpcBatchException(e.getMessage(), e);
		}
	}
}
