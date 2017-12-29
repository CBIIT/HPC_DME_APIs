/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.easybatch.core.processor.RecordProcessingException;
import org.springframework.web.client.RestClientException;

import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;

public class HpcLocalDirectoryListProcessor {

	private Properties properties = new Properties();
	private HpcServerConnection connection;
	private String logFile;
	private String recordFile;

	public HpcLocalDirectoryListProcessor(String configProps) throws IOException, FileNotFoundException {
		InputStream input = new FileInputStream(configProps);
		properties.load(input);
	}

	public HpcLocalDirectoryListProcessor(HpcServerConnection connection) throws IOException, FileNotFoundException {
		this.connection = connection;
	}

	public boolean run(List<HpcPathAttributes> files, String filePath, String filePathBaseName, String destinationBasePath,
			String logFile, String recordFile, boolean testRun, boolean confirmation, boolean metadataOnly, boolean checksum) {
		this.logFile = logFile;
		this.recordFile = recordFile;
		boolean success = true;
		try {
			if (files != null && !testRun) {
				Collections.sort(files);
				for (HpcPathAttributes file : files) {
					try {

						File fileAbsolutePath = new File(file.getAbsolutePath());
						if (!fileAbsolutePath.isDirectory()) {
							HpcLocalFileProcessor fileProcess = new HpcLocalFileProcessor(connection);
							fileProcess.process(file, filePath, filePathBaseName, destinationBasePath, logFile, recordFile,
									metadataOnly, false, checksum);
						} else {
							HpcLocalFolderProcessor folderProcess = new HpcLocalFolderProcessor(connection);
							folderProcess.process(file, filePath, filePathBaseName, destinationBasePath, logFile, recordFile,
									metadataOnly, false, checksum);
						}

					} catch (RecordProcessingException e) {
						String message = "Failed to process cmd due to: " + e.getMessage();
						HpcClientUtil.writeException(e, message, null, logFile);
						success = false;
					}
				}
			}
		} catch (HpcCmdException e) {
			String message = "Failed to process cmd due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			success = false;
		} catch (RestClientException e) {
			String message = "Failed to process cmd due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			success = false;
		} catch (Exception e) {
			String message = "Failed to process cmd due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			success = false;
		}
		return success;
	}

}
