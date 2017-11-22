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

import gov.nih.nci.hpc.exception.HpcException;

@Component
public class HPCCmdSignedS3URL extends HPCCmdClient {

	public HPCCmdSignedS3URL() {
		super();
	}

	protected void initializeLog() {
	}

	protected void createErrorLog() {
		if (logFile == null) {
			logFile = logDir + File.separator + "getDatafiles_errorLog"
					+ new SimpleDateFormat("yyyyMMdd'.txt'").format(new Date());
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

	protected void createRecordsLog(String fileName, String type) {
		if (fileName == null || fileName.isEmpty())
			fileName = logDir + File.separator + "getDatafiles_Records"
					+ new SimpleDateFormat("yyyyMMdd").format(new Date());

		logRecordsFile = fileName;

		if (type != null && type.equalsIgnoreCase("csv"))
			logRecordsFile = logRecordsFile + ".csv";
		else if (type != null && type.equalsIgnoreCase("json"))
			logRecordsFile = logRecordsFile + ".json";
		else
			logRecordsFile = logRecordsFile + ".txt";

		File file2 = new File(logRecordsFile);
		try {
			if (!file2.exists()) {
				file2.createNewFile();
			}
			fileRecordWriter = new FileWriter(file2, false);
		} catch (IOException e) {
			System.out.println("Failed to initialize output file: " + e.getMessage());
		}
	}

	@Override
	protected boolean processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
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
		msec += 1000 * 60 * 60; // Add 1 hour.
		expiration.setTime(msec);

		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName,
				objectPath);
		generatePresignedUrlRequest.setMethod(HttpMethod.PUT);
		generatePresignedUrlRequest.setExpiration(expiration);
		try {
			URL s = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
			System.out.println(s.toString());
		} catch (Exception e) {
			System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
		}
		long stop = System.currentTimeMillis();
		System.out.println("Execution time: " + ((stop - start) / 1000));
		return true;
	}
}
