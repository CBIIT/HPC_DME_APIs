/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import java.io.IOException;
import java.util.Map;

import org.easybatch.core.processor.RecordProcessingException;
import org.easybatch.core.processor.RecordProcessor;
import org.easybatch.core.record.Record;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;

public class HPCBatchLocalFileRecordProcessor implements RecordProcessor {

	@Override
	public Record processRecord(Record record) throws RecordProcessingException {

		HPCDataObject dataObject = (HPCDataObject) record.getPayload();
		Map<String, String> criteriaMap = dataObject.getCriteriaMap();
		String filePathBaseName = criteriaMap.get("filePathBaseName");
		String filePath = criteriaMap.get("filePath");
		String destinationBasePath = criteriaMap.get("destinationBasePath");
		String metadata = criteriaMap.get("metadata");
		String checksumStr = criteriaMap.get("checksum");
		boolean checksum = true;
		if(checksumStr != null && checksumStr.equalsIgnoreCase("false"))
			checksum = false;
		
		String archiveType = criteriaMap.get("archiveType");
		boolean metadataOnly = metadata != null && metadata.equals("true");
		System.out.println("Processing " + dataObject.getDataFilePathAttrs().getAbsolutePath());
		try {
			HpcPathAttributes pathAttr = dataObject.getDataFilePathAttrs();
			if (!pathAttr.getIsDirectory()) {
				HpcLocalFileProcessor fileProcess;
				fileProcess = new HpcLocalFileProcessor(dataObject.getConnection());
				fileProcess.process(pathAttr, filePath, filePathBaseName, destinationBasePath, dataObject.getLogFile(),
						dataObject.getErrorRecordsFile(), metadataOnly, (archiveType != null && archiveType.equalsIgnoreCase("POSIX") ? false : true), checksum);
			} else {
				HpcLocalFolderProcessor folderProcess = new HpcLocalFolderProcessor(dataObject.getConnection());
				folderProcess.process(pathAttr, filePath, filePathBaseName, destinationBasePath, dataObject.getLogFile(),
						dataObject.getErrorRecordsFile(), metadataOnly, (archiveType != null && archiveType.equalsIgnoreCase("POSIX") ? false : true), checksum);
			}
		} catch (RecordProcessingException e) {
			throw e;
		} catch (IOException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			throw new RecordProcessingException(message, e);
		}
		return record;
	}
}
