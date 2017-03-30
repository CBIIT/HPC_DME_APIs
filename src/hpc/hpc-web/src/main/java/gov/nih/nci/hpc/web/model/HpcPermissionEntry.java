package gov.nih.nci.hpc.web.model;

public class HpcPermissionEntry implements Comparable<HpcPermissionEntry> {
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

	public String getPermission() {
		if (isRead())
			return "READ";
		else if (isWrite())
			return "WRITE";
		else if (isOwn())
			return "OWN";
		else
			return "NONE";
	}

	@Override
	public int compareTo(HpcPermissionEntry arg0) {
		// TODO Auto-generated method stub
		return this.name.compareTo(arg0.getName());
	}
}
