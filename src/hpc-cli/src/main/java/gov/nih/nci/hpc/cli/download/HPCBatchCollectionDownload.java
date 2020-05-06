/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.download;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;
import gov.nih.nci.hpc.cli.HPCBatchClient;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;

@Component
public class HPCBatchCollectionDownload extends HPCBatchClient {

	Map<String, String> criteriaMap = null;

	public HPCBatchCollectionDownload() {
		super();
	}

	public void setCriteria(Map<String, String> criteriaMap) {
		this.criteriaMap = criteriaMap;
	}

	protected void initializeLog() {
		logFile = logDir + File.separator + "downloadCollection_errorLog"
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
		logRecordsFile = logDir + File.separator + "downloadCollection_errorRecords"
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
	}

	protected String processFile(String fileName, String userId, String password, String authToken) {
		if (authToken == null && (userId == null || userId.trim().length() == 0 || password == null
				|| password.trim().length() == 0)) {
			System.out.println("Invalid login credentials");
			return Constants.CLI_1;
		}
		if (criteriaMap == null || criteriaMap.isEmpty())
			return Constants.CLI_2;
		try {
			if (authToken == null)
				authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL,
						hpcServerProxyPort, hpcCertPath, hpcCertPassword);
			HpcServerConnection connection = new HpcServerConnection(hpcServerURL, hpcServerProxyURL,
					hpcServerProxyPort, authToken, hpcCertPath, hpcCertPassword);
			return new HPCBatchCollectionDownloadProcessor(criteriaMap, connection, logFile, logRecordsFile)
					.processData();
		} catch (Exception e) {
			System.out.println("Failed to download collection to local directory: " + e.getMessage());
			return Constants.CLI_4;
		}
	}
}
