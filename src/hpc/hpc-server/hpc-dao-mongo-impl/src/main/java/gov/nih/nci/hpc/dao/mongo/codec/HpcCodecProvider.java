/**
 * HpcCodecProvider.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.dto.types.HpcDataset;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.Codec;
import org.bson.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Codec Provider. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcCodecProvider implements CodecProvider
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
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
    public HpcCodecProvider()
    {
    }   
    
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // CodecProvider Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override                                                                                          
    public <T> Codec<T> get(final Class<T> clazz, 
    		                final CodecRegistry registry) {                      
        if(clazz == HpcManagedDatasetsBson.class) {                      
           return (Codec<T>) new HpcManagedDatasetsBsonCodec(registry);           
        } else if(clazz == HpcDataset.class) {
        	      return (Codec<T>) new HpcDatasetCodec(registry);  
        }
                                                                                                       
        // CodecProvider returns null if it's not a provider for the requresed Class 
        return null;                                          
    }                    
}

 