/**
 * HpcDataManagementServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STATUS_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcDataObjectDeletionDAO;
import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectListRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationStatus;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementService;

/**
 * <p>
 * HPC Data Management Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataManagementServiceImpl implements HpcDataManagementService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
    // The Data Management Authenticator.
	@Autowired
    private HpcDataManagementAuthenticator dataManagementAuthenticator = null;
	
	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;
	
	// Data Hierarchy Validator.
	@Autowired
	private HpcDataHierarchyValidator dataHierarchyValidator = null;
	
	// Data Management configuration locator.
	@Autowired
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;
	
	// Data Object Deletion DAO.
	@Autowired
	private HpcDataObjectDeletionDAO dataObjectDeletionDAO = null;
	
	// Data Registration DAO.
	@Autowired
	private HpcDataRegistrationDAO dataRegistrationDAO = null;
	
	// Prepared query to get data objects that have their data transfer in-progress to archive.
	private List<HpcMetadataQuery> dataTransferReceivedQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data transfer in-progress to archive.
	private List<HpcMetadataQuery> dataTransferInProgressToArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data transfer in-progress to temporary archive.
	private List<HpcMetadataQuery> dataTransferInProgressToTemporaryArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data in temporary archive.
	private List<HpcMetadataQuery> dataTransferInTemporaryArchiveQuery = new ArrayList<>();
	
	// List of subjects (user-id / group-name) that permission update is not allowed.
	private List<String> systemAdminSubjects = new ArrayList<>();
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param systemAdminSubjects The system admin subjects (which update permissions not allowed for).
     */
    private HpcDataManagementServiceImpl(String systemAdminSubjects)
    {
    	// Prepare the query to get data objects in data transfer status of received.
        dataTransferReceivedQuery.add(
            toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
            		        HpcMetadataQueryOperator.EQUAL, 
        	                HpcDataTransferUploadStatus.RECEIVED.value()));
        
    	// Prepare the query to get data objects in data transfer in-progress to archive.
        dataTransferInProgressToArchiveQuery.add(
            toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
            		        HpcMetadataQueryOperator.EQUAL, 
        	                HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE.value()));
        
        // Prepare the query to get data objects in data transfer in-progress to temporary archive.
        dataTransferInProgressToTemporaryArchiveQuery.add(
        	toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
        			        HpcMetadataQueryOperator.EQUAL, 
        			        HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE.value()));
        
        // Prepare the query to get data objects in temporary archive.
        dataTransferInTemporaryArchiveQuery.add(
        	toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
        			        HpcMetadataQueryOperator.EQUAL, 
        			        HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE.value()));
        
        // Populate the list of system admin subjects (user-id / group-name). Set permission is 
        // not allowed for these subjects.
        this.systemAdminSubjects.addAll(Arrays.asList(systemAdminSubjects.split("\\s+")));
    }   
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
	private HpcDataManagementServiceImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementService Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public boolean createDirectory(String path) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	String relativePath = dataManagementProxy.getRelativePath(path);
    	// Validate the path is not a configured base path. 
    	if(dataManagementConfigurationLocator.getBasePaths().contains(relativePath)) {
    	   throw new HpcException("Invalid collection path: " + path, 
	                              HpcErrorType.INVALID_REQUEST_INPUT); 
    	}
    	
    	// Validate the path is not root.
    	if(relativePath.equals("/")) {
    	   throw new HpcException("Invalid collection path: " + path, 
	                              HpcErrorType.INVALID_REQUEST_INPUT); 
    	}
    	
    	// Validate the directory path doesn't exist.
    	HpcPathAttributes pathAttributes = 
    	   dataManagementProxy.getPathAttributes(authenticatedToken, path);
    	if(pathAttributes.getExists()) {
    	   if(pathAttributes.getIsDirectory()) {
    		  // Directory already exists.
    		  return false;
    	   }
    	   if(pathAttributes.getIsFile()) {
    		  throw new HpcException("Path already exists as a file: " + path, 
    				                 HpcErrorType.INVALID_REQUEST_INPUT); 
    	   }
    	}
    	
    	// Validate the parent directory exists.
    	if(!dataManagementProxy.isPathParentDirectory(authenticatedToken, path)) {
    		throw new HpcException("Invalid collection path. Parent directory doesn't exist: " + path, 
                                   HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
    	}
    	
    	// Create the directory.
    	dataManagementProxy.createCollectionDirectory(authenticatedToken, path);
    	return true;
    }
    
    @Override
    public boolean isPathParentDirectory(String path) throws HpcException
    {
    	return dataManagementProxy.isPathParentDirectory(dataManagementAuthenticator.getAuthenticatedToken(), path);
    }
    
    @Override
    public boolean createFile(String path) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	
    	// Validate the file path.
    	HpcPathAttributes pathAttributes = 
    	   dataManagementProxy.getPathAttributes(authenticatedToken, path);
    	if(pathAttributes.getExists()) {
    	   if(pathAttributes.getIsFile()) {
    		  // File already exists.
    		  return false;
    	   }
    	   if(pathAttributes.getIsDirectory()) {
    		  throw new HpcException("Path already exists as a directory: " + path, 
    				                 HpcErrorType.INVALID_REQUEST_INPUT); 
    	   }
    	}
    	
    	//  Validate the parent directory exists.
    	if(!dataManagementProxy.isPathParentDirectory(authenticatedToken, path)) {
    		throw new HpcException("Invalid data object path. Parent directory doesn't exist: " + path, 
                                   HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
    	}
    	
    	// Create the data object file.
    	dataManagementProxy.createDataObjectFile(authenticatedToken, path);
    	return true;
    }
    
    @Override
    public void delete(String path, boolean quiet) throws HpcException
    {
		try {
	    	 // Delete the data object file.
	    	 dataManagementProxy.delete(dataManagementAuthenticator.getAuthenticatedToken(), path);
   	     
		} catch(HpcException e) {
			    if(quiet) {
			       logger.error("Failed to delete a file", e);
			    } else {
			    	    throw(e);
			    }
		}

    }
    
    @Override
    public void saveDataObjectDeletionRequest(String path, HpcFileLocation archiveLocation,
    		                                  boolean archiveDeleteStatus, 
    		                                  HpcMetadataEntries metadataEntries,
    		                                  boolean dataManagementDeleteStatus, String message) 
    		                                 throws HpcException
    {
		dataObjectDeletionDAO.insert(HpcRequestContext.getRequestInvoker().getNciAccount().getUserId(), 
				                     path, metadataEntries, archiveLocation, archiveDeleteStatus, 
				                     dataManagementDeleteStatus, Calendar.getInstance(), message);
    }
    
    @Override
    public void setCollectionPermission(String path, HpcSubjectPermission subjectPermission) 
                                       throws HpcException
    {
    	// Validate the permission request - ensure the subject is NOT a system account.
    	validatePermissionRequest(subjectPermission);
    	
        dataManagementProxy.setCollectionPermission(dataManagementAuthenticator.getAuthenticatedToken(), 
    		    		                            path, subjectPermission);
    }

    @Override
    public List<HpcSubjectPermission> getCollectionPermissions(String path) throws HpcException
    {
    	return dataManagementProxy.getCollectionPermissions(dataManagementAuthenticator.getAuthenticatedToken(), path);
    }

    @Override
    public HpcSubjectPermission getCollectionPermissionForUser(String path, String userId) throws HpcException
    {
    	return dataManagementProxy.getCollectionPermissionForUser(dataManagementAuthenticator.getAuthenticatedToken(), path, userId);
    }

    @Override
    public void setDataObjectPermission(String path, HpcSubjectPermission subjectPermission) 
                                       throws HpcException
    {
    	// Validate the permission request - ensure the subject is NOT a system account.
    	validatePermissionRequest(subjectPermission);
    	
    	dataManagementProxy.setDataObjectPermission(dataManagementAuthenticator.getAuthenticatedToken(), 
                                                    path, subjectPermission);
    }

    @Override
    public List<HpcSubjectPermission> getDataObjectPermissions(String path) throws HpcException
    {
    	return dataManagementProxy.getDataObjectPermissions(dataManagementAuthenticator.getAuthenticatedToken(), path);
    }
    
    @Override
    public HpcSubjectPermission getDataObjectPermissionForUser(String path, String userId) throws HpcException
    {
    	return dataManagementProxy.getDataObjectPermissionForUser(dataManagementAuthenticator.getAuthenticatedToken(), path, userId);
    }
    
    @Override
    public HpcSubjectPermission getDataObjectPermission(String path) throws HpcException
    {
    	return dataManagementProxy.getDataObjectPermissionForUser(
    			                      dataManagementAuthenticator.getAuthenticatedToken(), path, 
    			                      HpcRequestContext.getRequestInvoker().getNciAccount().getUserId());
    }

    @Override
    public void setCoOwnership(String path, String userId) throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = 
    	    	     systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS);
    	if(dataManagementAccount == null) {
    	   throw new HpcException("System Data Management Account not configured",
    	      	                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	// System account ownership request.
        HpcSubjectPermission systemAccountPermissionRequest = new HpcSubjectPermission();
        systemAccountPermissionRequest.setPermission(HpcPermission.OWN);
        systemAccountPermissionRequest.setSubject(dataManagementAccount.getUsername());
        
        // User ownership request.
        HpcSubjectPermission userPermissionRequest = new HpcSubjectPermission();
        userPermissionRequest.setPermission(HpcPermission.OWN);
        userPermissionRequest.setSubject(userId);
        
        // Determine if it's a collection or data object.
        Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
        HpcPathAttributes pathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, path);
        if(pathAttributes.getIsDirectory()) {
           dataManagementProxy.setCollectionPermission(authenticatedToken, path, systemAccountPermissionRequest);
           dataManagementProxy.setCollectionPermission(authenticatedToken, path, userPermissionRequest);
        } else if(pathAttributes.getIsFile()) {
        	      dataManagementProxy.setDataObjectPermission(authenticatedToken, path, systemAccountPermissionRequest);
        	      dataManagementProxy.setDataObjectPermission(authenticatedToken, path, userPermissionRequest);
        }
    }
    
    @Override
    public void validateHierarchy(String path, String configurationId,
    		                      boolean dataObjectRegistration) 
    		                     throws HpcException
    {
    	// Calculate the collection path to validate.
    	String validationCollectionPath = dataManagementProxy.getRelativePath(path);
    	validationCollectionPath = validationCollectionPath.substring(1, validationCollectionPath.length());
    	
    	// Build the collection path types list.
    	List<String> collectionPathTypes = new ArrayList<>();
    	StringBuilder subCollectionPath = new StringBuilder();
		for(String s : validationCollectionPath.split("/")) {
			subCollectionPath.append("/" + s);
			String collectionType = getCollectionType(subCollectionPath.toString());
			if(collectionType == null) {
			   if(!collectionPathTypes.isEmpty()) {
				  throw new HpcException("Invalid collection path hierarchy: " + path,
						                 HpcErrorType.INVALID_REQUEST_INPUT);
			   }
			} else {
			        collectionPathTypes.add(collectionType);
			}
		}

		// Perform the hierarchy validation.
		dataHierarchyValidator.validateHierarchy(configurationId, collectionPathTypes, 
				                                 dataObjectRegistration);
    }
    
    @Override
    public HpcCollection getCollection(String path, boolean list) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsDirectory()) {
    	   return dataManagementProxy.getCollection(authenticatedToken, path, list);
    	}
    	
    	return null;
    }
    
    @Override
    public HpcCollection getCollectionChildren(String path) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
   	   return dataManagementProxy.getCollectionChildren(authenticatedToken, path);
    }

    @Override
    public HpcDataObject getDataObject(String path) throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsFile()) {
    	   return dataManagementProxy.getDataObject(authenticatedToken, path);
    	}
    	
    	return null;
    }
    
    @Override
    public List<HpcDataObject> getDataObjectsUploadReceived() throws HpcException
    {
    	return dataManagementProxy.getDataObjects(
	                         dataManagementAuthenticator.getAuthenticatedToken(),
	                         dataTransferReceivedQuery);
    }
    
    @Override
    public List<HpcDataObject> getDataObjectsUploadInProgress() throws HpcException
    {
    	Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
    	List<HpcDataObject> objectsInProgress = new ArrayList<>();
    	objectsInProgress.addAll(
    		   dataManagementProxy.getDataObjects(authenticatedToken, 
    			                                  dataTransferInProgressToArchiveQuery));
    	objectsInProgress.addAll(
    		   dataManagementProxy.getDataObjects(authenticatedToken, 
    			                                  dataTransferInProgressToTemporaryArchiveQuery));
    	
    	return objectsInProgress;
    }
    
    @Override
    public List<HpcDataObject> getDataObjectsUploadInTemporaryArchive() throws HpcException
    {
    	return dataManagementProxy.getDataObjects(
    			             dataManagementAuthenticator.getAuthenticatedToken(),
    			             dataTransferInTemporaryArchiveQuery);
    }
    
    @Override
    public void closeConnection()
    {
    	try {
    	     dataManagementProxy.disconnect(dataManagementAuthenticator.getAuthenticatedToken());
    	     
    	} catch(HpcException e) {
    		    // Ignore.
    		    logger.error("Failed to close data management connection", e);
    	}
    }
    
    @Override
    public String registerDataObjects(String userId, 
                                      Map<String, HpcDataObjectRegistrationRequest> dataObjectRegistrationRequests)
                                     throws HpcException
    {
    	// Input validation
    	if(StringUtils.isEmpty(userId)) {
    	   throw new HpcException("Null / Empty userId in registration list request", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Create a data object list registration task.
    	HpcDataObjectListRegistrationTask dataObjectListRegistrationTask = new HpcDataObjectListRegistrationTask();
    	dataObjectListRegistrationTask.setUserId(userId);
    	dataObjectListRegistrationTask.setCreated(Calendar.getInstance());
    	dataObjectListRegistrationTask.setStatus(HpcDataObjectListRegistrationTaskStatus.RECEIVED);
    	
    	// Iterate through the individual data object registration requests and add them as items to the 
    	// list registration task.
    	for(String path : dataObjectRegistrationRequests.keySet()) {
    		HpcDataObjectRegistrationRequest registrationRequest = dataObjectRegistrationRequests.get(path);
    	    // Validate registration request.
    		if(!HpcDomainValidator.isValidFileLocation(registrationRequest.getSource())) {
    			throw new HpcException("Invalid source in registration request for: " + path, 
		                               HpcErrorType.INVALID_REQUEST_INPUT);
    		}
    		
    		// Create a data object registration item.
    		HpcDataObjectListRegistrationItem registrationItem = new HpcDataObjectListRegistrationItem();
    		HpcDataObjectRegistrationTaskItem reqistrationTask = new HpcDataObjectRegistrationTaskItem();
    		reqistrationTask.setPath(path);
    		registrationItem.setTask(reqistrationTask);
    		registrationItem.setRequest(registrationRequest);
    		
    		dataObjectListRegistrationTask.getItems().add(registrationItem);
    	}
    	
    	// Persist the registration request.
    	dataRegistrationDAO.upsertDataObjectListRegistrationTask(dataObjectListRegistrationTask);
    	return dataObjectListRegistrationTask.getId();
    }
    
    @Override
    public List<HpcDataObjectListRegistrationTask> getDataObjectListRegistrationTasks(
                                                      HpcDataObjectListRegistrationTaskStatus status) 
                                                      throws HpcException
    {
    	return dataRegistrationDAO.getDataObjectListRegistrationTasks(status);
    }
    
	@Override
	public void updateDataObjectListRegistrationTask(HpcDataObjectListRegistrationTask registrationTask)
                                                    throws HpcException
    {
		dataRegistrationDAO.upsertDataObjectListRegistrationTask(registrationTask);
    }
	
	@Override
	public void completeDataObjectListRegistrationTask(HpcDataObjectListRegistrationTask registrationTask,
			                                           boolean result, String message, Calendar completed)
	                                                  throws HpcException
	{
		// Input validation
		if(registrationTask == null) {
		   throw new HpcException("Invalid data object list registration task", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Cleanup the DB record.
		dataRegistrationDAO.deleteDataObjectListRegistrationTask(registrationTask.getId());
		
		// Create a registration result object.
		HpcDataObjectListRegistrationResult registrationResult = new HpcDataObjectListRegistrationResult();
		registrationResult.setId(registrationTask.getId());
		registrationResult.setUserId(registrationTask.getUserId());
		registrationResult.setResult(result);
		registrationResult.setMessage(message);
		registrationResult.setCreated(registrationTask.getCreated());
		registrationResult.setCompleted(completed);	
		registrationResult.getItems().addAll(registrationTask.getItems());
		dataRegistrationDAO.upsertDataObjectListRegistrationResult(registrationResult);
	}
	
	@Override
	public HpcDataObjectListRegistrationStatus getDataObjectListRegistrationTaskStatus(String taskId) 
                                                                                      throws HpcException
    {
		if(StringUtils.isEmpty(taskId)) {
		   throw new HpcException("Null / Empty task id", HpcErrorType.INVALID_REQUEST_INPUT);
		}
			
		HpcDataObjectListRegistrationStatus taskStatus = new HpcDataObjectListRegistrationStatus();
		HpcDataObjectListRegistrationResult taskResult = dataRegistrationDAO.getDataObjectListRegistrationResult(taskId);
		if(taskResult != null) {
		   // Task completed or failed. Return the result.
		   taskStatus.setInProgress(false);
		   taskStatus.setResult(taskResult);
		   return taskStatus;
		}
	    
		// Task still in-progress. 
		taskStatus.setInProgress(true);
		HpcDataObjectListRegistrationTask task = dataRegistrationDAO.getDataObjectListRegistrationTask(taskId);
		if(task != null) {
		   taskStatus.setTask(task);
		   return taskStatus;	
		}
		
		// Task not found.
		return null;
	}
	
	@Override
    public String getCollectionType(String path) throws HpcException
    {
    	for(HpcMetadataEntry metadataEntry : 
    		dataManagementProxy.getCollectionMetadata(
    				               dataManagementAuthenticator.getAuthenticatedToken(), path)) {
    		if(metadataEntry.getAttribute().equals(HpcMetadataValidator.COLLECTION_TYPE_ATTRIBUTE)) {
    		   return metadataEntry.getValue();
    		}
    	}
    	
    	return null;
    }
	
    @Override
    public List<HpcDataManagementConfiguration> getDataManagementConfigurations()
    {
    	return new ArrayList<>(dataManagementConfigurationLocator.values());
    }
    
	@Override
	public String findDataManagementConfigurationId(String path)
    {
		String relativePath = dataManagementProxy.getRelativePath(path);
		for(HpcDataManagementConfiguration dataManagementConfiguration : 
			dataManagementConfigurationLocator.values()) {
		    if(relativePath.startsWith(dataManagementConfiguration.getBasePath())) {
			   return dataManagementConfiguration.getId();
			}
		}
		
		return null;
    }
	
	@Override
	public String getDataManagementConfigurationId(String basePath)
	{
		return dataManagementConfigurationLocator.getConfigurationId(basePath);
	}
	
	@Override
	public HpcDataManagementConfiguration getDataManagementConfiguration(String id)
	{
		return dataManagementConfigurationLocator.get(id);
	}
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Generate a metadata query.
     * 
     * @param attribute The metadata entry attribute.
     * @param operator The query operator.
     * @param value The metadata entry value.
     * @return HpcMetadataEntry instance
     */
    private HpcMetadataQuery toMetadataQuery(String attribute, HpcMetadataQueryOperator operator, 
    		                                 String value)
    {
		HpcMetadataQuery query = new HpcMetadataQuery();
		query.setAttribute(attribute);
	    query.setOperator(operator);
	    query.setValue(value);
	    
	    return query;
    }
    
    /**
     * Validate that the subject is not system account. A system account is either the HPC system account
     * or other 'system admin' accounts that are configured to disallow permission change.
     *
     * @param subjectPermission The permission request.
     * @throws HpcException If the request is to change permission of a system account
     */
    private void validatePermissionRequest(HpcSubjectPermission subjectPermission) 
                                          throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = 
   	                               systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS);
		if(dataManagementAccount == null) {
		   throw new HpcException("System Data Management Account not configured",
		     	                  HpcErrorType.UNEXPECTED_ERROR);
		}
		
		String subject = subjectPermission.getSubject();
		if(subject.equals(dataManagementAccount.getUsername()) || systemAdminSubjects.contains(subject)) {
		   throw new HpcException("Changing permission of admin account/group is not allowed: " + subject,
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
    }
}
