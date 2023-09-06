package Register.Pojo;

import java.util.ArrayList;

public class BulkDataObjectRegister {
	
  boolean dryRun = false;

	ArrayList<DataObjectRegistration> dataObjectRegistrationItems;
    ArrayList<DirectoryScanRegistrationItemPojo> directoryScanRegistrationItems = new ArrayList<DirectoryScanRegistrationItemPojo>();

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public ArrayList<DirectoryScanRegistrationItemPojo> getDirectoryScanRegistrationItems() {
		return directoryScanRegistrationItems;
	}

	public void setDirectoryScanRegistrationItems(ArrayList<DirectoryScanRegistrationItemPojo> directoryScanRegistrations) {
		this.directoryScanRegistrationItems = directoryScanRegistrations;
	}

	public void setDataObjectRegistrationItems(ArrayList<DataObjectRegistration> dataObjectRegistrations) {
		this.dataObjectRegistrationItems = dataObjectRegistrations;
	}

	public ArrayList<DataObjectRegistration> getDataObjectRegistrationItems() {
		return this.dataObjectRegistrationItems;
	}
}
