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
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.DuplicateDataException;
import org.irods.jargon.core.exception.InvalidInputParameterException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.protovalues.UserTypeEnum;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.domain.Collection;
import org.irods.jargon.core.pub.domain.DataObject;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.AVUQueryElement;
import org.irods.jargon.core.query.AVUQueryElement.AVUQueryPart;
import org.irods.jargon.core.query.AVUQueryOperatorEnum;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.springframework.beans.factory.annotation.Autowired;

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
    public void createCollectionDirectory(
    		          HpcIntegratedSystemAccount dataManagementAccount, 
    		          String path) 
    		          throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFile collectionFile = 
			      irodsConnection.getIRODSFileFactory(dataManagementAccount).instanceIRODSFile(path);
			 mkdirs(collectionFile);
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to create a collection directory: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override    
    public void createDataObjectFile(
    		          HpcIntegratedSystemAccount dataManagementAccount, 
    		          String path) 
    		          throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFile dataObjectFile = 
			      irodsConnection.getIRODSFileFactory(dataManagementAccount).instanceIRODSFile(path);
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
    public void addMetadataToCollection(
    		       HpcIntegratedSystemAccount dataManagementAccount, 
    		       String path,
    		       List<HpcMetadataEntry> metadataEntries) 
    		       throws HpcException
    {
    	path = addPath(path);
		List<AvuData> avuDatas = new ArrayList<AvuData>();

		try {
			logger.debug("dataManagementAccount user: " + dataManagementAccount.getUsername());
			logger.debug("dataManagementAccount metadataEntries: " + metadataEntries.toString());
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     avuDatas.add(AvuData.instance(metadataEntry.getAttribute(),
			                                   metadataEntry.getValue(), 
			                                   metadataEntry.getUnit()));
		     }

		     irodsConnection.getCollectionAO(dataManagementAccount).addBulkAVUMetadataToCollection(path, avuDatas);
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to add metadata to a collection: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public void updateCollectionMetadata(
    		          HpcIntegratedSystemAccount dataManagementAccount, 
    		          String path,
    		          List<HpcMetadataEntry> metadataEntries) 
    		          throws HpcException
    {
		try {
			path = addPath(path);
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     AvuData avuData = AvuData.instance(metadataEntry.getAttribute(),
			                                        metadataEntry.getValue(), 
			                                        metadataEntry.getUnit());
		         try {
		              irodsConnection.getCollectionAO(dataManagementAccount).
		                              modifyAvuValueBasedOnGivenAttributeAndUnit(path, avuData);
		         } catch(DataNotFoundException e) {
		        	     // Metadata was not found to update. Add it.
		        	     irodsConnection.getCollectionAO(dataManagementAccount).addAVUMetadata(path, avuData);
		         }
		     }
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to update collection metadata: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public void addMetadataToDataObject(
    		       HpcIntegratedSystemAccount dataManagementAccount, 
    		       String path,
    		       List<HpcMetadataEntry> metadataEntries) 
    		       throws HpcException
    {
		List<AvuData> avuDatas = new ArrayList<AvuData>();

		try {
			 path = addPath(path);
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     avuDatas.add(AvuData.instance(metadataEntry.getAttribute(),
			                                   metadataEntry.getValue(), 
			                                   metadataEntry.getUnit()));
		     }
		     irodsConnection.getDataObjectAO(dataManagementAccount).addBulkAVUMetadataToDataObject(path, avuDatas);
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to add metadata to a data object: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override    
    public boolean isParentPathDirectory(
    		                   HpcIntegratedSystemAccount dataManagementAccount, 
    		                   String path) 
    		                   throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFileFactory irodsFileFactory = 
					          irodsConnection.getIRODSFileFactory(dataManagementAccount);
			 IRODSFile file = irodsFileFactory.instanceIRODSFile(path);
			 IRODSFile parentPath = irodsFileFactory.instanceIRODSFile(file.getParent());
			 return parentPath.isDirectory();
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to get a path parent: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		        
		} 
    }
    
    @Override    
    public void createParentPathDirectory(
    		          HpcIntegratedSystemAccount dataManagementAccount, 
    		          String path) 
    		          throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFileFactory irodsFileFactory = 
					          irodsConnection.getIRODSFileFactory(dataManagementAccount);
			 IRODSFile file = irodsFileFactory.instanceIRODSFile(path);
			 IRODSFile parentPath = irodsFileFactory.instanceIRODSFile(file.getParent());
			 
			 if(parentPath.isFile()) {
				throw new HpcException("Path exists as a file: " + parentPath.getPath(), 
                                       HpcErrorType.INVALID_REQUEST_INPUT);
			 }
			 
			 if(!parentPath.isDirectory()) {
				mkdirs(parentPath); 
			 }
			 
		} catch(InvalidInputParameterException ex) {
			    
			    
		} catch(JargonException e) {
		        throw new HpcException("Failed to get a path parent: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		        
		} 
    }
    
    @Override    
    public HpcDataManagementPathAttributes getPathAttributes(
    		                HpcIntegratedSystemAccount dataManagementAccount, 
    		                String path) 
    		                throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFile file = 
					   irodsConnection.getIRODSFileFactory(dataManagementAccount).instanceIRODSFile(path);
			 HpcDataManagementPathAttributes attributes = new HpcDataManagementPathAttributes();
			 attributes.exists = file.exists();
			 attributes.isDirectory = file.isDirectory();
			 attributes.isFile = file.isFile();
			 
			 return attributes;
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to check if a path exists: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public HpcCollection getCollection(HpcIntegratedSystemAccount dataManagementAccount,
    		                           String path) throws HpcException
    {
    	try {
    		path = addPath(path);
             return toHpcCollection(irodsConnection.getCollectionAO(dataManagementAccount).
            		                                findByAbsolutePath(path));
             
		} catch(Exception e) {
	            throw new HpcException("Failed to get Collection: " + 
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public List<HpcCollection> getCollections(
    		    HpcIntegratedSystemAccount dataManagementAccount,
    		    List<HpcMetadataQuery> metadataQueries) 
    		    throws HpcException
    {
    	try {
    		 // Execute the query w/ Case insensitive query.
             List<Collection> irodsCollections = 
             irodsConnection.getCollectionAO(dataManagementAccount).
                             findDomainByMetadataQuery(toIRODSQuery(metadataQueries), 0, true);
             
             // Map the query results to a Domain POJO.
             List<HpcCollection> hpcCollections = new ArrayList<HpcCollection>();
             if(irodsCollections != null) {
                for(Collection irodsCollection : irodsCollections) {
            	    hpcCollections.add(toHpcCollection(irodsCollection));
                }
             }
             
             return hpcCollections;
             
    	} catch(HpcException ex) {
    		    throw ex;
    		    
		} catch(Exception e) {
	            throw new HpcException("Failed to get Collections: " + 
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public List<HpcMetadataEntry> getCollectionMetadata(
   		        HpcIntegratedSystemAccount dataManagementAccount, 
   		        String path) 
   		        throws HpcException
    {
		try {
			path = addPath(path);
			 return toHpcMetadata(irodsConnection.getCollectionAO(dataManagementAccount).
					              findMetadataValuesForCollection(path));

		} catch(Exception e) {
	            throw new HpcException("Failed to get metadata of a collection: " + 
                                      e.getMessage(),
                                      HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public HpcDataObject getDataObject(HpcIntegratedSystemAccount dataManagementAccount,
    		                           String path) throws HpcException
    {
    	try {
    		path = addPath(path);
             return toHpcDataObject(irodsConnection.getDataObjectAO(dataManagementAccount).
            		                                findByAbsolutePath(path));
             
		} catch(Exception e) {
	            throw new HpcException("Failed to get Data Object: " + 
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public List<HpcDataObject> getDataObjects(
    		    HpcIntegratedSystemAccount dataManagementAccount,
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException
    {
    	try {
    		 // Execute the query w/ Case insensitive query.
             List<DataObject> irodsDataObjects = 
             irodsConnection.getDataObjectAO(dataManagementAccount).
                             findDomainByMetadataQuery(toIRODSQuery(metadataQueries), 0, true);
             
             // Map the query results to a Domain POJO.
             List<HpcDataObject> hpcDataObjects = new ArrayList<HpcDataObject>();
             if(hpcDataObjects != null) {
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
    public List<HpcMetadataEntry> getDataObjectMetadata(
   		                          HpcIntegratedSystemAccount dataManagementAccount, 
   		                          String path) throws HpcException
    {
		try {
			 path = addPath(path);
			 return toHpcMetadata(irodsConnection.getDataObjectAO(dataManagementAccount).
					              findMetadataValuesForDataObject(path));
	
		} catch(Exception e) {
	            throw new HpcException("Failed to get metadata of a collection: " + 
	                                     e.getMessage(),
	                                     HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
	}
    
    @Override
    public String getUserType(HpcIntegratedSystemAccount dataManagementAccount) 
                             throws HpcException
    {
		try {
			 User user = irodsConnection.getUserAO(dataManagementAccount).
					                     findByName(dataManagementAccount.getUsername());
			 if(user == null) {
				return null;  
			 }
			 
			 // Bypassing a Jargon defect returning unknown for a groupadmin.
			 return user.getUserType().equals(UserTypeEnum.RODS_UNKNOWN) ? 
					"groupadmin" :  user.getUserType().getTextValue();
	
		} catch(Exception e) {
	            throw new HpcException("Failed to get user type: " + 
	                                    e.getMessage(),
	                                    HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
	}  
    
    @Override
    public void addUser(HpcIntegratedSystemAccount dataManagementAccount,
                        HpcNciAccount nciAccount, String userType) 
                       throws HpcException
    {
    	// Instantiate an iRODS user domain object.
    	User irodsUser = new User();
    	irodsUser.setName(nciAccount.getUserId());
    	irodsUser.setInfo(nciAccount.getFirstName() + " " + nciAccount.getLastName());
    	irodsUser.setComment("Created by " + dataManagementAccount.getUsername() + " via HPC-DM API");
    	irodsUser.setZone(irodsConnection.getZone());
    	irodsUser.setUserType(UserTypeEnum.valueOf(userType));
    	
    	// Add the user to iRODS.
    	try {
    	     irodsConnection.getUserAO(dataManagementAccount).addUser(irodsUser);
    	     
    	} catch(DuplicateDataException ex) {
    		    throw new HpcException("iRODS account already exists: " + nciAccount.getUserId(), 
                                       HpcRequestRejectReason.USER_ALREADY_EXISTS, ex);
    		    
		} catch(Exception e) {
                throw new HpcException("Failed add iRODS user: " + e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }
    
    @Override
    public void closeConnection(HpcIntegratedSystemAccount dataManagementAccount)
    {
    	irodsConnection.closeConnection(dataManagementAccount);
    }
    
    @Override
    public void setCollectionPermission(HpcIntegratedSystemAccount dataManagementAccount,
                                        String path,
                                        HpcUserPermission permissionRequest) 
                                       throws HpcException
    {
    	FilePermissionEnum permission = null;
    	path = addPath(path);
    	try {
    		 permission = FilePermissionEnum.valueOf(permissionRequest.getPermission());
    		 
    	} catch(Throwable t) {
    		    throw new HpcException("Invalid permission: " + permissionRequest.getPermission(),
    		    		               HpcErrorType.INVALID_REQUEST_INPUT, t);
    	}
    	
    	try {
    	     irodsConnection.getCollectionAO(dataManagementAccount).setAccessPermission(
    		     	                         irodsConnection.getZone(), 
    		     	                         path,
    			                             permissionRequest.getUserId(),
    			                             true, permission);
    	     
    	} catch(Exception e) {
                throw new HpcException("Failed to set collection permission: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	} 
    }
    
    @Override
    public void setDataObjectPermission(HpcIntegratedSystemAccount dataManagementAccount,
                                        String path,
                                        HpcUserPermission permissionRequest) 
                                       throws HpcException
    {
    	path = addPath(path);
    	FilePermissionEnum permission = null;
    	try {
    		 permission = FilePermissionEnum.valueOf(permissionRequest.getPermission());
    		 
    	} catch(Throwable t) {
    		    throw new HpcException("Invalid permission: " + permissionRequest.getPermission(),
    		    		               HpcErrorType.INVALID_REQUEST_INPUT, t);
    	}
    	
    	try {
    	     irodsConnection.getDataObjectAO(dataManagementAccount).setAccessPermission(
    		     	                         irodsConnection.getZone(), 
    		     	                         path,
    			                             permissionRequest.getUserId(),
    			                             permission);
    	     
    	} catch(Exception e) {
                throw new HpcException("Failed to set data object permission: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	} 
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------// 
    
    /**
     * Create directories. This Jargon API throws runtime exception if the 
     * path is invalid, so we catch it and convert to HpcException
     *
     * @param irodsFile The iRODS file.
     * 
     * @throws HpcException
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
     * 
     * @throws HpcException
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
    		    
    		    throw new HpcException("Invalid query operator: " + operator +
    		    		               ". Valid values: " + operatorValues,
                                       HpcErrorType.INVALID_REQUEST_INPUT , t);
    	}
    }
    
    /**
     * Prepare an iRODS query.
     *
     * @param metadataQueries The HPC metadata queries.
     * @return The iRODS query.
     * 
     * @throws HpcException
     */
    private List<AVUQueryElement> toIRODSQuery(List<HpcMetadataQuery> metadataQueries)
                                              throws HpcException
    {
    	List<AVUQueryElement> queryElements = new ArrayList<AVUQueryElement>();
    	try {
		     // Prepare the Query.
		     for(HpcMetadataQuery metadataQuery : metadataQueries) {
			     AVUQueryOperatorEnum operator = valueOf(metadataQuery.getOperator());
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
     * @return HpcMetadataEntry list.
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
     * @return HpcCollection.
     */
    private HpcCollection toHpcCollection(Collection irodsCollection)
    {
    	if(irodsCollection == null) {
    	   return null;
    	}
	
	    HpcCollection hpcCollection = new HpcCollection();
	    hpcCollection.setCollectionId(irodsCollection.getCollectionId());
	    hpcCollection.setCollectionName(removePath(irodsCollection.getCollectionName()));
	    hpcCollection.setAbsolutePath(removePath(irodsCollection.getAbsolutePath()));
	    hpcCollection.setCollectionParentName(removePath(irodsCollection.getCollectionParentName()));
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
	    
	    return hpcCollection;
    }
    
    /**
     * Convert iRODS data object to HPC data object domain-object.
     *
     * @param irodsDataObject The iRODS data object.
     * @return HpcDataObject.
     */
    private HpcDataObject toHpcDataObject(DataObject irodsDataObject)
    {
    	if(irodsDataObject == null) {
    	   return null;
    	}
	
    	HpcDataObject hpcDataObject = new HpcDataObject();
	    hpcDataObject.setId(irodsDataObject.getId());
	    hpcDataObject.setCollectionId(irodsDataObject.getCollectionId());
	    hpcDataObject.setCollectionName(removePath(irodsDataObject.getCollectionName()));
	    hpcDataObject.setAbsolutePath(removePath(irodsDataObject.getAbsolutePath()));
	    hpcDataObject.setDataReplicationNumber(irodsDataObject.getDataReplicationNumber());
	    hpcDataObject.setDataVersion(irodsDataObject.getDataVersion());
	    hpcDataObject.setDataSize(irodsDataObject.getDataSize());
	    hpcDataObject.setDataTypeName(irodsDataObject.getDataTypeName());
	    hpcDataObject.setResourceGroupName(irodsDataObject.getResourceGroupName());
	    hpcDataObject.setResourceName(irodsDataObject.getResourceName());
	    hpcDataObject.setDataPath(irodsDataObject.getDataPath());
	    hpcDataObject.setDataOwnerName(irodsDataObject.getDataOwnerName());
	    hpcDataObject.setDataOwnerZone(irodsDataObject.getDataOwnerZone());
	    hpcDataObject.setReplicationStatus(irodsDataObject.getReplicationStatus());
	    hpcDataObject.setDataStatus(irodsDataObject.getDataStatus());
	    hpcDataObject.setChecksum(irodsDataObject.getChecksum());
	    hpcDataObject.setExpiry(irodsDataObject.getExpiry());
	    hpcDataObject.setDataMapId(irodsDataObject.getDataMapId());
	    hpcDataObject.setComments(irodsDataObject.getComments());
	    hpcDataObject.setSpecColType(irodsDataObject.getSpecColType().toString());
	    
	    Calendar createdAt = Calendar.getInstance();
	    if(irodsDataObject.getCreatedAt() != null)
	    {
	    	createdAt.setTime(irodsDataObject.getCreatedAt());
	    	hpcDataObject.setCreatedAt(createdAt);
	    }
	    Calendar updatedAt = Calendar.getInstance();
	    updatedAt.setTime(irodsDataObject.getUpdatedAt());
	    hpcDataObject.setUpdatedAt(updatedAt);
	    
	    return hpcDataObject;
    }

    private String addPath(String path)
    {
    	if(path == null)
    		return irodsConnection.getBasePath();
    	if(path.startsWith(irodsConnection.getBasePath()))
    		return path;
    	else 
    		return irodsConnection.getBasePath() + (path.startsWith("/") ? "":"/")+ path;
    }

    private String removePath(String path)
    {
    	if(path == null)
    		return path;
    	
    	if(path.startsWith(irodsConnection.getBasePath()))
    		return path.substring((irodsConnection.getBasePath()).length());
    	else
    		return path;
    }

}

 