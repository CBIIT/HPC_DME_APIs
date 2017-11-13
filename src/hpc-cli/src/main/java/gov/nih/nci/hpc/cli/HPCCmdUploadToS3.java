/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.util.StreamUtils;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.google.gson.Gson;

import gov.nih.nci.hpc.cli.domain.HPCDataFileRecord;
import gov.nih.nci.hpc.cli.util.CsvFileWriter;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.cli.util.MultipartUtil;
import gov.nih.nci.hpc.cli.util.MultipartUtilityV2;

@Component
public class HPCCmdUploadToS3 extends HPCCmdClient {

	public HPCCmdUploadToS3() {
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
				e.printStackTrace();
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
			e.printStackTrace();
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
		msec += 1000 * 60 * 60 * 24; // Add 1 hour.
		expiration.setTime(msec);

		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName,
				objectPath);
		generatePresignedUrlRequest.setMethod(HttpMethod.PUT);
		generatePresignedUrlRequest.setExpiration(expiration);
		try {
			URL s = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
			//System.out.println(s.toString());

//			uploadToUrl(s, new File(filePath));
			s3Client.putObject(
					bucketName, 
					objectPath,
					new File(filePath)
					);
			
//			HttpURLConnection connection=(HttpURLConnection) s.openConnection();
//			connection.setDoOutput(true);
//			connection.setRequestMethod("PUT");
//			BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
//			InputStream inputStream = new BufferedInputStream(
//					new FileInputStream(filePath));
//			copyInputStreamToFile(inputStream, out);
//			int responseCode = connection.getResponseCode();
//			System.out.println("Service returned response code " + responseCode);
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
		}
		long stop = System.currentTimeMillis();
		System.out.println("Execution time: " + ((stop - start) / 1000));
		return true;
	}

	public void uploadObject(String urlString, String filePath) {
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(filePath));

			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");
			BufferedOutputStream outstream = new BufferedOutputStream(connection.getOutputStream());
			copyInputStreamToFile(inputStream, outstream);
			int responseCode = connection.getResponseCode();
			System.out.println("Service returned response code " + responseCode);
		} catch (IOException e) {
			System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
			}
		}

	}

	private void copyInputStreamToFile(InputStream in, BufferedOutputStream out) {
		try {
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//Upload service
	public void uploadToUrl(URL url, File file) {

	    HttpURLConnection connection;
	    try {
	        InputStream inputStream = new FileInputStream(file);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.setDoOutput(true);
	        connection.setRequestMethod("PUT");
	        OutputStream out =
	                connection.getOutputStream();

	        byte[] buf = new byte[1024];
	        int count;
	        int total = 0;
	        long fileSize = file.length();

	        while ((count = inputStream.read(buf)) != -1)
	        {
	            if (Thread.interrupted())
	            {
	                throw new InterruptedException();
	            }
	            out.write(buf, 0, count);
	            total += count;
	            int pctComplete = new Double(new Double(total) / new Double(fileSize) * 100).intValue();

	            System.out.print("\r");
	            System.out.print(String.format("PCT Complete: %d", pctComplete));
	        }
	        System.out.println();
	        out.close();
	        inputStream.close();

	        System.out.println("Finishing...");
	        int responseCode = connection.getResponseCode();

	        if (responseCode == 200) {
	            System.out.printf("Successfully uploaded.");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    }
	}	
//	URL s = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
//	System.out.println(s.toString());
//	//MultipartUtil multipartUtil = new MultipartUtil(s.toString(), "UTF-8");
//	//multipartUtil.addFilePart("file", new File(filePath));
//	//List<String> response = multipartUtil.finish();
//	MultipartUtilityV2 v2 = new MultipartUtilityV2(s.toString());
//	v2.addFilePart("file", new File(filePath));
//	String response = v2.finish();
//	System.out.println("Response: "+response);
//	// Use the pre-signed URL to upload an object.
//	//uploadObject(s.toString(), filePath);
//	} catch (IOException e) {
//		e.printStackTrace();
//		System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
//	} catch (Exception e) {
//		e.printStackTrace();
//		System.out.println("Failed to read input file from: " + filePath + " Message: " + e.getMessage());
//	}
	
}
