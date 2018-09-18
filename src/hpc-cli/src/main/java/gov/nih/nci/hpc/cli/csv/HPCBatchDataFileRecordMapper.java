/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.csv;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.easybatch.core.mapper.RecordMappingException;
import org.easybatch.core.record.GenericRecord;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecord;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecordMapper;

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

  private HpcFileLocation origFileSource;
  private List<HpcMetadataEntry> metadataAttributes;
  private List<HpcMetadataEntry> parentMetadataAttributes;
  private CSVRecord csvRecord;


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
		throws RecordMappingException {
    this.csvRecord = record.getPayload();
    initOrClearMetadataLists();
    this.origFileSource = null;
    String objectPath = null;
		boolean createParentCollection = false;
    for (Entry<String, Integer> entry : headersMap.entrySet()) {
      String attribName = entry.getKey();
      String attribVal = this.csvRecord.get(attribName);
      if (accumulateFileSourceDetails(attribName, attribVal)) {
        continue;
      } else if (Constants.CREATE_PARENT_COLLECTION.equals(attribName)) {
        createParentCollection = Boolean.valueOf(attribVal);
      } else {
        Optional<String> optDestPath = obtainObjectDestPath(attribName,
          attribVal);
        if (optDestPath.isPresent()) {
          objectPath = optDestPath.get();
          continue;
        }
			}
      accumulateMetadata(attribName, attribVal);
    }
    HPCDataObject dataObject = buildDataObjModel(objectPath,
      buildDataObjRegRqstDto(createParentCollection));

		return new GenericRecord(record.getHeader(), dataObject);
	}


	private boolean accumulateFileSourceDetails(String metaAttribName,
    String metaAttribVal) {
    if (null == this.origFileSource) {
      this.origFileSource = new HpcFileLocation();
    }
    boolean sourceDetailAccumulated = false;
    if ("fileContainerId".equals(metaAttribName)) {
      this.origFileSource.setFileContainerId(metaAttribVal);
      sourceDetailAccumulated = true;
    } else if ("fileId".equals(metaAttribName)) {
      this.origFileSource.setFileId(metaAttribVal);
      sourceDetailAccumulated = true;
    }

    return sourceDetailAccumulated;
  }


  private void accumulateMetadata(String metaAttribName, String metaAttribVal) {
    if (StringUtils.isNotBlank(metaAttribVal)) {
      HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
      hpcMetadataEntry.setAttribute(metaAttribName);
      hpcMetadataEntry.setValue(metaAttribVal);
      if (metaAttribName.startsWith(Constants.PARENT_COLLECTION_PREFIX)) {
        this.parentMetadataAttributes.add(hpcMetadataEntry);
      } else {
        this.metadataAttributes.add(hpcMetadataEntry);
      }
    }
  }


  private HPCDataObject buildDataObjModel(String dmeDestPath,
    HpcDataObjectRegistrationRequestDTO dataObjRegRqstDto) {
    HPCDataObject dataObject = new HPCDataObject();
    dataObject.setAuthToken(this.authToken);
    dataObject.setBasePath(this.basePath);
    dataObject.setHpcCertPassword(this.hpcCertPassword);
    dataObject.setHpcCertPath(this.hpcCertPath);
    dataObject.setPassword(this.password);
    dataObject.setUserId(this.userId);
    dataObject.setLogFile(this.logFile);
    dataObject.setErrorRecordsFile(this.errorRecordsFile);
    dataObject.setHeadersMap(this.headersMap);
    dataObject.setCsvRecord(this.csvRecord);
    dataObject.setObjectPath(dmeDestPath);
    dataObject.setDto(dataObjRegRqstDto);

    return dataObject;
  }


  private HpcDataObjectRegistrationRequestDTO buildDataObjRegRqstDto(
    boolean createParentFlag) {
    HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO = new
      HpcDataObjectRegistrationRequestDTO();
    hpcDataObjectRegistrationDTO.setGenerateUploadRequestURL(true);
    hpcDataObjectRegistrationDTO.setCallerObjectId("/");
    hpcDataObjectRegistrationDTO.setSource(this.origFileSource);
    hpcDataObjectRegistrationDTO.getMetadataEntries().addAll(
      this.metadataAttributes);
    hpcDataObjectRegistrationDTO.setCreateParentCollections(createParentFlag);
    if (!this.parentMetadataAttributes.isEmpty()) {
      hpcDataObjectRegistrationDTO.getParentCollectionsBulkMetadataEntries().getDefaultCollectionMetadataEntries()
                                  .addAll(this.parentMetadataAttributes);
    }

    return hpcDataObjectRegistrationDTO;
  }


  private void initOrClearMetadataLists() {
    if (null == this.metadataAttributes) {
      this.metadataAttributes = new ArrayList<HpcMetadataEntry>();
    } else {
      this.metadataAttributes.clear();
    }
    if (null == this.parentMetadataAttributes) {
      this.parentMetadataAttributes = new ArrayList<HpcMetadataEntry>();
    } else {
      this.parentMetadataAttributes.clear();
    }
  }


  private Optional<String> obtainObjectDestPath(String someAttribNm,  String
    someAttribVal) {
    Optional<String> retVal = ("object_path".equals(someAttribNm)) ?
      Optional.of(someAttribVal) : Optional.empty();
    return retVal;
  }


  private void record2ErrorLogs(String errorMessage) {
    StringBuilder sb = new StringBuilder();
    sb.append(this.csvRecord.toString())
      .append("\n");
    String entry = sb.toString();
    HpcLogWriter.getInstance().WriteLog(this.errorRecordsFile, entry);
    HpcLogWriter.getInstance().WriteLog(this.logFile, errorMessage);
  }

}