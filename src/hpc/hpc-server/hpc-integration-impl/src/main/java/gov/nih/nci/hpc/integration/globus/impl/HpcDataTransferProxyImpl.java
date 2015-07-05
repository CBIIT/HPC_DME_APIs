package gov.nih.nci.hpc.integration.globus.impl;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProxy;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.globus.driver.HpcGOTransfer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.nexus.exception.InvalidCredentialsException;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.json.JSONException;
import org.json.JSONObject;

public class HpcDataTransferProxyImpl 
       implements HpcDataTransferProxy, HpcDataTransferAccountValidatorProxy {
	
    private HpcGOTransfer hpcGOTransfer = null;

    public HpcDataTransferProxyImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }

    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param HpcGOTransfer instance.
     * 
     * @throws HpcException If a HpcGOTransfer instance was not provided.
     */
    @SuppressWarnings("unused")
	private HpcDataTransferProxyImpl(HpcGOTransfer hpcGOTransfer) throws HpcException
    {
    	if(hpcGOTransfer == null) {
    	   throw new HpcException("Null HpcGOTransfer instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.hpcGOTransfer = hpcGOTransfer;
    }
    
    @Override
    public HpcDataTransferReport transferDataset(HpcDataTransferLocations transferLocations,String username, String password )
    {
    	try
    	{    
    		hpcGOTransfer = new HpcGOTransfer(username, password);
    		return transfer(transferLocations);
	    } catch (Exception e) {
	        e.printStackTrace();
	        
	    	//Return transfer exception
	    	return null;
	    }
    }
    
    @Override
    public boolean validateDataTransferAccount(
                               HpcDataTransferAccount dataTransferAccount) 
                        	   throws HpcException
    {
    	try
    	{    	
            GoauthClient cli = new GoauthClient("nexus.api.globusonline.org", "www.globusonline.org", 
            		                            dataTransferAccount.getUsername(), 
            		                            dataTransferAccount.getPassword());
    		JSONObject accessTokenJSON = cli.getClientOnlyAccessToken();
    		String accessToken = accessTokenJSON.getString("access_token");
    		System.out.println("Client only access token: " + accessToken);
    		cli.validateAccessToken(accessToken);
    		
    	} catch(InvalidCredentialsException ice) {
    		    return false;
    		    
	    } catch(Exception e) {
	    	    throw new HpcException("Failed to invoke GLOBUS account validation",
	    	    		               HpcErrorType.DATA_TRANSFER_ERROR);
	        	
	    }
    	
    	return true;
    }
 
    @Override
    public String getTransferStatus(String submissionId)
    {
    	JSONTransferAPIClient client = hpcGOTransfer.getTransferClient();
    	String status = "ACTIVE";
    	
    	try
    	{    	            
            JSONTransferAPIClient.Result r;

            String resource = "/task/" +  submissionId;
            Map<String, String> params = new HashMap<String, String>();
            params.put("fields", "status");

            r = client.getResult(resource, params);
            status = r.document.getString("status");

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
    	return status;
    }
    
      
    private HpcDataTransferReport transfer(HpcDataTransferLocations transferLocations)
    throws IOException, JSONException, GeneralSecurityException, APIError {
    	JSONTransferAPIClient client = hpcGOTransfer.getTransferClient();
    	HpcDataTransferReport hpcDataTransferReport = new HpcDataTransferReport();

        if (!autoActivate(transferLocations.getSource().getEndpoint(), client) || 
        		!autoActivate(transferLocations.getDestination().getEndpoint(), client)
        		) {
            System.err.println("Unable to auto activate go tutorial endpoints, "
                               + " exiting");
            // throw ENDPOINT NOT ACTIVATED HPCException
            //return false;
        }
        JSONTransferAPIClient.Result r;
        r = client.getResult("/transfer/submission_id");
        String submissionId = r.document.getString("value");
        JSONObject transfer = new JSONObject();
        transfer.put("DATA_TYPE", "transfer");
        transfer.put("submission_id", submissionId);
        JSONObject item = setJSONItem(transferLocations, client);
        transfer.append("DATA", item);

        r = client.postResult("/transfer", transfer, null);
        String taskId = r.document.getString("task_id");
        System.out.println("Transfer task id :"+taskId );
        
        hpcDataTransferReport.setTaskID(taskId);

        return hpcDataTransferReport;
    }

    
    private JSONObject setJSONItem(HpcDataTransferLocations transferLocations,JSONTransferAPIClient client)  throws IOException, JSONException, GeneralSecurityException {
    	JSONObject item = new JSONObject();
        item.put("DATA_TYPE", "transfer_item");
        item.put("source_endpoint", transferLocations.getSource().getEndpoint());
        item.put("source_path", transferLocations.getSource().getPath());
        item.put("destination_endpoint", transferLocations.getDestination().getEndpoint());
        item.put("destination_path", transferLocations.getDestination().getPath());
        item.put("recursive", checkFileDirectoryAndSetRecursive(transferLocations.getSource().getEndpoint(),transferLocations.getSource().getPath(),client));
        return item;
    }

    public boolean autoActivate(String endpointName, JSONTransferAPIClient client)
    throws IOException, JSONException, GeneralSecurityException, APIError {
        String resource = BaseTransferAPIClient.endpointPath(endpointName)
                          + "/autoactivate?if_expires_in=100";
        JSONTransferAPIClient.Result r = client.postResult(resource, null,
                                                           null);
        String code = r.document.getString("code");
        if (code.startsWith("AutoActivationFailed")) {
            return false;
        }
        return true;
    }

    	
    public boolean checkFileDirectoryAndSetRecursive(String endpointName, String path,JSONTransferAPIClient client)
    throws IOException, JSONException, GeneralSecurityException {
        Map<String, String> params = new HashMap<String, String>();
        if (path != null) {
            params.put("path", path);
        }
        String resource = BaseTransferAPIClient.endpointPath(endpointName)
                          + "/ls";
        try
        {
        	client.getResult(resource, params);
        }catch(APIError error)
        {
        	if("ExternalError.DirListingFailed.NotDirectory".equals(error.code))
        		return false;
        }
                
        return true;

    }	
}
