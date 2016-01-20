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
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

import org.irods.jargon.core.connection.AuthScheme;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.auth.AuthResponse;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.UserAO;
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
	
	// iRODS connection attributes.
	private String irodsHost = null;
	private Integer irodsPort = null;
	private String irodsZone = null;
	private String irodsResource = null;
	private AuthScheme irodsAuthentication = null;
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param irodsHost The iRODS server host name / IP.
     * @param irodsPort The iRODS server port.
     * @param irodsZone The iRODS zone.
     * @param irodsResource The iRODS resource to use.
     * @param irodsAuthentication The iRODS authentication method to use.
     * 
     * @throws HpcException If it failed to instantiate the iRODS file system.
     */
    private HpcIRODSConnection(String irodsHost, Integer irodsPort, 
    		                   String irodsZone, String irodsResource,
    		                   AuthScheme irodsAuthentication) throws HpcException
    {
    	if(irodsHost == null || irodsHost.isEmpty() ||
    	   irodsPort == null ||
    	   irodsZone == null || irodsZone.isEmpty() ||
    	   irodsResource == null || irodsResource.isEmpty() ||
    	   irodsAuthentication == null) {
    	   throw new HpcException("Null or empty iRODS connection attributes",
                                  HpcErrorType.SPRING_CONFIGURATION_ERROR);	
    	}
    	this.irodsHost = irodsHost;
    	this.irodsPort = irodsPort;
    	this.irodsZone = irodsZone;
    	this.irodsResource = irodsResource;
    	this.irodsAuthentication = irodsAuthentication;
    	
		try {
		     irodsFileSystem = IRODSFileSystem.instance();
		     
		} catch(JargonException e) {
			    throw new HpcException("Failed to instantiate iRODs file system: " + 
		                               e.getMessage(),
	                                   HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }  
    
    /**
     * Default Constructor disabled.
     * 
     * @throws HpcException.
     */
    private HpcIRODSConnection() throws HpcException
    {
    	throw new HpcException("HpcIRODSConnection default constructor disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get iRODS file factory instance.
     * 
     * @param dataManagementAccount The Data Management System account.
     *
     * @throws HpcException
     */
    public IRODSFileFactory getIRODSFileFactory(
    		                   HpcIntegratedSystemAccount dataManagementAccount) 
    		                   throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSFileFactory(
					                   getIrodsAccount(dataManagementAccount));
			 
		} catch(JargonException e) {
			    throw new HpcException(
			    		     "Failed to get iRODs file factory instance: " + 
			                 e.getMessage(),
                             HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}
    }
    
    /**
     * Get iRODS Collection AO instance.
     *
     * @param dataManagementAccount The Data Management System account.
     *  
     * @throws HpcException
     */
    public CollectionAO getCollectionAO(
    		               HpcIntegratedSystemAccount dataManagementAccount) 
    		               throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSAccessObjectFactory().getCollectionAO(
					                   getIrodsAccount(dataManagementAccount));
			 
		} catch(JargonException e) {
			    throw new HpcException(
			                 "Failed to get iRODs Colelction Access Object: " + 
	                         e.getMessage(),
                             HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}	
    }
    
    /**
     * Get iRODS Data Object AO instance.
     *
     * @param dataManagementAccount The Data Management System account.
     * 
     * @throws HpcException
     */
    public DataObjectAO getDataObjectAO(
    		               HpcIntegratedSystemAccount dataManagementAccount) 
    		               throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSAccessObjectFactory().getDataObjectAO(
					                   getIrodsAccount(dataManagementAccount));
			 
		} catch(JargonException e) {
			    throw new HpcException(
			                 "Failed to get iRODs Data Object Access Object: " + 
	                         e.getMessage(),
                             HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}	
    }
    
    /**
     * Get iRODS User AO instance.
     *
     * @param dataManagementAccount The Data Management System account.
     * 
     * @throws HpcException
     */
    public UserAO getUserAO(
    		               HpcIntegratedSystemAccount dataManagementAccount) 
    		               throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSAccessObjectFactory().getUserAO(
					                   getIrodsAccount(dataManagementAccount));
			 
		} catch(JargonException e) {
			    throw new HpcException(
			                 "Failed to get iRODs User Access Object: " + 
	                         e.getMessage(),
                             HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}	
    }
    
    /**
     * Close iRODS connection of an account.
     *
     * @param dataManagementAccount The Data Management System account.
     */
    public void closeConnection(HpcIntegratedSystemAccount dataManagementAccount)
    {
    	try {
			 irodsFileSystem.getIRODSAccessObjectFactory().closeSessionAndEatExceptions(
					            getIrodsAccount(dataManagementAccount));
			 
		} catch(Exception e) {
			    logger.error("Failed to close iRODS session: " + e);
		}
    }   
    
    /**
     * Get the iRODS zone.
     *
     */
    public String getZone()
    {
    	return irodsZone;
    }

    public String getBasePath()
    {
    	return "/" + irodsZone + "/home";
    }
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Get iRODS Account instance for a HPC user.
     * 
     * @param dataManagementAccount The Data Management System account.
     *
     * @throws HpcException
     */
    private IRODSAccount getIrodsAccount(HpcIntegratedSystemAccount dataManagementAccount) 
    		                            throws HpcException
    {
    	try {
    		IRODSAccount irodsAccount = 
    				     IRODSAccount.instance(irodsHost, irodsPort, 
    	    		                           dataManagementAccount.getUsername(), 
    	    		                           dataManagementAccount.getPassword(), "", 
	    	                                   irodsZone, irodsResource);
    		IRODSFileSystem irodsFileSystem = IRODSFileSystem.instance();
    		irodsAccount.setAuthenticationScheme(irodsAuthentication);
    		//IRODSSession session = new IRODSSession(new IRODSSimpleProtocolManager());
    		
    		AuthResponse authResponse = irodsFileSystem.getIRODSAccessObjectFactory().
    				                        authenticateIRODSAccount(irodsAccount);
    		return authResponse.getAuthenticatedIRODSAccount();
    		
    	} catch(JargonException e) {
    		    throw new HpcException("Failed instantiate an iRODS account: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
    }
}

 