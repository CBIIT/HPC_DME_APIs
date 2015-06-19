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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    // HpcManagedDataset Document keys.
	public final static String MANAGED_DATASET_ID_KEY = "id"; 
	public final static String MANAGED_DATASET_NAME_KEY = "name"; 
	public final static String MANAGED_DATASET_PRIMARY_INVESTIGATOR_ID_KEY = 
			                   "primary_investigator_id"; 
	public final static String MANAGED_DATASET_CREATOR_ID_KEY = "creator_id"; 
	public final static String MANAGED_DATASET_REGISTRATOR_ID_KEY = 
			                   "registrator_id"; 
	public final static String MANAGED_DATASET_LAB_BRANCH_KEY = "lab_branch"; 
	public final static String MANAGED_DATASET_CREATED_KEY = "created"; 
    
    // HpcFileLocation Document keys.
    public final static String FILE_LOCATION_ENDPOINT_KEY = "endpoint"; 
    public final static String FILE_LOCATION_PATH_KEY = "path"; 
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
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

 