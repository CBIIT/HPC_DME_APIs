package Download.pojos;

public class GlobusType {
	
	/**
	 * 
	 * 
	 * 
	"globusDownloadDestination": {
		"destinationLocation": {
			"fileContainerId": "4a3b132a-815f-11e7-8dff-22000b9923ef",
			"fileId": "test-12-02-18"
		},
                        "destinationOverwrite" : true,
	}
}


	 */
	
	
	DestinationLocationType destinationLocation;
	public DestinationLocationType getDestinationLocation() {
		return destinationLocation;
	}
	public void setDestinationLocation(DestinationLocationType destinationLocation) {
		this.destinationLocation = destinationLocation;
	}
	public Boolean getDestinationOverwrite() {
		return destinationOverwrite;
	}
	public void setDestinationOverwrite(Boolean destinationOverwrite) {
		this.destinationOverwrite = destinationOverwrite;
	}
	Boolean destinationOverwrite;

}
