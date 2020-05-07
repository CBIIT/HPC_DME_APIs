/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.download;

import java.util.Date;
import java.util.Map;

import org.easybatch.core.mapper.AbstractRecordMapper;
import org.easybatch.core.mapper.RecordMapper;
import org.easybatch.core.mapper.RecordMappingException;
import org.easybatch.core.record.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.domain.HPCLocalFileRecord;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;

public class HPCBatchCollectionDownloadRecordMapper extends AbstractRecordMapper
		implements RecordMapper<HPCBatchCollectionDownloadRecord, GenericRecord> {
	protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

	Map<String, Integer> headersMap;
	private HpcServerConnection connection;
	private String logFile;
	private String errorRecordsFile;
	private Map<String, String> criteriaMap;

	public HPCBatchCollectionDownloadRecordMapper(Class recordClass, Map<String, String> criteriaMap,
			HpcServerConnection connection, String logFile, String errorRecordsFile) {
		super(recordClass);
		this.logFile = logFile;
		this.connection = connection;
		this.errorRecordsFile = errorRecordsFile;
		this.criteriaMap = criteriaMap;
	}

	@Override
	public GenericRecord processRecord(HPCBatchCollectionDownloadRecord record) throws RecordMappingException {
		logger.debug("HPCBatchCollectionDownloadRecordMapper: processRecord: " + record.toString());

		System.out.println("Including: " + record.getPayload().getPath());
		HPCDataObject dataObject = new HPCDataObject();
		dataObject.setLogFile(logFile);
		dataObject.setErrorRecordsFile(errorRecordsFile);
		dataObject.setHeadersMap(headersMap);
		dataObject.setBasePath(record.getPayload().getPath());
		dataObject.setCriteriaMap(criteriaMap);
		dataObject.setConnection(connection);
		org.easybatch.core.record.Header header = new org.easybatch.core.record.Header(record.getRecordNumber(),
				record.getPayload().getPath(), new Date());
		return new GenericRecord(header, dataObject);
	}
}
