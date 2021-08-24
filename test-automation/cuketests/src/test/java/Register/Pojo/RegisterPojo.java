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
    //Map<String, List<Map<String, String>>> parentCollectionsBulkMetadataEntries;
	
	ParentMetadataPojo parentCollectionsBulkMetadataEntries;
    
    public List<Map<String, String>> getDefaultCollectionMetadataEntries() {
		return defaultCollectionMetadataEntries;
	}
	public void setDefaultCollectionMetadataEntries(List<Map<String, String>> defaultCollectionMetadataEntries) {
		this.defaultCollectionMetadataEntries = defaultCollectionMetadataEntries;
	}
	public void setParentCollectionsBulkMetadataEntries(ParentMetadataPojo parentCollectionsBulkMetadataEntries) {
		this.parentCollectionsBulkMetadataEntries = parentCollectionsBulkMetadataEntries;
	}
	List<Map<String, String>> defaultCollectionMetadataEntries;
    
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
	public ParentMetadataPojo getParentCollectionsBulkMetadataEntries() {
		return parentCollectionsBulkMetadataEntries;
	}
	
    
	
	
}
