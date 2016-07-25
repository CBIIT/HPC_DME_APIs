package gov.nih.nci.hpc.cli;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

@Component
public class HPCDatafiles extends HPCBatchClient {
	@Autowired
	private HpcConfigProperties configProperties;

	public HPCDatafiles() {
		super();
	}

	protected void initializeLog() {
		logFile = logDir + File.separator + "putDatafiles_errorLog"
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
		logRecordsFile = logDir + File.separator + "putDatafiles_errorRecords"
				+ new SimpleDateFormat("yyyyMMddhhmm'.csv'").format(new Date());
	}

	protected boolean processFile(String fileName, String userId, String password) {
		boolean success = true;

		if(userId == null || userId.trim().length() == 0 || password == null || password.trim().length() == 0)
		{
			System.out.println("Invalid login credentials");
			return false;
		}
		try {
			String authToken =  HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL);
			success = new HPCDataFileProcessor(fileName, threadCount, hpcServerURL+ "/" + hpcDataService,
					hpcCertPath, hpcCertPassword, null, null, logFile, logRecordsFile, authToken).processData();
		} catch (Exception e) {
			System.out.println("Cannot read the input file");
			e.printStackTrace();
			return false;
		}
		return success;

	}
}
