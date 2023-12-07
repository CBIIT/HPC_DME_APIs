package BulkMetadataUpdate.Pojo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import  common.MetadataEntry;

/*
  This is the equivalent of HpcBulkMetadataEntries in domain-types/metadata
*/
public class BulkMetadataUpdatePojo {
    List<String> collectionPaths;
    List<MetadataEntry> metadataEntries;
    
	public List<String> getCollectionPaths() {
		return collectionPaths;
	}
	public void setCollectionPaths(List<String> collectionPaths) {
		this.collectionPaths = collectionPaths;
	}
	public List<MetadataEntry> getMetadataEntries() {
		return metadataEntries;
	}
	public void setMetadataEntries(List<MetadataEntry> metadataEntries) {
		this.metadataEntries = metadataEntries;
	}


}
