package gov.nih.nci.hpc.cli.domain;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;

public class HpcMetadataAttributes {
	List<HpcMetadataEntry> metadataEntries;

	public List<HpcMetadataEntry> getMetadataEntries() {
		if(metadataEntries == null)
			metadataEntries = new ArrayList<HpcMetadataEntry>();
		return metadataEntries;
	}

	public void setMetadataEntries(List<HpcMetadataEntry> metadataEntries) {
		this.metadataEntries = metadataEntries;
	}
	
}
