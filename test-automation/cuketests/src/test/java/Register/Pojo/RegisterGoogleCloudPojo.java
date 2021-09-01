package Register.Pojo;

import java.util.List;
import java.util.Map;

public class RegisterGoogleCloudPojo {
	
	SourceLocationPojo sourceLocation;
	String accessToken;
	List<Map<String, String>> metadataEntries;
	Boolean createParentCollections;
	
	ParentMetadataPojo parentCollectionsBulkMetadataEntries;

}
