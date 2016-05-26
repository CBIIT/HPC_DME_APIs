package gov.nih.nci.hpc.cli;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.easybatch.core.mapper.RecordMappingException;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecord;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecordMapper;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;

public class HPCDataFileRecordMapper extends ApacheCommonCsvRecordMapper {

	Map<String, Integer> headersMap;
	private String basePath;
	private String hpcCertPath;
	private String hpcCertPassword;
	private String userId;
	private String password;
	private String authToken;
	private String logFile;
	private String errorRecordsFile;

	public HPCDataFileRecordMapper(Class recordClass, Map<String, Integer> headersMap, String basePath,
			String hpcCertPath, String hpcCertPassword, String userId, String password, String authToken, String logFile, String errorRecordsFile) {
		super(recordClass);
		this.headersMap = headersMap;
		this.basePath = basePath;
		this.hpcCertPath = hpcCertPath;
		this.hpcCertPassword = hpcCertPassword;
		this.userId = userId;
		this.password = password;
		this.authToken = authToken;
		this.logFile = logFile;
		this.errorRecordsFile = errorRecordsFile;
	}

	@Override
    public GenericRecord processRecord(final ApacheCommonCsvRecord record) throws RecordMappingException {
        CSVRecord csvRecord = record.getPayload();
        String objectPath = null;
		List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
		HpcFileLocation source = new HpcFileLocation();
		for (Entry<String, Integer> entry : headersMap.entrySet()) {
			String cellVal = csvRecord.get(entry.getKey());
			HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
			hpcMetadataEntry.setAttribute(entry.getKey());
			hpcMetadataEntry.setValue(cellVal);
			if (entry.getKey().equals("fileContainerId")) {
				source.setFileContainerId(cellVal);
				continue;
			} else if (entry.getKey().equals("fileId")) {
				source.setFileId(cellVal);
				continue;
			} else if (entry.getKey().equals("object_path")) {
				objectPath = cellVal;
				continue;
			}
			if (StringUtils.isNotBlank(cellVal))
				listOfhpcCollection.add(hpcMetadataEntry);
		}
		//System.out.println("Processing record: "+objectPath);

		HPCDataObject dataObject = new HPCDataObject();
		dataObject.setAuthToken(authToken);
		dataObject.setBasePath(basePath);
		dataObject.setHpcCertPassword(hpcCertPassword);
		dataObject.setHpcCertPath(hpcCertPath);
		dataObject.setPassword(password);
		dataObject.setUserId(userId);
		dataObject.setLogFile(logFile);
		dataObject.setErrorRecordsFile(errorRecordsFile);
		dataObject.setHeadersMap(headersMap);
		dataObject.setCsvRecord(csvRecord);
		
		HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO = new HpcDataObjectRegistrationDTO();
//		if (!objectPath.startsWith("/"))
//			objectPath = "/" + objectPath;
		dataObject.setObjectPath(objectPath);

		hpcDataObjectRegistrationDTO.setSource(source);
		hpcDataObjectRegistrationDTO.setCallerObjectId("/");
		hpcDataObjectRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);   
		dataObject.setDto(hpcDataObjectRegistrationDTO);
        return new GenericRecord(record.getHeader(), dataObject);
    }

}
