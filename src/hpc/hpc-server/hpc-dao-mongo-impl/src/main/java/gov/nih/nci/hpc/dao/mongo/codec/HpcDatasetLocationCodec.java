/**
 * HpcDatasetLocationCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.dto.types.HpcDatasetLocation;
import gov.nih.nci.hpc.dto.types.HpcFacility;
import gov.nih.nci.hpc.dto.types.HpcDataTransfer;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Dataset Location Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetLocationCodec extends HpcCodec<HpcDatasetLocation>
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
    public HpcDatasetLocationCodec() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcDatasetLocation> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcDatasetLocation location,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		HpcFacility facility = location.getFacility();
		String endpoint = location.getEndpoint();
		HpcDataTransfer dataTransfer = location.getDataTransfer();

		// Set the data on the BSON document.
		if(facility != null) {
		   document.put(DATASET_LOCATION_FACILITY_KEY, facility.value());
		}
		if(endpoint != null) {
		   document.put(DATASET_LOCATION_ENDPOINT_KEY, endpoint);
		}
		if(dataTransfer != null) {
		   document.put(DATASET_LOCATION_DATA_TRANSFER_KEY, dataTransfer.value());
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcDatasetLocation decode(BsonReader reader, 
			                         DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
	             getRegistry().get(Document.class).decode(reader, 
	            		                                  decoderContext);
		
		// Map the document to HpcDataset instance.
		HpcDatasetLocation location = new HpcDatasetLocation();
		location.setFacility(HpcFacility.valueOf(
				             document.get(DATASET_LOCATION_FACILITY_KEY, 
				            		      String.class)));
		location.setEndpoint(document.get(DATASET_LOCATION_ENDPOINT_KEY, 
				                          String.class));
		location.setDataTransfer(HpcDataTransfer.valueOf(
                                 document.get(DATASET_LOCATION_DATA_TRANSFER_KEY, 
                                		      String.class)));
		
		return location;
	}
	
	@Override
	public Class<HpcDatasetLocation> getEncoderClass() 
	{
		return HpcDatasetLocation.class;
	}
}

 