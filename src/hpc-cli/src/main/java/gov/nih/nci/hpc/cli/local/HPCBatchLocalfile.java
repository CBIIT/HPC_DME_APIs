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
import gov.nih.nci.hpc.cli.util.Constants;
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
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
	}

	public void setCriteria(String cmd, Map<String, String> criteriaMap) {
		this.criteriaMap = criteriaMap;
		this.cmd = cmd;
	}

	protected String processFile(String fileName, String userId, String password, String authToken) {
		if (authToken == null && (userId == null || userId.trim().length() == 0 || password == null || password.trim().length() == 0)) {
			System.out.println("Invalid login credentials");
			return Constants.CLI_1;
		}
		if (criteriaMap == null || criteriaMap.isEmpty())
			return Constants.CLI_2;

		if(authToken == null)
			authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL,
				hpcServerProxyPort, hpcCertPath, hpcCertPassword);
		HpcServerConnection connection = new HpcServerConnection(hpcServerURL, hpcServerProxyURL,
				hpcServerProxyPort, authToken, hpcCertPath, hpcCertPassword);
		connection.setBufferSize(bufferSize);
		return new HPCBatchLocalFolderExecutor(criteriaMap, connection, logFile, logRecordsFile, authToken, maxAttempts, backOffPeriod)
				.processData();
	}
}
