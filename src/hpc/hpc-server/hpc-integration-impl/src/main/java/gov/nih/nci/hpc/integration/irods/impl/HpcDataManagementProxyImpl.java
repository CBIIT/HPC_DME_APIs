/**
 * HpcDataManagementProxyImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.irods.impl;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.DuplicateDataException;
import org.irods.jargon.core.exception.InvalidGroupException;
import org.irods.jargon.core.exception.InvalidInputParameterException;
import org.irods.jargon.core.exception.InvalidUserException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.protovalues.UserTypeEnum;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.domain.Collection;
import org.irods.jargon.core.pub.domain.DataObject;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.domain.UserFilePermission;
import org.irods.jargon.core.pub.domain.UserGroup;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.AVUQueryElement;
import org.irods.jargon.core.query.AVUQueryElement.AVUQueryPart;
import org.irods.jargon.core.query.AVUQueryOperatorEnum;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.core.query.RodsGenQueryEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * <p>
 * HPC Data Management Proxy iRODS Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementProxyImpl implements HpcDataManagementProxy
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
    // The iRODS connection.
	@Autowired
    private HpcIRODSConnection irodsConnection = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    private HpcDataManagementProxyImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementProxyImpl Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public Object authenticate(HpcIntegratedSystemAccount dataManagementAccount,
    		                   boolean ldapAuthenticated) 
		                      throws HpcException
    {
    	return irodsConnection.authenticate(dataManagementAccount, 
    			                            ldapAuthenticated);
    }
    
    @Override
    public HpcDataManagementAccount getHpcDataManagementAccount(Object irodsAccount) throws HpcException
    {
    	try
    	{
    		return irodsConnection.getHpcDataManagementAccount((IRODSAccount)irodsAccount);
    	} catch (ClassCastException e)
    	{
	        throw new HpcException("Failed to get Data management account from IRODSAccount: " + 
                    e.getMessage(),
                    HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
    	
    }
    
    @Override
    public Object getProxyManagementAccount(HpcDataManagementAccount irodsAccount) throws HpcException
    {
    	try
    	{
    		return irodsConnection.getIRODSAccount(irodsAccount);
		} catch(JargonException e) {
		        throw new HpcException("Failed to get iRODS account: " + 
		                               e.getMessage(),
		                               HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public void disconnect(Object authenticatedToken)
    {
    	irodsConnection.disconnect(authenticatedToken);
    }
    
    @Override    
    public void createCollectionDirectory(Object authenticatedToken, 
    		                              String path) 
    		                             throws HpcException
    {
		try {
			 IRODSFile collectionFile = 
			      irodsConnection.getIRODSFileFactory(authenticatedToken).instanceIRODSFile(getAbsolutePath(path));
			 mkdirs(collectionFile);
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to create a collection directory: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override    
    public void createDataObjectFile(Object authenticatedToken, 
    		                         String path) 
    		                        throws HpcException
    {
		try {
			 IRODSFile dataObjectFile = 
			      irodsConnection.getIRODSFileFactory(authenticatedToken).
			                      instanceIRODSFile(getAbsolutePath(path));
			 dataObjectFile.createNewFile();
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to create a file: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} catch(IOException ioe) {
	            throw new HpcException("Failed to create a file: " + 
                                       ioe.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, ioe);
		} 
    }
    
    @Override    
    public boolean delete(Object authenticatedToken, String path)
    {
		try {
			 IRODSFile dataObjectFile = 
			      irodsConnection.getIRODSFileFactory(authenticatedToken).
			                      instanceIRODSFile(getAbsolutePath(path));
			 return dataObjectFile.deleteWithForceOption();
			 
		} catch(Exception e) {
		        return false;
		} 
    }

    @Override
    public void addMetadataToCollection(Object authenticatedToken, 
    		                            String path,
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException
    {
		List<AvuData> avuDatas = new ArrayList<AvuData>();

		try {
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     avuDatas.add(AvuData.instance(metadataEntry.getAttribute(),
			                                   metadataEntry.getValue(), 
			                                   metadataEntry.getUnit()));
		     }

		     irodsConnection.getCollectionAO(authenticatedToken).
		                     addBulkAVUMetadataToCollection(getAbsolutePath(path), avuDatas);
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to add metadata to a collection: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public void updateCollectionMetadata(Object authenticatedToken, 
    		                             String path,
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException
    {
		try {
			 String absolutePath = getAbsolutePath(path);
			 CollectionAO collectionAO = irodsConnection.getCollectionAO(authenticatedToken);
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     AvuData avuData = AvuData.instance(metadataEntry.getAttribute(),
			                                        metadataEntry.getValue(), 
			                                        metadataEntry.getUnit());
		         try {
		        	  collectionAO.modifyAvuValueBasedOnGivenAttributeAndUnit(absolutePath, avuData);
		        	  
		         } catch(DataNotFoundException e) {
		        	     // Metadata was not found to update. Add it.
		        	     irodsConnection.getCollectionAO(authenticatedToken).
		        	                     addAVUMetadata(absolutePath, avuData);
		         }
		     }
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to update collection metadata: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public void addMetadataToDataObject(Object authenticatedToken, 
    		                            String path,
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException
    {
		List<AvuData> avuDatas = new ArrayList<AvuData>();

		try {
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     avuDatas.add(AvuData.instance(metadataEntry.getAttribute(),
			                                   metadataEntry.getValue(), 
			                                   metadataEntry.getUnit()));
		     }
		     irodsConnection.getDataObjectAO(authenticatedToken).
		                     addBulkAVUMetadataToDataObject(getAbsolutePath(path), avuDatas);
		     
		} catch(DuplicateDataException dde) {
			    throw new HpcException("Failed to add metadata to a data object: " + 
                                       dde.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, dde);
		} catch(JargonException e) {
	            throw new HpcException("Failed to add metadata to a data object: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public void updateDataObjectMetadata(Object authenticatedToken, 
    		                             String path,
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException
    {
		try {
			 String absolutePath = getAbsolutePath(path);
			 DataObjectAO dataObjectAO = irodsConnection.getDataObjectAO(authenticatedToken);
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     AvuData avuData = AvuData.instance(metadataEntry.getAttribute(),
			                                        metadataEntry.getValue(), 
			                                        metadataEntry.getUnit());
		         try {
		        	  dataObjectAO.modifyAvuValueBasedOnGivenAttributeAndUnit(absolutePath, avuData);
		        	  
		         } catch(DataNotFoundException e) {
		        	     // Metadata was not found to update. Add it.
		        	     irodsConnection.getDataObjectAO(authenticatedToken).
		        	                     addAVUMetadata(absolutePath, avuData);
		         }
		     }
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to update data object metadata: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override    
    public boolean isPathParentDirectory(Object authenticatedToken, 
    		                             String path) 
    		                            throws HpcException
    {
    	IRODSFile parentPath = getParentPath(authenticatedToken, path);
    	return (parentPath != null && parentPath.isDirectory());
    }
    
    @Override    
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                   String path) 
    		                                  throws HpcException
    {
		try {
			 IRODSFile file = 
					   irodsConnection.getIRODSFileFactory(authenticatedToken).
					                   instanceIRODSFile(getAbsolutePath(path));
			 HpcPathAttributes attributes = new HpcPathAttributes();
			 attributes.setExists(file.exists());
			 attributes.setIsDirectory(file.isDirectory());
			 attributes.setIsFile(file.isFile());
			 attributes.setSize(-1);
			 attributes.setIsAccessible(file.canWrite());
			 if(attributes.getIsDirectory()) {
			    attributes.getFiles().addAll(Arrays.asList(file.list()));
			 }
			 
			 return attributes;
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to check if a path exists: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public HpcCollection getCollection(Object authenticatedToken, String path, boolean list) 
    		                          throws HpcException
    {
    	try {
             return toHpcCollection(irodsConnection.getCollectionAO(authenticatedToken).
            		                                findByAbsolutePath(getAbsolutePath(path)),
            		                list ? irodsConnection.getCollectionAndDataObjectListAndSearchAO(authenticatedToken).
            		                                          listDataObjectsAndCollectionsUnderPath(getAbsolutePath(path)) :
            		                       null);
             
		} catch(Exception e) {
	            throw new HpcException("Failed to get Collection: " + path + ". " +
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public List<HpcMetadataEntry> getCollectionMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException
    {
		try {
			 return toHpcMetadata(irodsConnection.getCollectionAO(authenticatedToken).
					              findMetadataValuesForCollection(getAbsolutePath(path)));

		} catch(Exception e) {
	            throw new HpcException("Failed to get metadata of a collection: " + path + ". " +
                                      e.getMessage(),
                                      HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public HpcDataObject getDataObject(Object authenticatedToken, String path) 
    	                              throws HpcException
    {
    	try {
             return toHpcDataObject(irodsConnection.getDataObjectAO(authenticatedToken).
            		                                findByAbsolutePath(getAbsolutePath(path)));
             
		} catch(Exception e) {
	            throw new HpcException("Failed to get Data Object: " + 
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public List<HpcDataObject> getDataObjects(Object authenticatedToken,
    		                                  List<HpcMetadataQuery> metadataQueries) 
    		                                 throws HpcException
    {
    	try {
    		 // Execute the query w/ Case insensitive query.
             List<DataObject> irodsDataObjects = 
             irodsConnection.getDataObjectAO(authenticatedToken).
                             findDomainByMetadataQuery(toIRODSQuery(metadataQueries), 0, true);
             
             // Map the query results to a Domain POJO.
             List<HpcDataObject> hpcDataObjects = new ArrayList<HpcDataObject>();
             if(irodsDataObjects != null) {
                for(DataObject irodsDataObject : irodsDataObjects) {
                	hpcDataObjects.add(toHpcDataObject(irodsDataObject));
                }
             }
             
             return hpcDataObjects;
             
    	} catch(HpcException ex) {
    		    throw ex;
    		    
		} catch(Exception e) {
	            throw new HpcException("Failed to get data objects: " + 
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public List<HpcMetadataEntry> getDataObjectMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException
    {
		try {
			 return toHpcMetadata(irodsConnection.getDataObjectAO(authenticatedToken).
					              findMetadataValuesForDataObject(getAbsolutePath(path)));
	
		} catch(Exception e) {
	            throw new HpcException("Failed to get metadata of a data object: " + path + ". " + 
	                                   e.getMessage(),
	                                   HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
	}
    
    @Override
    public HpcUserRole getUserRole(Object authenticatedToken, String username) 
    		                      throws HpcException
    {
		 User irodsUser = getUser(authenticatedToken, username);
		 if(irodsUser == null) {
			return null;  
		 }
		 
		 return toHpcUserRole(irodsUser.getUserType());
	}  

    @Override
    public void addUser(Object authenticatedToken,
                        HpcNciAccount nciAccount, HpcUserRole userRole) 
                       throws HpcException
    {
    	// Instantiate an iRODS user domain object.
    	User irodsUser = new User();
    	irodsUser.setName(nciAccount.getUserId());
    	irodsUser.setInfo(nciAccount.getFirstName() + " " + nciAccount.getLastName());
    	irodsUser.setComment("Created by HPC-DM API");
    	irodsUser.setZone(irodsConnection.getZone());
    	irodsUser.setUserType(toIRODSUserType(userRole));
    	
    	// Add the user to iRODS.
    	try {
    	     irodsConnection.getUserAO(authenticatedToken).addUser(irodsUser);
    	     
    	} catch(DuplicateDataException ex) {
    		    throw new HpcException("iRODS account already exists: " + nciAccount.getUserId(), 
                                       HpcRequestRejectReason.USER_ALREADY_EXISTS, ex);
    		    
		} catch(Exception e) {
                throw new HpcException("Failed add iRODS user: " + e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }
    
    @Override
    public void updateUser(Object authenticatedToken,
                           String username, String firstName, String lastName,
                           HpcUserRole userRole) 
                          throws HpcException
    {
    	// Get the iRODS user
    	User irodsUser = getUser(authenticatedToken, username);
    	if(irodsUser == null) {
    	   throw new HpcException("iRODS account does not exist: " + username, 
                                  HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);  
		}
    	
    	// Update the iRODS user.
    	irodsUser.setInfo(firstName + " " + lastName);
    	irodsUser.setComment("Updated by HPC-DM API");
    	irodsUser.setUserType(toIRODSUserType(userRole));
    	
    	try {
    	     irodsConnection.getUserAO(authenticatedToken).updateUser(irodsUser);
    	     
		} catch(Exception e) {
                throw new HpcException("Failed to update iRODS user: " + e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}    	
    }
    
    @Override
    public void deleteUser(Object authenticatedToken, String nciUserId)
                          throws HpcException
    {
    	// Delete the user in iRODS.
    	try {
    	     irodsConnection.getUserAO(authenticatedToken).deleteUser(nciUserId);
    	     
		} catch(Exception e) {
                throw new HpcException("Failed delete iRODS user: " + e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }
    
    @Override
    public List<HpcSubjectPermission> getCollectionPermissions(Object authenticatedToken,
                                                               String path) 
                                                              throws HpcException
    {
    	try {
    		 return toHpcSubjectPermissions(
    				  irodsConnection.getCollectionAO(authenticatedToken).
    				                  listPermissionsForCollection(getAbsolutePath(path)));
    	     
    	} catch(Exception e) {
                throw new HpcException("Failed to get collection permissions: " + 
    	                               e.getMessage(),
                		               HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
    }
    
    @Override
    public void setCollectionPermission(Object authenticatedToken, String path,
                                        HpcSubjectPermission permissionRequest) 
                                       throws HpcException
    {
    	// Validate the permission string and convert to IRODS enum.
    	FilePermissionEnum permission = toIRODSFilePermissionEnum(permissionRequest.getPermission());
    	
		// Set the permission.
		try {
    	     irodsConnection.getCollectionAO(authenticatedToken).setAccessPermission(
    		     	            irodsConnection.getZone(), getAbsolutePath(path), 
    		     	            permissionRequest.getSubject(), true, permission);
    	     
		} catch(InvalidGroupException ige) {
			    throw new HpcException("Failed to set collection permission. Invalid group: " + 
			    		               permissionRequest.getSubject(),
                                       HpcErrorType.INVALID_REQUEST_INPUT, ige);
			    
		} catch(InvalidUserException iue) {
			    throw new HpcException("Failed to set collection permission. " +
			    		               (permissionRequest.getSubjectType().equals(HpcSubjectType.USER) ? 
			    		                "Invalid user: " : "Invalid group") + 
		                               permissionRequest.getSubject(),
                                       HpcErrorType.INVALID_REQUEST_INPUT, iue);
			    
    	} catch(Exception e) {
                throw new HpcException("Failed to set collection permission: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	} 
    }
    
    @Override
    public List<HpcSubjectPermission> getDataObjectPermissions(Object authenticatedToken,
                                                               String path) 
                                                              throws HpcException
    {
    	try {
    		 return toHpcSubjectPermissions(
    				  irodsConnection.getDataObjectAO(authenticatedToken).
    				                  listPermissionsForDataObject(getAbsolutePath(path)));
    	     
    	} catch(Exception e) {
                throw new HpcException("Failed to get collection permissions: " + 
    	                               e.getMessage(),
                		               HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
    }
    
    @Override
    public void setDataObjectPermission(Object authenticatedToken, String path,
                                        HpcSubjectPermission permissionRequest) 
                                       throws HpcException
    {
    	// Validate the permission string and convert to IRODS enum.
    	FilePermissionEnum permission = toIRODSFilePermissionEnum(permissionRequest.getPermission());
    	
		// Set the permission.
		try {
    	     irodsConnection.getDataObjectAO(authenticatedToken).setAccessPermission(
    		     	            irodsConnection.getZone(), getAbsolutePath(path), 
    		     	            permissionRequest.getSubject(), permission);
    	     
		} catch(InvalidGroupException ige) {
		    throw new HpcException("Failed to set data object permission. Invalid group: " + 
		    		               permissionRequest.getSubject(),
                                   HpcErrorType.INVALID_REQUEST_INPUT, ige);
		    
		} catch(InvalidUserException iue) {
		        throw new HpcException("Failed to set data object permission. " +
		       		                   (permissionRequest.getSubjectType().equals(HpcSubjectType.USER) ? 
		    		                    "Invalid user: " : "Invalid group") + 
	                                   permissionRequest.getSubject(),
                                       HpcErrorType.INVALID_REQUEST_INPUT, iue);
		    
		} catch(Exception e) {
                throw new HpcException("Failed to set data object permission: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
	@Override
	public void addGroup(Object authenticatedToken, String groupName) throws HpcException 
	{
		try {
			 UserGroup irodsUserGroup = new UserGroup();
			 irodsUserGroup.setUserGroupName(groupName);
			 irodsUserGroup.setZone(irodsConnection.getZone());
			 irodsConnection.getUserGroupAO(authenticatedToken).addUserGroup(irodsUserGroup);

    	} catch(Exception e) {
                throw new HpcException("Failed to add a group: " + e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
	}
	
	@Override
	public void deleteGroup(Object authenticatedToken, String groupName) throws HpcException 
	{
		try {
			 irodsConnection.getUserGroupAO(authenticatedToken).removeUserGroup(groupName);

    	} catch(Exception e) {
                throw new HpcException("Failed to delete a group: " + e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
	}
	
	@Override
	public boolean groupExists(Object authenticatedToken, String groupName) throws HpcException
	{
		try {
			 return (irodsConnection.getUserGroupAO(authenticatedToken).findByName(groupName) != null) ? true : false;

		} catch(Exception e) {
                throw new HpcException("Failed to get a group: " + e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}	
	}
	
	@Override
	public void addGroupMember(Object authenticatedToken, String groupName, 
                               String userId) 
                              throws HpcException
    {
		try {
			 irodsConnection.getUserGroupAO(authenticatedToken).addUserToGroup(groupName, userId, 
					                                                           irodsConnection.getZone());
			 
		} catch(InvalidGroupException ige) {
		        throw new HpcException("Failed to add group member. Invalid group: " + groupName,
                                       HpcErrorType.INVALID_REQUEST_INPUT, ige);
		    
		} catch(InvalidUserException iue) {
		        throw new HpcException("Failed to add group member. Invalid user: " + userId,
                                       HpcErrorType.INVALID_REQUEST_INPUT, iue);
		    
		} catch(Exception e) {
                throw new HpcException("Failed to add group member: " + e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }
	
	@Override
	public void deleteGroupMember(Object authenticatedToken, String groupName, 
                                  String userId) 
                                 throws HpcException
	{
		try {
			 irodsConnection.getUserGroupAO(authenticatedToken).removeUserFromGroup(groupName, userId, 
			                                                                        irodsConnection.getZone());
		
		} catch(InvalidGroupException ige) {
			    throw new HpcException("Failed to delete group member. Invalid group: " + groupName,
		                               HpcErrorType.INVALID_REQUEST_INPUT, ige);
		
		} catch(InvalidUserException iue) {
		        throw new HpcException("Failed to delete group member. Invalid user: " + userId,
		                               HpcErrorType.INVALID_REQUEST_INPUT, iue);
		
		} catch(Exception e) {
		        throw new HpcException("Failed to delete group member: " + e.getMessage(),
		                               HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
	}

	@Override
    public List<String> getGroupMembers(Object authenticatedToken, String groupName) 
                                       throws HpcException
	{
    	try {
	         return toHpcUserIds(irodsConnection.getUserGroupAO(authenticatedToken).listUserGroupMembers(groupName));
	
    	} catch(Exception e) {
	            throw new HpcException("Failed to get group members: " + e.getMessage(),
	                                   HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
	}

	@Override
    public List<String> getGroups(Object authenticatedToken, String groupSearchCriteria) 
    		                     throws HpcException
    {
    	try {
    		 String where = RodsGenQueryEnum.COL_USER_GROUP_NAME.getName() + " LIKE '" + groupSearchCriteria + "' and " +
    				 RodsGenQueryEnum.COL_USER_TYPE.getName() + " = 'rodsgroup'";
    		 return toHpcGroupNames(irodsConnection.getUserGroupAO(authenticatedToken).findWhere(where));
    		 
    	} catch(Exception e) {
                throw new HpcException("Failed to get user groups: " + e.getMessage(),
		                               HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
    }
	
	@Override
	public String getAbsolutePath(String path)
	{
    	if(path == null) {
     	   return irodsConnection.getBasePath();
     	} 
    	
     	if(path.startsWith(irodsConnection.getBasePath())) {
     	   return path;
        } else { 
     		    return irodsConnection.getBasePath() + (path.startsWith("/") ? "" : "/") + path;
        }
	}
	
    @Override
    public String getRelativePath(String absolutePath)
    {
    	if(absolutePath == null) {
    	   return null;
    	}
    	
    	// Ensure absolute path is valid with leading '/'.
    	String relativePath = getAbsolutePath(absolutePath).substring((irodsConnection.getBasePath()).length());

    	// Remove trailing '/'.
    	relativePath = StringUtils.trimTrailingCharacter(relativePath, '/');
    	return !relativePath.isEmpty() ? relativePath : "/";
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------// 
    
    /**
     * Create directories. This Jargon API throws runtime exception if the 
     * path is invalid, so we catch it and convert to HpcException
     *
     * @param irodsFile The iRODS file.
     * @throws HpcException on data management system failure.
     */
    private void mkdirs(IRODSFile irodsFile) throws HpcException
    {
    	boolean directoriesCreated = true;
    	
    	try {
    		 directoriesCreated = irodsFile.mkdirs();
    		 
    	} catch(Throwable t) {
    		    throw new HpcException("Failed to create directory: " + 
    	                               irodsFile.getPath(),
                                       HpcErrorType.INVALID_REQUEST_INPUT , t);
    	}
    	
    	if(!directoriesCreated) {
    	   throw new HpcException("Failed to create directory (possibly insufficient permission on path): " + 
                                  irodsFile.getPath(),
                                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    }
    
    /**
     * Get Query operator enum from a String.
     *
     * @param operator The string
     * @return The enum value
     * @throws HpcException on invalid enum value.
     */
    private AVUQueryOperatorEnum valueOf(String operator) throws HpcException
    {
    	try {
    		 return AVUQueryOperatorEnum.valueOf(operator);
    		 
    	} catch(Throwable t) {
    		    StringBuffer operatorValues = new StringBuffer();
    		    operatorValues.append("[ ");
    		    for(AVUQueryOperatorEnum value : AVUQueryOperatorEnum.values()) {
    		    	operatorValues.append(value + " | ");
    		    }
    		    operatorValues.replace(operatorValues.length() - 2, operatorValues.length(), "]");
    		    
//    		    throw new HpcException("Invalid query operator: " + operator +
//    		    		               ". Valid values: " + operatorValues,
//                                       HpcErrorType.INVALID_REQUEST_INPUT , t);
    		    throw new HpcException("Invalid query operator: " + operator +
 		               ". Valid values: EQUAL,NOT_EQUAL,LESS_THAN,GREATER_THAN,LESS_OR_EQUAL, LIKE",
                        HpcErrorType.INVALID_REQUEST_INPUT , t);

    	}
    }
    
    /**
     * Prepare an iRODS query.
     *
     * @param metadataQueries The HPC metadata queries.
     * @return The iRODS query.
     * @throws HpcException on data management system failure.
     */
    private List<AVUQueryElement> toIRODSQuery(List<HpcMetadataQuery> metadataQueries)
                                              throws HpcException
    {
    	List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();
    	try {
		     // Prepare the Query.
		     for(HpcMetadataQuery metadataQuery : metadataQueries) {
			     AVUQueryOperatorEnum operator = valueOf(metadataQuery.getOperator().value());
			     queryElements.add(
		              AVUQueryElement.instanceForValueQuery(AVUQueryPart.ATTRIBUTE, 
		        		                                    operator, 
		    		                                        metadataQuery.getAttribute()));
			     queryElements.add(
			    	  AVUQueryElement.instanceForValueQuery(AVUQueryPart.VALUE, 
   		        		                                    operator, 
   		        		                                    metadataQuery.getValue()));
		     }
		     
		 } catch(JargonQueryException e) {
			     throw new HpcException("Failed to get prepare iRODS query: " + 
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		 }
    	
		 return queryElements;
    }
    
    /**
     * Convert iRODS metadata results to HPC metadata domain objects.
     *
     * @param metadataValues The iRODS metadata values
     * @return A list of metadata entries.
     */
    private List<HpcMetadataEntry> toHpcMetadata(List<MetaDataAndDomainData> metadataValues)
    {
    	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
	    if(metadataValues != null) {
		   for(MetaDataAndDomainData metadataValue : metadataValues) {
		       HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
		       metadataEntry.setAttribute(metadataValue.getAvuAttribute());
		       metadataEntry.setValue(metadataValue.getAvuValue());
		       String unit = metadataValue.getAvuUnit();
		       metadataEntry.setUnit(unit != null && !unit.isEmpty() ? unit : null);
		       metadataEntries.add(metadataEntry);
		   }
	    }
	    
	    return metadataEntries;
    }
    
    /**
     * Convert iRODS collection to HPC collection domain object.
     *
     * @param irodsCollection The iRODS collection.
     * @param listingEntries A list of sub-directories and files under the collection.
     * @return A collection.
     */
    private HpcCollection toHpcCollection(Collection irodsCollection, 
    		                              List<CollectionAndDataObjectListingEntry> listingEntries)
    {
    	if(irodsCollection == null) {
    	   return null;
    	}
	
	    HpcCollection hpcCollection = new HpcCollection();
	    hpcCollection.setCollectionId(irodsCollection.getCollectionId());
	    hpcCollection.setCollectionName(getRelativePath(irodsCollection.getCollectionName()));
	    hpcCollection.setAbsolutePath(getRelativePath(irodsCollection.getAbsolutePath()));
	    hpcCollection.setCollectionParentName(getRelativePath(irodsCollection.getCollectionParentName()));
	    hpcCollection.setCollectionOwnerName(irodsCollection.getCollectionOwnerName());
	    hpcCollection.setCollectionOwnerZone(irodsCollection.getCollectionOwnerZone());
	    hpcCollection.setCollectionMapId(irodsCollection.getCollectionMapId());
	    hpcCollection.setCollectionInheritance(irodsCollection.getCollectionInheritance());
	    hpcCollection.setComments(irodsCollection.getComments());
	    hpcCollection.setInfo1(irodsCollection.getInfo1());
	    hpcCollection.setInfo2(irodsCollection.getInfo2());
	    hpcCollection.setSpecColType(irodsCollection.getSpecColType().toString());
	    
	    Calendar createdAt = Calendar.getInstance();
	    createdAt.setTime(irodsCollection.getCreatedAt());
	    hpcCollection.setCreatedAt(createdAt);
	    
	    Calendar modifiedAt = Calendar.getInstance();
	    modifiedAt.setTime(irodsCollection.getModifiedAt());
	    hpcCollection.setModifiedAt(modifiedAt);
	    
	    if(listingEntries != null) {
	       for(CollectionAndDataObjectListingEntry listingEntry : listingEntries) {
	    	   HpcCollectionListingEntry hpcCollectionListingEntry = new HpcCollectionListingEntry();
	    	   hpcCollectionListingEntry.setId(listingEntry.getId());
	    	   hpcCollectionListingEntry.setPath(getRelativePath(listingEntry.getFormattedAbsolutePath()));
	    	   if(listingEntry.isCollection()) {
	    	      hpcCollection.getSubCollections().add(hpcCollectionListingEntry);
	    	   } else if(listingEntry.isDataObject()) {
	    		         hpcCollection.getDataObjects().add(hpcCollectionListingEntry);
	    	   } else {
	    		       logger.error("Unxpected listing entry type: " + listingEntry.getObjectType()); 
	    	   }
	       }
	    }
	    
	    return hpcCollection;
    }
    
    /**
     * Convert iRODS data object to HPC data object domain-object.
     *
     * @param irodsDataObject The iRODS data object.
     * @return A data object.
     */
    private HpcDataObject toHpcDataObject(DataObject irodsDataObject)
    {
    	if(irodsDataObject == null) {
    	   return null;
    	}
	
    	HpcDataObject hpcDataObject = new HpcDataObject();
	    hpcDataObject.setId(irodsDataObject.getId());
	    hpcDataObject.setCollectionId(irodsDataObject.getCollectionId());
	    hpcDataObject.setCollectionName(getRelativePath(irodsDataObject.getCollectionName()));
	    hpcDataObject.setAbsolutePath(getRelativePath(irodsDataObject.getAbsolutePath()));
	    hpcDataObject.setDataSize(irodsDataObject.getDataSize());
	    hpcDataObject.setDataPath(irodsDataObject.getDataPath());
	    hpcDataObject.setDataOwnerName(irodsDataObject.getDataOwnerName());
	    
	    Calendar createdAt = Calendar.getInstance();
	    if(irodsDataObject.getCreatedAt() != null) {
	       createdAt.setTime(irodsDataObject.getCreatedAt());
	       hpcDataObject.setCreatedAt(createdAt);
	    }
	    
	    Calendar updatedAt = Calendar.getInstance();
	    if(irodsDataObject.getUpdatedAt() != null) {
	       updatedAt.setTime(irodsDataObject.getUpdatedAt());
	       hpcDataObject.setUpdatedAt(updatedAt);
	    }
	    
	    return hpcDataObject;
    }

    /**
     * Convert an iRODS user type to HPC user role.
     *
     * @param irodsUserType The iRODS user type.
     * @return The HPC user role.
     */
    private HpcUserRole toHpcUserRole(UserTypeEnum irodsUserType)
    {
    	switch(irodsUserType) {
    	       case RODS_ADMIN:
    		        return HpcUserRole.SYSTEM_ADMIN;
    	       case RODS_USER:
    	    	    return HpcUserRole.USER;
    	       case RODS_UNKNOWN: 
    	    	    // Current Jargon API doesn't support GROUP_ADMIN. This is a workaround 
    	    	    return HpcUserRole.GROUP_ADMIN;
    	       default:
    	    	    return HpcUserRole.NOT_REGISTERED;
    	}
    }
    
    /**
     * Convert an HPC user role to iRODS user type.
     *
     * @param userRole The user role
     * @return The iRODS user type.
     */
    private UserTypeEnum toIRODSUserType(HpcUserRole userRole)
    {
    	switch(userRole) {
    	       case SYSTEM_ADMIN:
    		        return UserTypeEnum.RODS_ADMIN;
    	       case USER:
    	    	    return UserTypeEnum.RODS_USER;
    	       case GROUP_ADMIN: 
    	    	    // Current Jargon API doesn't support GROUP_ADMIN. This is a workaround.
    	    	    return UserTypeEnum.RODS_USER;
    	       default:
    	    	    return UserTypeEnum.RODS_UNKNOWN;
    	}
    }
    
    /**
     * Get an iRODS user by account name
     *
     * @param authenticatedToken An authenticated token.
     * @param username The iRODS account name.
     * @return User or null if not found.
     * @throws HpcException on iRODS failure.
     */
    private User getUser(Object authenticatedToken, String username) 
                        throws HpcException
	{
		try {
			 return irodsConnection.getUserAO(authenticatedToken).findByName(username);
			
		} catch(DataNotFoundException dnf) {
	            // User not found.
			    return null;
		} catch(Exception e) {
			    throw new HpcException("Failed to get user: " + username + ". " + 
			                           e.getMessage(),
			                           HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
	}
    
    /**
     * Get a parent path file.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The path.
     * @return The parent file.
     * @throws HpcException on data management system failure.
     */
    private IRODSFile getParentPath(Object authenticatedToken, String path) 
    		                       throws HpcException
    {
    	if(path.equals(irodsConnection.getBasePath())) {
    	   return null;
    	}
    	
		try {
			 IRODSFileFactory irodsFileFactory = 
			                  irodsConnection.getIRODSFileFactory(authenticatedToken);
			 IRODSFile file = irodsFileFactory.instanceIRODSFile(getAbsolutePath(path));
			 return irodsFileFactory.instanceIRODSFile(file.getParent());
			 
		} catch(InvalidInputParameterException ex) {
			    return null;
		        
		} catch(JargonException e) {
		        throw new HpcException("Failed to get a parent path" + 
	                                   e.getMessage(),
	                                   HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}    
    }
    
    /**
     * Convert a list of iRODS user permission to list of HPC entity permission.
     *
     * @param irodsUserPermissions The iRODS user permissions.
     * @return A list of HPC subject permissions.
     */
    private List<HpcSubjectPermission> toHpcSubjectPermissions(List<UserFilePermission> irodsUserPermissions)
    {
    	if(irodsUserPermissions == null) {
    	   return null;
    	}
    	
    	List<HpcSubjectPermission> hpcSubjectPermissions = new ArrayList<>();
    	for(UserFilePermission irodsUserPermission : irodsUserPermissions) {
    		HpcSubjectPermission hpcSubjectPermission = new HpcSubjectPermission();
    		hpcSubjectPermission.setPermission(irodsUserPermission.getFilePermissionEnum().toString());
    		hpcSubjectPermission.setSubject(irodsUserPermission.getUserName());
    		// The IRODS user-type determines the HPC subject-type.
    		if(irodsUserPermission.getUserType().equals(UserTypeEnum.RODS_GROUP)) {
    		   hpcSubjectPermission.setSubjectType(HpcSubjectType.GROUP);
    		} else {
    			    hpcSubjectPermission.setSubjectType(HpcSubjectType.USER);
    		}
    		
    		hpcSubjectPermissions.add(hpcSubjectPermission);
    	}
    	
    	return hpcSubjectPermissions;
    }
    
    /**
     * Convert a list of iRODS user groups to HPC group names.
     *
     * @param irodsUserGroups The iRODS user groups.
     * @return A list of HPC user group names.
     */
    private List<String> toHpcGroupNames(List<UserGroup> irodsUserGroups)
    {
    	if(irodsUserGroups == null) {
    	   return null;
    	}
    	
    	List<String> hpcUserGroups = new ArrayList<>();
    	for(UserGroup irodsUserGroup : irodsUserGroups) {
    		hpcUserGroups.add(irodsUserGroup.getUserGroupName());
    	}
    	
    	return hpcUserGroups;
    }
    
    /**
     * Convert a list of iRODS users to list of HPC user ids.
     *
     * @param irodsUsers The iRODS users.
     * @return A list of HPC user ids.
     */
    private List<String> toHpcUserIds(List<User> irodsUsers)
    {
    	if(irodsUsers == null) {
    	   return null;
    	}
    	
    	List<String> hpcUserIds = new ArrayList<>();
    	for(User irodsUser : irodsUsers) {
    		hpcUserIds.add(irodsUser.getName());
    	}
    	
    	return hpcUserIds;
    }
    
    /**
     * Convert HPC permission string to irods permission enum.
     *
     * @param permission The HPC permission string.
     * @return The IRODS file permission enum.
     * @throws HpcException if failed to map the input string to an enum value.
     */
    private FilePermissionEnum toIRODSFilePermissionEnum(String permission) throws HpcException
    {
		try {
			 return FilePermissionEnum.valueOf(permission);
			 
		} catch(Throwable t) {
			    throw new HpcException("Invalid permission: " + permission + " . Valid values are " +
			    		               Arrays.asList(FilePermissionEnum.values()),
			    		               HpcErrorType.INVALID_REQUEST_INPUT, t);
		}
    }
}

 