package gov.nih.nci.hpc.web.model;

public class HpcPermissionEntry {
	private String name;
	private HpcPermissionEntryType type;
	private boolean read;
	private boolean write;
	private boolean own;

	public boolean isRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}
	public boolean isWrite() {
		return write;
	}
	public void setWrite(boolean write) {
		this.write = write;
	}
	public boolean isOwn() {
		return own;
	}
	public void setOwn(boolean own) {
		this.own = own;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public HpcPermissionEntryType getType() {
		return type;
	}
	public void setType(HpcPermissionEntryType type) {
		this.type = type;
	}
	
}
