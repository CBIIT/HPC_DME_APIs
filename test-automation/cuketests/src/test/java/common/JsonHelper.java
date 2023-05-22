package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonHelper {
	
	public static String getPrettyJson(Object json) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJsonString = gson.toJson(json);
		System.out.println("Formatted JSON String");
		System.out.println(prettyJsonString);
		return prettyJsonString;
	}

	
	


}
