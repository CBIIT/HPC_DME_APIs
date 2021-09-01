package Download.pojos;

import java.util.List;

public class SyncType {

	String compressedArchiveType;
	
	List<String>  includePatterns;
	
	public String getCompressedArchiveType() {
		return compressedArchiveType;
	}

	public void setCompressedArchiveType(String compressedArchiveType) {
		this.compressedArchiveType = compressedArchiveType;
	}

	public List<String> getIncludePatterns() {
		return includePatterns;
	}

	public void setIncludePatterns(List<String> includePatterns) {
		this.includePatterns = includePatterns;
	}

	
}
