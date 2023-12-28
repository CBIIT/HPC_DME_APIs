package Register.Pojo;

import java.util.List;
import java.util.Map;

public class DirectoryScanRegistrationItemPojo {
  
    String basePath;
    DirectoryLocationUploadPojo globusScanDirectory;
    DirectoryLocationUploadPojo s3ScanDirectory;
    DirectoryLocationUploadPojo googleDriveScanDirectory;
    DirectoryLocationUploadPojo googleCloudStorageScanDirectory;
    DirectoryLocationUploadPojo fileSystemScanDirectory;
    BulkMetadataEntriesPojo bulkMetadataEntries;
    String includePatterns;
    String excludePatterns;
    String callerObjectId;
    DirectoryScanPathMapPojo pathMap;
    String patternType;
  
	public String getBasePath() {
      return basePath;
    }
    public void setBasePath(String basePath) {
      this.basePath = basePath;
    }
    public String getIncludePatterns() {
      return includePatterns;
    }
    public void setIncludePatterns(String includePatterns) {
      this.includePatterns = includePatterns;
    }
    public String getExcludePatterns() {
      return excludePatterns;
    }
    public void setExcludePatterns(String excludePatterns) {
      this.excludePatterns = excludePatterns;
    }
    public DirectoryScanPathMapPojo getPathMap() {
      return pathMap;
    }
    public void setPathMap(DirectoryScanPathMapPojo pathMap) {
      this.pathMap = pathMap;
    }
    public String getCallerObjectId() {
      return callerObjectId;
    }
    public void setCallerObjectId(String callerObjectId) {
      this.callerObjectId = callerObjectId;
    }
	public DirectoryLocationUploadPojo getGlobusScanDirectory() {
		return globusScanDirectory;
	}
	public void setGlobusScanDirectory(DirectoryLocationUploadPojo globusScanDirectory) {
		this.globusScanDirectory = globusScanDirectory;
	}
	public DirectoryLocationUploadPojo getS3ScanDirectory() {
		return s3ScanDirectory;
	}
	public void setS3ScanDirectory(DirectoryLocationUploadPojo s3ScanDirectory) {
		this.s3ScanDirectory = s3ScanDirectory;
	}
	public DirectoryLocationUploadPojo getGoogleDriveScanDirectory() {
		return googleDriveScanDirectory;
	}
	public void setGoogleDriveScanDirectory(DirectoryLocationUploadPojo googleDriveScanDirectory) {
		this.googleDriveScanDirectory = googleDriveScanDirectory;
	}
	public DirectoryLocationUploadPojo getGoogleCloudStorageScanDirectory() {
		return googleCloudStorageScanDirectory;
	}
	public void setGoogleCloudStorageScanDirectory(DirectoryLocationUploadPojo googleCloudStorageScanDirectory) {
		this.googleCloudStorageScanDirectory = googleCloudStorageScanDirectory;
	}
	public DirectoryLocationUploadPojo getFileSystemScanDirectory() {
		return fileSystemScanDirectory;
	}
	public void setFileSystemScanDirectory(DirectoryLocationUploadPojo fileSystemScanDirectory) {
		this.fileSystemScanDirectory = fileSystemScanDirectory;
	}
	public BulkMetadataEntriesPojo getBulkMetadataEntries() {
		return bulkMetadataEntries;
	}
	public void setBulkMetadataEntries(BulkMetadataEntriesPojo bulkMetadataEntries) {
		this.bulkMetadataEntries = bulkMetadataEntries;
	}
	public String getPatternType() {
		return patternType;
	}
	public void setPatternType(String patternType) {
		this.patternType = patternType;
	}

  

}
