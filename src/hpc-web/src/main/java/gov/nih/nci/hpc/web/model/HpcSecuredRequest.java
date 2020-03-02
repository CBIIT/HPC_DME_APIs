package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcSecuredRequest {

	@JsonView(Views.Public.class)
	private String userKey;
	@JsonView(Views.Public.class)
	private String textString;
	
	
    public String getUserKey() {
      return userKey;
    }
  
    public void setUserKey(String userKey) {
      this.userKey = userKey;
    }
  
    public String getTextString() {
      return textString;
    }
  
    public void setTextString(String textString) {
      this.textString = textString;
    }

}
