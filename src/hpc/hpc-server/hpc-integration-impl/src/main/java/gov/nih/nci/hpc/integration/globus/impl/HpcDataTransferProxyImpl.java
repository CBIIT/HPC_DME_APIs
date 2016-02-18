package gov.nih.nci.hpc.integration.globus.impl;

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.globusonline.transfer.APIError;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class HpcDataTransferProxyImpl implements HpcDataTransferProxy 
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Globus transfer status strings
	private final static String FAILED_STATUS = "FAILED"; 
	private final static String ARCHIVED_STATUS = "SUCCEEDED";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Globus connection instance.
	@Autowired
    private HpcGlobusConnection globusConnection = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
	private HpcDataTransferProxyImpl()
    {
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataTransferProxy Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount) 
		                      throws HpcException
    {
    	return globusConnection.authenticate(dataTransferAccount);
    }
    
    @Override
    public String transferData(Object authenticatedToken,
    		                   HpcFileLocation source, HpcFileLocation destination)
    		                  throws HpcException
    {
    	JSONTransferAPIClient client = 
    			       globusConnection.getTransferClient(authenticatedToken);
    	
        if (!autoActivate(source.getEndpoint(), client) || 
        	!autoActivate(destination.getEndpoint(), client)) {
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
	        JSONObject item = setJSONItem(source, destination, client);
	        transfer.append("DATA", item);

	        r = client.postResult("/transfer", transfer, null);
	        String taskId = r.document.getString("task_id");
	        logger.debug("Transfer task id :"+ taskId );
	      
	
	        return taskId;
	        
        } catch(Exception e) {
	        	throw new HpcException(
       		                 "Failed to transfer: " + source + ", " + destination, 
       		                 HpcErrorType.DATA_TRANSFER_ERROR, e);
        }
    }
    
    @Override
    public HpcDataTransferStatus getDataTransferStatus(Object authenticatedToken,
                                                       String dataTransferRequestId) 
                                                      throws HpcException
    {
		 HpcDataTransferReport report = getDataTransferReport(authenticatedToken, 
				                                              dataTransferRequestId);
		 if(report.getStatus().equals(ARCHIVED_STATUS)) {
			return HpcDataTransferStatus.ARCHIVED;
		 }

		 if(report.getStatus().equals(FAILED_STATUS)) {
 			return HpcDataTransferStatus.FAILED;
 		 }
		 
		 return HpcDataTransferStatus.IN_PROGRESS;
    }
    
    @Override
    public HpcDataTransferReport getDataTransferReport(Object authenticatedToken,
                                                       String dataTransferRequestId) 
                                                      throws HpcException
    {
    	JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
    	HpcDataTransferReport hpcDataTransferReport = new HpcDataTransferReport();
    	
        JSONTransferAPIClient.Result r;
        String resource = "/task/" +  dataTransferRequestId;
       // Map<String, String> params = new HashMap<String, String>();
    //    params.put("fields", "status");
        try {
	        r = client.getResult(resource);
	        r.document.getString("status");
	        hpcDataTransferReport.setTaskID(dataTransferRequestId);
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
	        		     "Failed to get task report for task: " + dataTransferRequestId, 
	        		     HpcErrorType.DATA_TRANSFER_ERROR, e);
		} 
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    private Calendar convertToLexicalTime(String timeStr) 
    {
    	if (timeStr == null || "null".equalsIgnoreCase(timeStr))    	
    		return null;     	
    	else
    		return DatatypeConverter.parseDateTime(timeStr.trim().replace(' ', 'T'));
	}
    
	private JSONObject setJSONItem(HpcFileLocation source, HpcFileLocation destination, 
			                       JSONTransferAPIClient client)  
			                      throws HpcException 
	{
    	JSONObject item = new JSONObject();
    	try {
	        item.put("DATA_TYPE", "transfer_item");
	        item.put("source_endpoint", source.getEndpoint());
	        item.put("source_path", source.getPath());
	        item.put("destination_endpoint", destination.getEndpoint());
	        item.put("destination_path", destination.getPath());
	        item.put("recursive", checkFileDirectoryAndSetRecursive(source.getEndpoint(),
	        		                                                source.getPath(), client));
	        return item;
	        
		} catch(Exception e) {
	        throw new HpcException(
	        		     "Failed to create JSON: " + source + ", " + destination, 
	        		     HpcErrorType.DATA_TRANSFER_ERROR, e);
		}    
    }

	private boolean autoActivate(String endpointName, JSONTransferAPIClient client)
                                throws HpcException 
	{
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

    private boolean checkFileDirectoryAndSetRecursive(String endpointName, String path,
    		                                          JSONTransferAPIClient client)
                                                     throws HpcException 
    {
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
