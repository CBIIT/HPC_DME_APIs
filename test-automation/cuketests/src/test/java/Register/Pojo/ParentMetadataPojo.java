package Register.Pojo;

import java.util.List;
import java.util.Map;

public class ParentMetadataPojo {
	
	List<Map<String, String>> defaultCollectionMetadataEntries;

	public List<Map<String, String>> getDefaultCollectionMetadataEntries() {
		return defaultCollectionMetadataEntries;
	}

	public void setDefaultCollectionMetadataEntries(List<Map<String, String>> defaultCollectionMetadataEntries) {
		this.defaultCollectionMetadataEntries = defaultCollectionMetadataEntries;
	}
	
	//Map<String, List<Map<String, String>>> pathsMetadataEntries;

}
