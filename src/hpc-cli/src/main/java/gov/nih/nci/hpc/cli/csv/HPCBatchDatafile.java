/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.csv;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.HPCBatchClient;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;

@Component
public class HPCBatchDatafile extends HPCBatchClient {
	@Autowired
	private HpcConfigProperties configProperties;

	public HPCBatchDatafile() {
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

		if (userId == null || userId.trim().length() == 0 || password == null || password.trim().length() == 0) {
			System.out.println("Invalid login credentials");
			return false;
		}
		try {
			String authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
					hpcCertPassword);
			success = new HPCBatchDataFileProcessor(fileName, threadCount, hpcServerURL + "/" + hpcDataService,
					hpcCertPath, hpcCertPassword, null, null, logFile, logRecordsFile, authToken).processData();
		} catch (Exception e) {
			System.out.println("Cannot read the input file: "+e.getMessage());
			return false;
		}
		return success;

	}
}
