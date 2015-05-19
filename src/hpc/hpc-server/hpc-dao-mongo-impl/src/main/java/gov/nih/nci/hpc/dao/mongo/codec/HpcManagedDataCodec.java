/**
 * HpcManagedDataCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.HpcManagedData;
import gov.nih.nci.hpc.domain.HpcManagedDatasets;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Managed Datas Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDataCodec extends HpcCodec<HpcManagedData>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // BSON Document keys.
    private final static String DATASETS_KEY = "datasets"; 
    
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
    public HpcManagedDataCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcManagedDatasets> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
					   HpcManagedData managedData,
					   EncoderContext encoderContext) 
	{
		if(managedData instanceof HpcManagedDatasets) {
			HpcManagedDatasets managedDatasets = (HpcManagedDatasets) managedData;
		   getRegistry().get(HpcManagedDatasets.class).encode(writer, 
				                                              managedDatasets, 
				                                              encoderContext);
		}
	}
 
	@Override
	public HpcManagedDatasets decode(BsonReader reader, 
			                         DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcManagedDatasets managedDatasets = new HpcManagedDatasets();
		//TODO...
		
		return managedDatasets;
	}
	
	@Override
	public Class<HpcManagedData> getEncoderClass() 
	{
		return HpcManagedData.class;
	}
}

 