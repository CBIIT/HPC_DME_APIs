/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.csv;

import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.easybatch.core.mapper.RecordMappingException;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecord;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecordMapper;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;

public class HPCBatchDataFileRecordMapper extends ApacheCommonCsvRecordMapper {

	Map<String, Integer> headersMap;
	private String basePath;
	private String hpcCertPath;
	private String hpcCertPassword;
	private String userId;
	private String password;
	private String authToken;
	private String logFile;
	private String errorRecordsFile;

	public HPCBatchDataFileRecordMapper(Class recordClass, Map<String, Integer> headersMap, String basePath,
			String hpcCertPath, String hpcCertPassword, String userId, String password, String authToken,
			String logFile, String errorRecordsFile) {
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
	public GenericRecord processRecord(final ApacheCommonCsvRecord record)
		throws RecordMappingException, HpcCmdException {
		CSVRecord csvRecord = record.getPayload();
		String objectPath = null;
		List<HpcMetadataEntry> metadataAttributes = new ArrayList<HpcMetadataEntry>();
		List<HpcMetadataEntry> parentMetadataAttributes = new ArrayList<HpcMetadataEntry>();
		boolean createParentCollection = false;
		HpcFileLocation source = new HpcFileLocation();
		for (Entry<String, Integer> entry : headersMap.entrySet()) {
			String cellVal = csvRecord.get(entry.getKey());
			HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
			hpcMetadataEntry.setAttribute(entry.getKey());
			hpcMetadataEntry.setValue(cellVal);
			if (entry.getKey().equals(Constants.CREATE_PARENT_COLLECTION))
				createParentCollection = entry.getKey().equalsIgnoreCase("true");

			if (entry.getKey().equals("fileContainerId")) {
				source.setFileContainerId(cellVal);
				continue;
			} else if (entry.getKey().equals("fileId")) {
				source.setFileId(cellVal);
				continue;
			} else if (entry.getKey().equals("object_path")) {
				objectPath = cellVal;
				// validateDmeArchivePath method below could throw HpcCmdException
				try {
					HpcClientUtil.validateDmeArchivePath(objectPath);
				} catch (HpcCmdException e) {
					String enhancedMsg = "Failed to process record in CSV file;" +
						" reason: [" + e.getMessage() + "]\n" +
						"  Echo CSV record: [" + csvRecord.toString() + "]\n\n";
					throw new HpcCmdException(enhancedMsg);
				}
				continue;
			}
			if (StringUtils.isNotBlank(cellVal)) {
				if (entry.getKey().startsWith(Constants.PARENT_COLLECTION_PREFIX))
					parentMetadataAttributes.add(hpcMetadataEntry);
				else
					metadataAttributes.add(hpcMetadataEntry);
			}
		}

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
		dataObject.setObjectPath(objectPath);

		HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO = new HpcDataObjectRegistrationRequestDTO();
		hpcDataObjectRegistrationDTO.setSource(source);
		hpcDataObjectRegistrationDTO.setCallerObjectId("/");
		hpcDataObjectRegistrationDTO.getMetadataEntries().addAll(metadataAttributes);
		hpcDataObjectRegistrationDTO.setGenerateUploadRequestURL(true);
		dataObject.setDto(hpcDataObjectRegistrationDTO);
		hpcDataObjectRegistrationDTO.setCreateParentCollections(createParentCollection);
		if (!parentMetadataAttributes.isEmpty())
			hpcDataObjectRegistrationDTO.getParentCollectionMetadataEntries().addAll(parentMetadataAttributes);
		return new GenericRecord(record.getHeader(), dataObject);
	}
}
