/**
 * HpcCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * <p>
 * HPC Codec abstract base class. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public abstract class HpcCodec<T> implements Codec<T>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // HpcUser Document keys.
	public final static String USER_CREATED_KEY = "created"; 
	public final static String USER_LAST_UPDATED_KEY = "last_updated"; 
	public final static String USER_NCI_ACCOUNT_KEY = "nci_account"; 
	public final static String USER_DATA_TRANSFER_ACCOUNT_KEY = 
			                   "data_transfer_account"; 
	public final static String USER_DATA_MANAGEMENT_ACCOUNT_KEY = 
                               "data_management_account"; 
	
	// HpcNciAccount Document keys.
	public final static String NCI_ACCOUNT_USER_ID_KEY = "user_id"; 
	public final static String NCI_ACCOUNT_FIRST_NAME_KEY = "first_name"; 
	public final static String NCI_ACCOUNT_LAST_NAME_KEY = "last_name"; 
	
	// HpcIntegratedSystemAccount Document keys.
	public final static String INTEGRATED_SYSTEM_ACCOUNT_USERNAME_KEY = "username";
	public final static String INTEGRATED_SYSTEM_ACCOUNT_PASSWORD_KEY = "password";
	public final static String INTEGRATED_SYSTEM_ACCOUNT_INTEGRATED_SYSTEM_KEY = 
			                   "integrated_system";
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The codec registry.
	private CodecRegistry codecRegistry;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get Codec Registry
     *
     * @return The codec registry.
     */
    protected CodecRegistry getRegistry()                              
    {
    	return codecRegistry;
    }   
    
    /**
     * Set Codec Registry
     *
     * @param codecRegistry The codec registry.
     */
    protected void setRegistry(CodecRegistry codecRegistry)                            
    {
    	this.codecRegistry = codecRegistry;
    }   
}

 