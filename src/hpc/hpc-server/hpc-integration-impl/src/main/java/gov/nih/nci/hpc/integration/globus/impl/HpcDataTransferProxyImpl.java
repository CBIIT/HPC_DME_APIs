package gov.nih.nci.hpc.integration.globus.impl;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProxy;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.globus.driver.HpcGOTransfer;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.nexus.exception.InvalidCredentialsException;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HpcDataTransferProxyImpl 
       implements HpcDataTransferProxy, HpcDataTransferAccountValidatorProxy 
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Globus Online transfer instance.
    private HpcGOTransfer hpcGOTransfer = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
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
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataTransferProxy Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataTransferReport transferDataset(
    		                             HpcDataTransferLocations transferLocations,
    		                             HpcUser user)
    		                             throws HpcException
    {
    	try {
    	     hpcGOTransfer.setTransferCient(user.getDataTransferAccount().getUsername(), 
    		       	                        user.getDataTransferAccount().getPassword());
    	     
    	} catch(Exception e) {
    		    throw new HpcException("Failed to create Globus transfer client",
		                               HpcErrorType.DATA_TRANSFER_ERROR, e);
    	}
    	
    	return transfer(transferLocations, user.getNihAccount().getUserId());
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
    		logger.debug("Client only access token: " + accessToken);
    		cli.validateAccessToken(accessToken);
    		
    	} catch(InvalidCredentialsException ice) {
    		    return false;
    		    
	    } catch(Exception e) {
	    	    throw new HpcException("Failed to invoke GLOBUS account validation",
	    	    		               HpcErrorType.DATA_TRANSFER_ERROR);
	        	
	    }
    	
    	return true;
    }
      
    private HpcDataTransferReport transfer(HpcDataTransferLocations transferLocations,
    		                               String nihUsername)
                                          throws HpcException 
    {
    	JSONTransferAPIClient client = hpcGOTransfer.getTransferClient();
    	
        if (!autoActivate(transferLocations.getSource().getEndpoint(), client) || 
        	!autoActivate(transferLocations.getDestination().getEndpoint(), client)
        		) {
            logger.error("Unable to auto activate go tutorial endpoints, exiting");
            // throw ENDPOINT NOT ACTIVATED HPCException
            //return false;
        }
        
        try {
	        JSONTransferAPIClient.Result r;
	        r = client.getResult("/transfer/submission_id");
	        String submissionId = r.document.getString("value");
	        JSONObject transfer = new JSONObject();
	        transfer.put("DATA_TYPE", "transfer");
	        transfer.put("submission_id", submissionId);
	        JSONObject item = setJSONItem(transferLocations, client, nihUsername);
	        transfer.append("DATA", item);

	        r = client.postResult("/transfer", transfer, null);
	        String taskId = r.document.getString("task_id");
	        logger.debug("Transfer task id :"+ taskId );
	      
	
	        return getTaskStatusReport(taskId);
	        
        } catch(Exception e) {
	        	throw new HpcException(
       		                 "Failed to transfer: " + transferLocations, 
       		                 HpcErrorType.DATA_TRANSFER_ERROR, e);
        }
    }

    
    private boolean createDestinationDirectory(String destinationEndpoint, String destinationPath, String destinationFileName)
           throws HpcException 
    {
    	JSONTransferAPIClient client = hpcGOTransfer.getTransferClient();
		try 
		{
			logger.info("Creating a directory at endpoint :" + destinationEndpoint);
			JSONTransferAPIClient.Result r;
			JSONObject mkDirectory = new JSONObject();			
			mkDirectory.put("path", destinationPath+"/"+destinationFileName);
			mkDirectory.put("DATA_TYPE", "mkdir");
			logger.info("Submitting create directory request :" + mkDirectory.toString());
			r = client.postResult(client.endpointPath(destinationEndpoint) + "/mkdir", mkDirectory);
            String code = r.document.getString("code");
            if (code.startsWith("DirectoryCreated")) 
           	 	return true;
           	 else
           		return false;					
		} 
		catch(Exception e) 
		{
			throw new HpcException(
			 "Failed to create directory: " + destinationPath, 
			 HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
    }
    
    
    private boolean checkIfDirectoryExists(String endpointName, String destinationFilePath,String destinationFileName)
    throws HpcException {
    	JSONTransferAPIClient client = hpcGOTransfer.getTransferClient();
    	boolean directoryExists = false;
		try 
		{  
			JSONTransferAPIClient.Result endPointList;
	    	endPointList = client.endpointLs(endpointName, destinationFilePath);
	
	        JSONArray fileArray = endPointList.document.getJSONArray("DATA");
	        for (int i=0; i < fileArray.length(); i++) {
	            JSONObject fileObject = fileArray.getJSONObject(i);
	            if (fileObject.getString("name") != null && fileObject.getString("name").equals(destinationFileName))
	            	directoryExists = true;
	        }
		}
		catch(Exception e) 
		{
			throw new HpcException(
			 "Failed to check the directory exists: " + destinationFileName, 
			 HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
		return directoryExists;
    }    
    
    public HpcDataTransferReport getTaskStatusReport(String taskId,
    		                                         HpcDataTransferAccount dataTransferAccount)
    	                                            throws HpcException 
    {
    	try {
		     hpcGOTransfer.setTransferCient(dataTransferAccount.getUsername(), dataTransferAccount.getPassword());	        
		     return getTaskStatusReport(taskId);
        } catch(Exception e) {
        	throw new HpcException(
   		                 "Failed to get report for task-id: " + taskId, 
   		                 HpcErrorType.DATA_TRANSFER_ERROR, e);
        }
    }
    
    private HpcDataTransferReport getTaskStatusReport(String taskId)
                                                     throws HpcException {
    	JSONTransferAPIClient client = hpcGOTransfer.getTransferClient();
    	HpcDataTransferReport hpcDataTransferReport = new HpcDataTransferReport();
    	
        JSONTransferAPIClient.Result r;
        String resource = "/task/" +  taskId;
       // Map<String, String> params = new HashMap<String, String>();
    //    params.put("fields", "status");
        try {
	        r = client.getResult(resource);
	        r.document.getString("status");
	        hpcDataTransferReport.setTaskID(taskId);
			hpcDataTransferReport.setTaskType(r.document.getString("type"));
	        hpcDataTransferReport.setStatus(r.document.getString("status"));
	        if (r.document.has("request_time") && !r.document.isNull("request_time"))
	        	hpcDataTransferReport.setRequestTime(convertToLexicalTime(r.document.getString("request_time")));
	        else
	        	hpcDataTransferReport.setRequestTime(null);  
	        if (r.document.has("deadline") && !r.document.isNull("deadline"))
	        	hpcDataTransferReport.setDeadline(convertToLexicalTime(r.document.getString("deadline")));
	        else
	        	hpcDataTransferReport.setDeadline(null);     
	        if (r.document.has("completion_time") && !r.document.isNull("completion_time"))
	        	hpcDataTransferReport.setCompletionTime(convertToLexicalTime(r.document.getString("completion_time")));
	        else
	        	hpcDataTransferReport.setCompletionTime(null);
	        hpcDataTransferReport.setTotalTasks(r.document.getInt("subtasks_total"));
	        hpcDataTransferReport.setTasksSuccessful(r.document.getInt("subtasks_succeeded"));
	        hpcDataTransferReport.setTasksExpired(r.document.getInt("subtasks_expired"));
	        hpcDataTransferReport.setTasksCanceled(r.document.getInt("subtasks_canceled"));
	        hpcDataTransferReport.setTasksPending(r.document.getInt("subtasks_pending"));
	        hpcDataTransferReport.setTasksRetrying(r.document.getInt("subtasks_retrying"));
	        hpcDataTransferReport.setCommand(r.document.getString("command"));
	        hpcDataTransferReport.setSourceEndpoint(r.document.getString("source_endpoint"));
	        hpcDataTransferReport.setDestinationEndpoint(r.document.getString("destination_endpoint"));
	        hpcDataTransferReport.setDataEncryption(r.document.getBoolean("encrypt_data"));
	        hpcDataTransferReport.setChecksumVerification(r.document.getBoolean("verify_checksum"));
	        hpcDataTransferReport.setDelete(r.document.getBoolean("delete_destination_extra"));
	        hpcDataTransferReport.setFiles(r.document.getInt("files"));
	        hpcDataTransferReport.setFilesSkipped(r.document.getInt("files_skipped"));
	        hpcDataTransferReport.setDirectories(r.document.getInt("directories"));
	        hpcDataTransferReport.setBytesTransferred(r.document.getLong("bytes_transferred"));
	        hpcDataTransferReport.setBytesChecksummed(r.document.getLong("bytes_checksummed"));
	        hpcDataTransferReport.setEffectiveMbitsPerSec(r.document.getDouble("effective_bytes_per_second"));
	        hpcDataTransferReport.setFaults(r.document.getInt("faults"));
	        
			return hpcDataTransferReport;
			
		} catch(Exception e) {
	        throw new HpcException(
	        		     "Failed to get task report for task: " + taskId, 
	        		     HpcErrorType.DATA_TRANSFER_ERROR, e);
		} 
    }

    private Calendar convertToLexicalTime(String timeStr) {
    	if (timeStr == null || "null".equalsIgnoreCase(timeStr))    	
    		return null;     	
    	else
    		return DatatypeConverter.parseDateTime(timeStr.trim().replace(' ', 'T'));
	}

    
	private JSONObject setJSONItem(HpcDataTransferLocations transferLocations,JSONTransferAPIClient client,String nihUsername)  throws HpcException {
    	JSONObject item = new JSONObject();
    	try {
	        item.put("DATA_TYPE", "transfer_item");
	        item.put("source_endpoint", transferLocations.getSource().getEndpoint());
	        item.put("source_path", transferLocations.getSource().getPath());
	        item.put("destination_endpoint", transferLocations.getDestination().getEndpoint());
	        //if(transferLocations.getDestination() != null && transferLocations.getDestination().getPath() != null && !transferLocations.getDestination().getPath().isEmpty())
	        	//item.put("destination_path", getGODestinationPath(transferLocations.getDestination().getPath(),nihUsername));
	        //else
	        item.put("destination_path", getGODestinationPath(transferLocations.getDestination().getPath(),transferLocations.getSource().getPath(),nihUsername,transferLocations.getDestination().getEndpoint()));
	        item.put("recursive", checkFileDirectoryAndSetRecursive(transferLocations.getSource().getEndpoint(),transferLocations.getSource().getPath(),client));
	        return item;
	        
		} catch(Exception e) {
	        throw new HpcException(
	        		     "Failed to create JSON: " + transferLocations, 
	        		     HpcErrorType.DATA_TRANSFER_ERROR, e);
		}    
    }

    private String getGODestinationPath(String destinationPath, String path,String nihUsername,String desEndpoint) throws HpcException {
    	String sourceFileName = path.substring(path.lastIndexOf('/')+1);
    	String destinationFileName = destinationPath.substring(destinationPath.lastIndexOf('/')+1);
    	StringBuilder createPath = new  StringBuilder(hpcGOTransfer.getDestinationBaseLocation()+"/"+nihUsername+"/");
    	if(!checkIfDirectoryExists(desEndpoint,createPath.toString(),destinationFileName))
    		createDestinationDirectory(desEndpoint,createPath.toString(),destinationFileName);
		return hpcGOTransfer.getDestinationBaseLocation()+"/"+nihUsername+"/"+destinationFileName+"/"+sourceFileName;
	}

	public boolean autoActivate(String endpointName, JSONTransferAPIClient client)
                               throws HpcException {
		try {
             String resource = BaseTransferAPIClient.endpointPath(endpointName)
                               + "/autoactivate?if_expires_in=100";
             JSONTransferAPIClient.Result r = client.postResult(resource, null,
                                                                null);
             String code = r.document.getString("code");
             if (code.startsWith("AutoActivationFailed")) {
            	 return false;
             }
             return true;
             
		} catch(Exception e) {
		        throw new HpcException(
		        		     "Failed to activate endpoint: " + endpointName, 
		        		     HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
    }

    public boolean checkFileDirectoryAndSetRecursive(String endpointName, String path,JSONTransferAPIClient client)
                                                    throws HpcException {
        Map<String, String> params = new HashMap<String, String>();
        if (path != null) {
            params.put("path", path);
        }
        try
        {
        	String resource = BaseTransferAPIClient.endpointPath(endpointName)
                          + "/ls";
        	client.getResult(resource, params);
        	
        } catch(APIError error) {
        	if("ExternalError.DirListingFailed.NotDirectory".equals(error.code))
        		return false;
        } catch(Exception e) {
	        throw new HpcException(
       		     "Failed to check file directory: " + endpointName + ":" + path, 
       		     HpcErrorType.DATA_TRANSFER_ERROR, e);
        }
                
        return true;

    }	
}
