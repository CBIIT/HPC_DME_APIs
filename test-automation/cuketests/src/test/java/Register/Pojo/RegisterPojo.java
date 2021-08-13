package Register.Pojo;

import java.util.List;
import java.util.Map;

public class RegisterPojo {

	/**
	 * 
	 * 

"callerObjectId" : "<user-defined-base-path-in-archive (optional) >",
"checksum" : "<data-checksum (optional) >",
"extractMetadata" : true|false (optional) ,
"metadataEntries": [
      	{
      	 "attribute": "name",
      	 "value": "Set100"
   	}
	],
  “createParentCollections” : <true|false> (optional, false by default)>,
  “parentCollectionsBulkMetadataEntries” :  {
     “defaultCollectionMetadataEntries” : [
              {
      	    "attribute": "collection_type",
      	    "value": "Folder"
   	  }
	  ]
   }
}

	 * 
	 */
	
	String callerObjectId;
	String checksum;
	Boolean extractMetadata;
	List<Map<String, String>> metadataEntries;
	Boolean createParentCollections;
    Map<String, List<Map<String, String>>> parentCollectionsBulkMetadataEntries;
    
	public String getCallerObjectId() {
		return callerObjectId;
	}
	public void setCallerObjectId(String callerObjectId) {
		this.callerObjectId = callerObjectId;
	}
	public String getChecksum() {
		return checksum;
	}
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}
	public Boolean getExtractMetadata() {
		return extractMetadata;
	}
	public void setExtractMetadata(Boolean extractMetadata) {
		this.extractMetadata = extractMetadata;
	}
	public List<Map<String, String>> getMetadataEntries() {
		return metadataEntries;
	}
	public void setMetadataEntries(List<Map<String, String>> metadataEntries) {
		this.metadataEntries = metadataEntries;
	}
	public Boolean getCreateParentCollections() {
		return createParentCollections;
	}
	public void setCreateParentCollections(Boolean createParentCollections) {
		this.createParentCollections = createParentCollections;
	}
	public Map<String, List<Map<String, String>>> getParentCollectionsBulkMetadataEntries() {
		return parentCollectionsBulkMetadataEntries;
	}
	public void setParentCollectionsBulkMetadataEntries(
			Map<String, List<Map<String, String>>> parentCollectionsBulkMetadataEntries) {
		this.parentCollectionsBulkMetadataEntries = parentCollectionsBulkMetadataEntries;
	}
    
	
	
}
