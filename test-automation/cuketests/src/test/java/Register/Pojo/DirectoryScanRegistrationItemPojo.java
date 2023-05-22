package Register.Pojo;

import java.util.List;
import java.util.Map;

public class DirectoryScanRegistrationItemPojo {
  
    String basePath;
    SourceLocationPojo scanDirectoryLocation;
    List<Map<String, String>> dataObjectMetadataEntries;    
    String includePatterns;
    String excludePatterns;
    String callerObjectId;
    DirectoryScanPathMapPojo pathMap;

    public String getBasePath() {
      return basePath;
    }
    public void setBasePath(String basePath) {
      this.basePath = basePath;
    }
    public SourceLocationPojo getScanDirectoryLocation() {
      return scanDirectoryLocation;
    }
    public void setScanDirectoryLocation(SourceLocationPojo scanDirectoryLocation) {
      this.scanDirectoryLocation = scanDirectoryLocation;
    }
    public List<Map<String, String>> getDataObjectMetadataEntries() {
      return dataObjectMetadataEntries;
    }
    public void setDataObjectMetadataEntries(List<Map<String, String>> dataObjectMetadataEntries) {
      this.dataObjectMetadataEntries = dataObjectMetadataEntries;
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

  /*
   * 
    <xsd:complexType
        name="HpcDirectoryScanRegistrationItemDTO">
        <xsd:sequence>
            <xsd:element name="basePath" type="xsd:string" />
            <xsd:element name="scanDirectoryLocation"
                type="hpc-domain-datatransfer:HpcFileLocation" />
            <xsd:element name="callerObjectId" type="xsd:string" />
            <xsd:element name="includePatterns" type="xsd:string"
                minOccurs="0" maxOccurs="unbounded" />
            <xsd:element name="excludePatterns" type="xsd:string"
                minOccurs="0" maxOccurs="unbounded" />
            <xsd:element name="patternType"
                type="hpc-domain-datatransfer:HpcPatternType" />
            <xsd:element name="bulkMetadataEntries"
                type="hpc-domain-metadata:HpcBulkMetadataEntries" />
            <xsd:element name="pathMap"
                type="hpc-domain-datamanagement:HpcDirectoryScanPathMap" />
        </xsd:sequence>
    </xsd:complexType>

   * 
   */

}
