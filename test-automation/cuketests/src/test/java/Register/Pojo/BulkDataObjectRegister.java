package Register.Pojo;

import java.util.ArrayList;

public class BulkDataObjectRegister {
	
  boolean dryRun = false;

	ArrayList<DataObjectRegistration> dataObjectRegistrationItems;	

  public void setDataObjectRegistrationItems(ArrayList<DataObjectRegistration> dataObjectRegistrations) {
    this.dataObjectRegistrationItems = dataObjectRegistrations;
  }

  public ArrayList<DataObjectRegistration> getDataObjectRegistrationItems() {
      return this.dataObjectRegistrationItems;
  }

}
