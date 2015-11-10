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

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
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
    public void createCollectionDirectory(
    		          HpcIntegratedSystemAccount dataManagementAccount, 
    		          String path) 
    		          throws HpcException
    {
		try {
			 IRODSFile collectionFile = 
			      irodsConnection.getIRODSFileFactory(dataManagementAccount).instanceIRODSFile(path);
			 collectionFile.mkdirs();
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to create a collection directory: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} finally {
			       irodsConnection.closeConnection(dataManagementAccount);
		}
    }
    
    @Override    
    public void createDataObjectFile(
    		          HpcIntegratedSystemAccount dataManagementAccount, 
    		          String path) 
    		          throws HpcException
    {
		try {
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
		} finally {
			       irodsConnection.closeConnection(dataManagementAccount);
		}
    }

    @Override
    public void addMetadataToCollection(
    		       HpcIntegratedSystemAccount dataManagementAccount, 
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

		     irodsConnection.getCollectionAO(dataManagementAccount).addBulkAVUMetadataToCollection(path, avuDatas);
		     
		} catch(JargonException e) {
	            throw new HpcException("Failed to add metadata to a collection: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} finally {
			       irodsConnection.closeConnection(dataManagementAccount);
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
		} finally {
			       irodsConnection.closeConnection(dataManagementAccount);
		}
    }
    
    @Override    
    public void createParentPathDirectory(
    		          HpcIntegratedSystemAccount dataManagementAccount, 
    		          String path) 
    		          throws HpcException
    {
		try {
			 IRODSFileFactory irodsFileFactory = 
					          irodsConnection.getIRODSFileFactory(dataManagementAccount);
			 IRODSFile file = irodsFileFactory.instanceIRODSFile(path);
			 IRODSFile parentPath = irodsFileFactory.instanceIRODSFile(file.getParent());
			 
			 if(parentPath.isFile()) {
				throw new HpcException("Path exists as a file: " + parentPath.getPath(), 
                                       HpcErrorType.INVALID_REQUEST_INPUT);
			 }
			 
			 if(!parentPath.isDirectory()) {
				parentPath.mkdirs(); 
			 }
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to get a path parent: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} finally {
			       irodsConnection.closeConnection(dataManagementAccount);
		}
    }
    
    @Override    
    public boolean exists(HpcIntegratedSystemAccount dataManagementAccount, 
    		              String path) 
    		             throws HpcException
    {
		try {
			 IRODSFile file = 
					   irodsConnection.getIRODSFileFactory(dataManagementAccount).instanceIRODSFile(path);
			 return file.exists();
			 
		} catch(JargonException e) {
		        throw new HpcException("Failed to check if a path exists: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		} finally {
			       irodsConnection.closeConnection(dataManagementAccount);
		}
    }
}

 