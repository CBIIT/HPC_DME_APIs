package gov.nih.nci.hpc.transfer.impl;

import gov.nih.nci.hpc.transfer.HpcDataTransfer;

import java.security.GeneralSecurityException;
import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import gov.nih.nci.hpc.domain.HpcDataset;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.transfer.*;
import org.json.*;
import org.globusonline.nexus.exception.NexusClientException;

public class GlobusOnlineDataTranfer implements HpcDataTransfer{
    private JSONTransferAPIClient client;
    private Properties transferProperties = new Properties();
    private static DateFormat isoDateFormat =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public GlobusOnlineDataTranfer() 
    {
    	// init transfer client
    	
    }
    
    public boolean transferDataset(HpcDataset dataset,String username, String password )
    {
    	//Set transferAPIClient
    	try
    	{    	
    		setJSONTransferClient(username,password);
    		return transfer(dataset);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
    	return false;
    }
    
    private void setJSONTransferClient(String username, String password) throws IOException, JSONException, GeneralSecurityException, APIError,NexusClientException
    {
    	//Replace this with nexus call to get oauthtoken
    	//loadProperties("transfer.config");
    	System.out.println("USERNAME : " + username);
        //String username = getPropValue("globus.username");
       // String username = "mahinarra";
        GoauthClient cli = new GoauthClient("nexus.api.globusonline.org", "www.globusonline.org", username, password);
		JSONObject accessTokenJSON = cli.getClientOnlyAccessToken();
		String accessToken = accessTokenJSON.getString("access_token");
		System.out.println("Client only access token: " + accessToken);
		cli.validateAccessToken(accessToken);        

        //String oauthToken = getPropValue("globus.oauthToken");
        //String oauthToken = "un=mahinarra|tokenid=6c238272-f996-11e4-9586-22000aeb2621|expiry=1463074518|client_id=mahinarra|token_type=Bearer|SigningSubject=https://nexus.api.globusonline.org/goauth/keys/a741437a-f5aa-11e4-b66e-22000aeb2621|sig=8a1bd3fc0640a8ee2dc90d080c32604bceb4ef14b67abd7cfa22f395f744856bc711bde2774e738563dfea4beb8bdc85aca52c80a0e1a1cbe89e4cbb3b2ba5db5782f3fcf66706ab9ed7be31e6173a26c58efaa7f194c8651d77f8c8c32a0856de52326c4120e1c7cc3d2e84bb30e2a3e22a6b3d9423c4fc5e9246b4715d49cf";
        Authenticator authenticator = new GoauthAuthenticator(accessToken);
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
