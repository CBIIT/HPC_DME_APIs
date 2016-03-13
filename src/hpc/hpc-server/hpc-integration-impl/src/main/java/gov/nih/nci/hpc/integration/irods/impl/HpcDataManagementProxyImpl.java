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
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.DuplicateDataException;
import org.irods.jargon.core.exception.InvalidInputParameterException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.protovalues.UserTypeEnum;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
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
			 path = addPath(path);
			 IRODSFile collectionFile = 
			      irodsConnection.getIRODSFileFactory(authenticatedToken).instanceIRODSFile(path);
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
			 path = addPath(path);
			 IRODSFile dataObjectFile = 
			      irodsConnection.getIRODSFileFactory(authenticatedToken).
			                      instanceIRODSFile(path);
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
			 path = addPath(path);
			 IRODSFile dataObjectFile = 
			      irodsConnection.getIRODSFileFactory(authenticatedToken).
			                      instanceIRODSFile(path);
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
    	path = addPath(path);
		List<AvuData> avuDatas = new ArrayList<AvuData>();

		try {
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     avuDatas.add(AvuData.instance(metadataEntry.getAttribute(),
			                                   metadataEntry.getValue(), 
			                                   metadataEntry.getUnit()));
		     }

		     irodsConnection.getCollectionAO(authenticatedToken).
		                     addBulkAVUMetadataToCollection(path, avuDatas);
		     
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
			 path = addPath(path);
			 CollectionAO collectionAO = irodsConnection.getCollectionAO(authenticatedToken);
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     AvuData avuData = AvuData.instance(metadataEntry.getAttribute(),
			                                        metadataEntry.getValue(), 
			                                        metadataEntry.getUnit());
		         try {
		        	  collectionAO.modifyAvuValueBasedOnGivenAttributeAndUnit(path, avuData);
		        	  
		         } catch(DataNotFoundException e) {
		        	     // Metadata was not found to update. Add it.
		        	     irodsConnection.getCollectionAO(authenticatedToken).
		        	                     addAVUMetadata(path, avuData);
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
			 path = addPath(path);
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     avuDatas.add(AvuData.instance(metadataEntry.getAttribute(),
			                                   metadataEntry.getValue(), 
			                                   metadataEntry.getUnit()));
		     }
		     irodsConnection.getDataObjectAO(authenticatedToken).addBulkAVUMetadataToDataObject(path, avuDatas);
		     
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
			 path = addPath(path);
			 DataObjectAO dataObjectAO = irodsConnection.getDataObjectAO(authenticatedToken);
		     for(HpcMetadataEntry metadataEntry : metadataEntries) {
			     AvuData avuData = AvuData.instance(metadataEntry.getAttribute(),
			                                        metadataEntry.getValue(), 
			                                        metadataEntry.getUnit());
		         try {
		        	  dataObjectAO.modifyAvuValueBasedOnGivenAttributeAndUnit(path, avuData);
		        	  
		         } catch(DataNotFoundException e) {
		        	     // Metadata was not found to update. Add it.
		        	     irodsConnection.getDataObjectAO(authenticatedToken).
		        	                     addAVUMetadata(path, avuData);
		         }
		     }
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to update data object metadata: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override    
    public boolean isParentPathDirectory(Object authenticatedToken, 
    		                             String path) 
    		                            throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFileFactory irodsFileFactory = 
			      irodsConnection.getIRODSFileFactory(authenticatedToken);
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
    public void createParentPathDirectory(Object authenticatedToken, 
    		                              String path) 
    		                             throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFileFactory irodsFileFactory = 
					          irodsConnection.getIRODSFileFactory(authenticatedToken);
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
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                   String path) 
    		                                  throws HpcException
    {
		try {
			 path = addPath(path);
			 IRODSFile file = 
					   irodsConnection.getIRODSFileFactory(authenticatedToken).
					                   instanceIRODSFile(path);
			 HpcPathAttributes attributes = new HpcPathAttributes();
			 attributes.setExists(file.exists());
			 attributes.setIsDirectory(file.isDirectory());
			 attributes.setIsFile(file.isFile());
			 attributes.setSize(-1);
			 
			 return attributes;
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to check if a path exists: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public HpcCollection getCollection(Object authenticatedToken, String path) 
    		                          throws HpcException
    {
    	try {
    		 path = addPath(path);
             return toHpcCollection(irodsConnection.getCollectionAO(authenticatedToken).
            		                                findByAbsolutePath(path));
             
		} catch(Exception e) {
	            throw new HpcException("Failed to get Collection: " + 
                                        e.getMessage(),
                                        HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public List<HpcCollection> getCollections(Object authenticatedToken,
    		                                  List<HpcMetadataQuery> metadataQueries) 
    		                                 throws HpcException
    {
    	try {
    		 // Execute the query w/ Case insensitive query.
             List<Collection> irodsCollections = 
             irodsConnection.getCollectionAO(authenticatedToken).
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
    public List<HpcMetadataEntry> getCollectionMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException
    {
		try {
			 path = addPath(path);
			 return toHpcMetadata(irodsConnection.getCollectionAO(authenticatedToken).
					              findMetadataValuesForCollection(path));

		} catch(Exception e) {
	            throw new HpcException("Failed to get metadata of a collection: " + 
                                      e.getMessage(),
                                      HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
    }
    
    @Override
    public HpcDataObject getDataObject(Object authenticatedToken,
    		                           String path) 
    	                              throws HpcException
    {
    	try {
    		 path = addPath(path);
             return toHpcDataObject(irodsConnection.getDataObjectAO(authenticatedToken).
            		                                findByAbsolutePath(path));
             
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
			 path = addPath(path);
			 return toHpcMetadata(irodsConnection.getDataObjectAO(authenticatedToken).
					              findMetadataValuesForDataObject(path));
	
		} catch(Exception e) {
	            throw new HpcException("Failed to get metadata of a collection: " + 
	                                   e.getMessage(),
	                                   HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
	}
    
    @Override
    public HpcUserRole getUserRole(Object authenticatedToken, String username) 
    		                      throws HpcException
    {
		try {
			 User user = irodsConnection.getUserAO(authenticatedToken).findByName(username);
			 if(user == null) {
				return null;  
			 }
			 
			 return toHpcUserRole(user.getUserType());
	
		} catch(Exception e) {
	            throw new HpcException("Failed to get user type: " + 
	                                    e.getMessage(),
	                                    HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
	}  
    
    @Override
    public HpcIntegratedSystemAccount getUser(Object authenticatedToken, String username) 
    		                      throws HpcException
    {
		try {
			 User user = irodsConnection.getUserAO(authenticatedToken).findByName(username);
			 if(user != null)
			 {
				 HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();
				 account.setUsername(user.getName());
				 account.setIntegratedSystem(HpcIntegratedSystem.IRODS);
				 return account;
			 }
		} catch(Exception e) {
	            throw new HpcException("Failed to get user type: " + 
	                                    e.getMessage(),
	                                    HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} 
		return null;
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
    public void setCollectionPermission(
    		       Object authenticatedToken,
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
    	     irodsConnection.getCollectionAO(authenticatedToken).setAccessPermission(
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
    public void setDataObjectPermission(
    		       Object authenticatedToken,
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
    	     irodsConnection.getDataObjectAO(authenticatedToken).setAccessPermission(
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

    /**
     * Append the base path to the user's provided path.
     *
     * @param path The user's path.
     * @return An absolute iRODS path
     */
    private String addPath(String path)
    {
    	if(path == null) {
    	   return irodsConnection.getBasePath();
    	} if(path.startsWith(irodsConnection.getBasePath())) {
    		 return path;
        } else { 
    		    return irodsConnection.getBasePath() + (path.startsWith("/") ? "":"/") + path;
        }
    }

    /**
     * Extract user's path from an absolute path.
     *
     * @param path The absolute path.
     * @return The user's path
     */
    private String removePath(String path)
    {
    	if(path == null)
    		return path;
    	
    	if(path.startsWith(irodsConnection.getBasePath()))
    		return path.substring((irodsConnection.getBasePath()).length());
    	else
    		return path;
    }

    /**
     * Convert an iRODS user type to HPC user role.
     *
     * @param userType The absolute path.
     * @return The user's path
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
     * @param userType The absolute path.
     * @return The user's path
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
}

 