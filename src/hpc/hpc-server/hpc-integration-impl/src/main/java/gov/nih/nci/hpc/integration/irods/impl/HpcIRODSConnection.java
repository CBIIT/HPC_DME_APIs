/**
 * HpcIrodsConnection.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.irods.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC iRODS connection via Jargon.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcIRODSConnection
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// iRODS file system.
	private IRODSFileSystem irodsFileSystem = null;
	
	// iRODS Account.
	IRODSAccount irodsAccount = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException If it failed to instantiate the iRODS file system.
     */
    private HpcIRODSConnection() throws HpcException
    {
		try {
		     irodsFileSystem = IRODSFileSystem.instance();
		     
		     // TODO: instantiate via spring injection.
		     irodsAccount = 
                  IRODSAccount.instance("52.7.244.225", 1247, "rods", 
  		         		                "irods", "/tempZone/home/rods", 
  		    	    	                "tempZone", "dsnetresource");
		     
		} catch(JargonException e) {
			    throw new HpcException("Failed to instantiate iRODs file system: " + 
		                               e.getMessage(),
	                                   HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get iRODS file factory instance.
     *
     * @throws HpcException
     */
    public IRODSFileFactory getIRODSFileFactory() throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSFileFactory(irodsAccount);
			 
		} catch(JargonException e) {
			    throw new HpcException(
			    		     "Failed to get iRODs file factory instance: " + 
			                 e.getMessage(),
                             HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }
    
    /**
     * Get iRODS file factory instance.
     *
     * @throws HpcException
     */
    public CollectionAO getCollectionAO() throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSAccessObjectFactory().getCollectionAO(irodsAccount);
			 
		} catch(JargonException e) {
			    throw new HpcException(
			                 "Failed to get iRODs Colelction Access Object: " + 
	                         e.getMessage(),
                             HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}	
    }
    
    /**
     * Get iRODS file factory instance.
     *
     * @throws HpcException
     */
    public void closeConnection()
    {
    	try {
			 irodsFileSystem.getIRODSAccessObjectFactory().closeSessionAndEatExceptions(irodsAccount);
			 
		} catch(JargonException e) {
			    logger.error("Failed to close iRODS session: " + e);
		}
    }   
}

 