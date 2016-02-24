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
import org.irods.jargon.core.exception.AuthenticationException;
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
	private String basePath = null;
	
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
     * @param basePath The iRODS base path.
     * 
     * @throws HpcException If it failed to instantiate the iRODS file system.
     */
    private HpcIRODSConnection(String irodsHost, Integer irodsPort, 
    		                   String irodsZone, String irodsResource,
    		                   String basePath) 
    		                  throws HpcException
    {
    	if(irodsHost == null || irodsHost.isEmpty() ||
    	   irodsPort == null ||
    	   irodsZone == null || irodsZone.isEmpty() ||
    	   irodsResource == null || irodsResource.isEmpty() ||
    	   basePath == null || basePath.isEmpty()) {
    	   throw new HpcException("Null or empty iRODS connection attributes",
                                  HpcErrorType.SPRING_CONFIGURATION_ERROR);	
    	}
    	this.irodsHost = irodsHost;
    	this.irodsPort = irodsPort;
    	this.irodsZone = irodsZone;
    	this.irodsResource = irodsResource;
    	this.basePath = basePath;
    	
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
     * @param authenticatedToken An authenticated token.
     *
     * @throws HpcException
     */
    public IRODSFileFactory getIRODSFileFactory(Object authenticatedToken) 
    		                                   throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSFileFactory(
					                   getIrodsAccount(authenticatedToken));
			 
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
     * @param authenticatedToken An authenticated token.
     *  
     * @throws HpcException
     */
    public CollectionAO getCollectionAO(Object authenticatedToken) 
    		                           throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSAccessObjectFactory().getCollectionAO(
					                   getIrodsAccount(authenticatedToken));
			 
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
     * @param authenticatedToken An authenticated token.
     * 
     * @throws HpcException
     */
    public DataObjectAO getDataObjectAO(Object authenticatedToken) 
    		                           throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSAccessObjectFactory().getDataObjectAO(
					                   getIrodsAccount(authenticatedToken));
			 
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
     * @param authenticatedToken An authenticated Data Management account
     * 
     * @throws HpcException
     */
    public UserAO getUserAO(Object authenticatedToken) throws HpcException
    {
    	try {
			 return irodsFileSystem.getIRODSAccessObjectFactory().getUserAO(
					                   getIrodsAccount(authenticatedToken));
			 
		} catch(JargonException e) {
			    throw new HpcException(
			                 "Failed to get iRODs User Access Object: " + 
	                         e.getMessage(),
                             HpcErrorType.DATA_MANAGEMENT_ERROR, e);
		}	
    }
    
    /**
     * Authenticate an account.
     *
     * @param dataManagementAccount A data management account to authenticate.
     * @param ldapAuthenticated An indicator if the user was authenticated via LDAP. 
     *                          This determines the authentication scheme to use w/ Data Management. 
     * @return An authenticated IRODSAccount object, or null if authentication failed.
     */
    public IRODSAccount authenticate(HpcIntegratedSystemAccount dataManagementAccount,
    		                         boolean ldapAuthenticated)
    		                        throws HpcException
    {
    	// Set the Authentication Scheme. PAM if the caller was authenticated via LDAP, 
    	// or STANDARD otherwise.
    	AuthScheme authScheme = ldapAuthenticated ? AuthScheme.PAM : AuthScheme.STANDARD;
    	
    	try {
	    	 AuthResponse authResponse = irodsFileSystem.getIRODSAccessObjectFactory().
	    				                      authenticateIRODSAccount(toIrodsAccount(dataManagementAccount,
	    				                    		                   authScheme));
	         return authResponse != null ? authResponse.getAuthenticatedIRODSAccount() : null;
			 
    	} catch(AuthenticationException ae) {
    		    return null;
    		    
    	} catch(JargonException e) {
		        throw new HpcException("Failed authenticate an iRODS account: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
    }  
    
    /**
     * Close iRODS connection of an account.
     *
     * @param authenticatedToken An authenticated token.
     */
    public void disconnect(Object authenticatedToken)
    {
    	try {
			 irodsFileSystem.getIRODSAccessObjectFactory().closeSessionAndEatExceptions(
					            getIrodsAccount(authenticatedToken));
			 
		} catch(Exception e) {
			    logger.error("Failed to close iRODS session: " + e);
		}
    }   
    
    /**
     * Get the iRODS zone.
     *
     * @return The iRODS zone.
     */
    public String getZone()
    {
    	return irodsZone;
    }

    /**
     * Get the iRODS base path.
     *
     * @return The iRODS base path.
     */
    public String getBasePath()
    {
    	return basePath;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Instantiate an IRODSAccount from HpcIntegratedSystemAccount.
     * 
     * @param dataManagementAccount The Data Management account.
     * @param authScheme The iRODS authentication scheme (PAM or STANDARD).
     *
     * @throws HpcException
     */
    private IRODSAccount toIrodsAccount(HpcIntegratedSystemAccount dataManagementAccount,
    		                            AuthScheme authScheme) 
    		                           throws HpcException
    {
    	try {
    		IRODSAccount irodsAccount = 
    				     IRODSAccount.instance(irodsHost, irodsPort, 
    	    		                           dataManagementAccount.getUsername(), 
    	    		                           dataManagementAccount.getPassword(), "", 
	    	                                   irodsZone, irodsResource);
    		irodsAccount.setAuthenticationScheme(authScheme);
    		return irodsAccount;
    		
    	} catch(JargonException e) {
    		    throw new HpcException("Failed instantiate an iRODS account: " + 
                                       e.getMessage(),
                                       HpcErrorType.DATA_MANAGEMENT_ERROR, e);
    	}
    }
    
    /**
     * Get iRODS Account instance from an authenticated token.
     * 
     * @param authenticatedToken An authenticated token.
     * @return An authenticated iRODS account object.
     * @throws HpcException
     */
    private IRODSAccount getIrodsAccount(Object authenticatedToken) 
    		                            throws HpcException
    {
    	if(authenticatedToken == null ||
    	   !(authenticatedToken instanceof IRODSAccount)) {
    	   throw new HpcException("Invalid iRODS authentication token",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return (IRODSAccount) authenticatedToken;
    }
}

 