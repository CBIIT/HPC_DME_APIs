/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.HPCBatchClient;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;

@Component
public class HPCBatchLocalfile extends HPCBatchClient {
	@Autowired
	private HpcConfigProperties configProperties;

	Map<String, String> criteriaMap = null;
	String cmd = null;

	public HPCBatchLocalfile() {
		super();
	}

	protected void initializeLog() {
		logFile = logDir + File.separator + "putDatafiles_errorLog"
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
		logRecordsFile = logDir + File.separator + "putDatafiles_errorRecords"
				+ new SimpleDateFormat("yyyyMMddhhmm'.csv'").format(new Date());
	}

	public void setCriteria(String cmd, Map<String, String> criteriaMap) {
		this.criteriaMap = criteriaMap;
		this.cmd = cmd;
	}

	protected boolean processFile(String fileName, String userId, String password) {
		boolean success = true;

		if (userId == null || userId.trim().length() == 0 || password == null || password.trim().length() == 0) {
			System.out.println("Invalid login credentials");
			return false;
		}
		try {
			if (criteriaMap == null || criteriaMap.isEmpty())
				return false;

			String authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL,
					hpcServerProxyPort, hpcCertPath, hpcCertPassword);
			HpcServerConnection connection = new HpcServerConnection(hpcServerURL, hpcServerProxyURL,
					hpcServerProxyPort, authToken, hpcCertPath, hpcCertPassword);
			connection.setBufferSize(bufferSize);
			success = new HPCBatchLocalFolderExecutor(criteriaMap, connection, logFile, logRecordsFile, authToken)
					.processData();
		} catch (Exception e) {
			System.out.println("Cannot read the input file: " + e.getMessage());
			return false;
		}
		return success;

	}
}
