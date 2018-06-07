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
import org.springframework.web.util.UriComponentsBuilder;

import gov.nih.nci.hpc.cli.HPCBatchClient;
import gov.nih.nci.hpc.cli.util.Constants;
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

	protected String processFile(String fileName, String userId, String password, String authToken) {
		if (authToken == null && (userId == null || userId.trim().length() == 0 || password == null || password.trim().length() == 0)) {
			System.out.println("Invalid login credentials");
			return Constants.CLI_1;
		}
		try {
			if(authToken == null)
				authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
					hpcCertPassword);
      final String apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(
        hpcServerURL).path(HpcClientUtil.prependForwardSlashIfAbsent(
				hpcDataService)).build().encode().toUri().toURL().toExternalForm();
      return new HPCBatchDataFileProcessor(fileName, threadCount, apiUrl2Apply,
        hpcCertPath, hpcCertPassword, null, null, logFile, logRecordsFile,
        authToken).processData();
		} catch (Exception e) {
			System.out.println("Cannot read the input file: "+e.getMessage());
			return Constants.CLI_2;
		}
	}
}
