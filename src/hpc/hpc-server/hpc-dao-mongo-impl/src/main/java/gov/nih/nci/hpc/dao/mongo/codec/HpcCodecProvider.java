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
import gov.nih.nci.hpc.dto.types.HpcDatasetLocation;
import gov.nih.nci.hpc.domain.HpcManagedDatasets;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.Codec;
import org.bson.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
	
	private Map<Class, HpcCodec> codecs = new HashMap<Class, HpcCodec>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcCodecProvider()
    {
    	codecs.put(HpcManagedDatasets.class, new HpcManagedDatasetsBsonCodec());
    	codecs.put(HpcDataset.class, new HpcDatasetCodec());
    	codecs.put(HpcDatasetLocation.class, new HpcDatasetLocationCodec());
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
    	HpcCodec codec = codecs.get(clazz);
    	if(codec != null) {
    	   codec.setRegistry(registry);	
    	}
    	
    	return  codec;                                        
    }                    
}

 