package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.domain.metadata.HpcProjectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ui.Model;

public class Util {
	public static Map<String, String> getPIs() {
		Map<String, String> users = new HashMap<String, String>();
		users.put("konkapv", "Prasad Konka");
		users.put("narram", "Mahidhar Narra");
		users.put("rosenbergea", "Eran Rosenberg");
		users.put("luz6", "Zhengwu Lu");
		users.put("stahlbergea", "Eric A Stahlberg");
		users.put("sdavis2", "Sean R Davis");
		users.put("maggiec", "Margaret C Cam");
		users.put("fitzgepe", "Peter C Fitzgerald");
		users.put("zhaoyong", "Yongmei Zhao");
		users.put("addepald", "Durga Addepalli");
		return users;
	}

	public static Map<String, String> getUserDefinedPrimaryMedataAttrs() {
		Map<String, String> attrs = new HashMap<String, String>();
		attrs.put("Attr1", "Attr1");
		attrs.put("Attr2", "Attr2");
		attrs.put("Attr3", "Attr3");
		attrs.put("Attr4", "Attr4");
		attrs.put("Attr5", "Attr5");
		attrs.put("Attr6", "Attr6");
		attrs.put("Attr7", "Attr7");
		attrs.put("Attr8", "Attr8");
		attrs.put("Attr9", "Attr9");
		attrs.put("Attr10", "Attr10");
		return attrs;
	}

	public static Map<String, String> getProjectTypes() {
		Map<String, String> attrs = new HashMap<String, String>();
		attrs.put("UMBRELLA", "UMBRELLA");
		attrs.put("ANALYSIS", "ANALYSIS");
		attrs.put("SEQUENCING", "SEQUENCING");
		attrs.put("UNKNOWN", "UNKNOWN");
		return attrs;
	}
	
}
