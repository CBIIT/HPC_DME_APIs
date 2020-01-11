/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.domain;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.csv.CSVRecord;

import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;

@XmlRootElement
public class HPCDataObject {
	private String objectPath;
	private HpcDataObjectRegistrationRequestDTO dto;
	private String basePath;
	private HpcPathAttributes dataFilePathAttrs;
	private String proxyURL;
	private String proxyPort;
	private String hpcCertPath;
	private String hpcCertPassword;
	private String userId;
	private String password;
	private String authToken;
	private String logFile;
	private int bufferSize;
	private String errorRecordsFile;
	private Map<String, Integer> headersMap;
	private Map<String, String> criteriaMap;
	private CSVRecord csvRecord;
	private HpcServerConnection connection;
	private int maxAttempts;
    private long backOffPeriod;

	public HpcServerConnection getConnection() {
		return connection;
	}

	public void setConnection(HpcServerConnection connection) {
		this.connection = connection;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public CSVRecord getCsvRecord() {
		return csvRecord;
	}

	public void setCsvRecord(CSVRecord csvRecord) {
		this.csvRecord = csvRecord;
	}

	public Map<String, Integer> getHeadersMap() {
		return headersMap;
	}

	public void setHeadersMap(Map<String, Integer> headersMap) {
		this.headersMap = headersMap;
	}

	public HPCDataObject() {
	}

	public String getObjectPath() {
		return objectPath;
	}

	public void setObjectPath(String objectPath) {
		this.objectPath = objectPath;
	}

	public HpcDataObjectRegistrationRequestDTO getDto() {
		return dto;
	}

	public void setDto(HpcDataObjectRegistrationRequestDTO dto) {
		this.dto = dto;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getHpcCertPath() {
		return hpcCertPath;
	}

	public void setHpcCertPath(String hpcCertPath) {
		this.hpcCertPath = hpcCertPath;
	}

	public String getHpcCertPassword() {
		return hpcCertPassword;
	}

	public void setHpcCertPassword(String hpcCertPassword) {
		this.hpcCertPassword = hpcCertPassword;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public String getErrorRecordsFile() {
		return errorRecordsFile;
	}

	public void setErrorRecordsFile(String errorRecordsFile) {
		this.errorRecordsFile = errorRecordsFile;
	}

	public String getProxyURL() {
		return proxyURL;
	}

	public void setProxyURL(String proxyURL) {
		this.proxyURL = proxyURL;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public HpcPathAttributes getDataFilePathAttrs() {
		return dataFilePathAttrs;
	}

	public void setDataFilePathAttrs(HpcPathAttributes dataFilePathAttrs) {
		this.dataFilePathAttrs = dataFilePathAttrs;
	}

	public Map<String, String> getCriteriaMap() {
		return criteriaMap;
	}

	public void setCriteriaMap(Map<String, String> criteriaMap) {
		this.criteriaMap = criteriaMap;
	}

    public int getMaxAttempts() {
      return maxAttempts;
    }
  
    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }
  
    public long getBackOffPeriod() {
      return backOffPeriod;
    }
  
    public void setBackOffPeriod(long backOffPeriod) {
      this.backOffPeriod = backOffPeriod;
    }

  @Override
  public String toString() {
    return "HPCDataObject [objectPath=" + objectPath + ", dto=" + dto + ", basePath=" + basePath
        + ", dataFilePathAttrs=" + dataFilePathAttrs + ", proxyURL=" + proxyURL + ", proxyPort="
        + proxyPort + ", hpcCertPath=" + hpcCertPath + ", hpcCertPassword=" + hpcCertPassword
        + ", userId=" + userId + ", password=" + password + ", authToken=" + authToken
        + ", logFile=" + logFile + ", bufferSize=" + bufferSize + ", errorRecordsFile="
        + errorRecordsFile + ", headersMap=" + headersMap + ", criteriaMap=" + criteriaMap
        + ", csvRecord=" + csvRecord + ", connection=" + connection + "]";
  }
	
	
}
