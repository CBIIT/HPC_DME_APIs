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

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.domain.error.HpcErrorType;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.Codec;
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
	
	@SuppressWarnings("rawtypes")
	private Map<Class, HpcCodec> codecs = new HashMap<Class, HpcCodec>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcCodecProvider() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param codecs The HPC codecs.
     * 
     * @throws HpcException If a HpcCodecProvider instance was not provided.
     */
    @SuppressWarnings("rawtypes")
    private HpcCodecProvider(Map<Class, HpcCodec> codecs) 
    		                throws HpcException
    {
    	if(codecs == null || codecs.isEmpty()) {
     	   throw new HpcException("Null or empty HpcCodecProvider instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.codecs.putAll(codecs);
    	logger.info("Registered Codecs: " + codecs);
    } 
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // CodecProvider Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override    
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(final Class<T> clazz, 
    		                final CodecRegistry registry) {  
		HpcCodec<T> codec = codecs.get(clazz);
    	if(codec != null) {
    	   codec.setRegistry(registry);	
    	}
    	
    	return codec;                                        
    }                    
}

 