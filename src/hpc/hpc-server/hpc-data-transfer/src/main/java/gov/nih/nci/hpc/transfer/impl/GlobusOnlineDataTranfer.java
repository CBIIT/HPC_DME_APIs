package gov.nih.nci.hpc.transfer.impl;

import gov.nih.nci.hpc.transfer.HpcDataTransfer;
import java.security.GeneralSecurityException;
import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import gov.nih.nci.hpc.domain.HpcDataset;
import org.globusonline.transfer.*;

import org.json.*;

public class GlobusOnlineDataTranfer implements HpcDataTransfer{
    private JSONTransferAPIClient client;
    private Properties transferProperties = new Properties();
    private static DateFormat isoDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public GlobusOnlineDataTranfer() 
    {
    	// init transfer client
    	
    }
    
    public boolean transferDataset(HpcDataset dataset)
    {
    	//Set transferAPIClient
    	try
    	{    	
    		setJSONTransferClient();
    		return transfer(dataset);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
    	return false;
    }
    
    private void setJSONTransferClient() throws IOException, JSONException, GeneralSecurityException, APIError
    {
    	//Replace this with nexus call to get oauthtoken
    	loadProperties("transfer.config");
    	System.out.println("OUTH TOKEN url: " + getPropValue("globus.oauthToken"));
    	System.out.println("USERNAME: " + getPropValue("globus.username"));
        String username = getPropValue("globus.username");

        String oauthToken = getPropValue("globus.oauthToken");        	
        Authenticator authenticator = new GoauthAuthenticator(oauthToken);
        client = new JSONTransferAPIClient(username, null, null);
        client.setAuthenticator(authenticator);    	
    }    
    
    private boolean transfer(HpcDataset dataset)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        JSONTransferAPIClient.Result r;
        System.out.println("=== Before Transfer ===");

        if (!autoActivate(dataset.getSource().getEndpoint())) {
            System.err.println("Unable to auto activate go tutorial endpoints, "
                               + " exiting");
            return false;
        }

        r = client.getResult("/transfer/submission_id");
        String submissionId = r.document.getString("value");
        JSONObject transfer = new JSONObject();
        transfer.put("DATA_TYPE", "transfer");
        transfer.put("submission_id", submissionId);
        JSONObject item = setJSONItem(dataset);
        transfer.append("DATA", item);

        r = client.postResult("/transfer", transfer, null);
        String taskId = r.document.getString("task_id");
        if (!waitForTask(taskId, 120)) {
            System.out.println(
                "Transfer not complete after 2 minutes, exiting");
            return false;
        }

        System.out.println("=== After Transfer ===");
        return true;
    }

    
    private JSONObject setJSONItem(HpcDataset dataset)  throws JSONException {
    	JSONObject item = new JSONObject();
        item.put("DATA_TYPE", "transfer_item");
        item.put("source_endpoint", dataset.getSource().getEndpoint());
        item.put("source_path", dataset.getSource().getFilePath());
        item.put("destination_endpoint", dataset.getLocation().getEndpoint());
        item.put("destination_path", dataset.getLocation().getFilePath());
        return item;
    }

    public boolean autoActivate(String endpointName)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String resource = BaseTransferAPIClient.endpointPath(endpointName)
                          + "/autoactivate";
        JSONTransferAPIClient.Result r = client.postResult(resource, null,
                                                           null);
        String code = r.document.getString("code");
        if (code.startsWith("AutoActivationFailed")) {
            return false;
        }
        return true;
    }

    public boolean waitForTask(String taskId, int timeout)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String status = "ACTIVE";
        JSONTransferAPIClient.Result r;

        String resource = "/task/" +  taskId;
        Map<String, String> params = new HashMap<String, String>();
        params.put("fields", "status");

        while (timeout > 0 && status.equals("ACTIVE")) {
            r = client.getResult(resource, params);
            status = r.document.getString("status");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                return false;
            }
            timeout -= 10;
        }

        if (status.equals("ACTIVE"))
            return false;
        return true;
    }

    
	private void loadProperties(String configFileName) throws IOException 
	{		 
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFileName);
 
		if (inputStream != null)
			transferProperties.load(inputStream);
		else 
			throw new FileNotFoundException("property file '" + configFileName + "' not found in the classpath");

	}
	
	private String getPropValue(String propKey) throws IOException 
	{		  
		return transferProperties.getProperty(propKey);
	}    
}
