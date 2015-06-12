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
    
    // HpcManagedData Document keys.
	public final static String MANAGED_DATA_ID_KEY = "id"; 
	public final static String MANAGED_DATA_TYPE_KEY = "type"; 
	public final static String MANAGED_DATA_CREATED_KEY = "created"; 
	public final static String MANAGED_DATA_PROJECT_NAME_KEY = "project_name"; 
	public final static String MANAGED_DATA_INVESTIGATOR_NAME_KEY = 
			                   "investigator_name"; 
	public final static String MANAGED_DATA_DATASETS_KEY = "datasets"; 
    
    // HpcDataset Document keys.
    public final static String DATASET_ID_KEY = "id";
    public final static String DATASET_LOCATION_KEY = "location"; 
    public final static String DATASET_NAME_KEY = "name"; 
    public final static String DATASET_TYPE_KEY = "type"; 
    public final static String DATASET_SIZE_KEY = "size"; 
    
    // HpcDatasetLocation Document keys.
    public final static String DATASET_LOCATION_FACILITY_KEY = "facility";
    public final static String DATASET_LOCATION_ENDPOINT_KEY = "endpoint"; 
    public final static String DATASET_LOCATION_DATA_TRANSFER_KEY = 
    		                   "data_transfer"; 
    
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

 