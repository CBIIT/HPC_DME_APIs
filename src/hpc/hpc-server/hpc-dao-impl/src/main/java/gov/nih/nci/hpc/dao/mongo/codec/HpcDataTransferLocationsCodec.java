/**
 * HpcDataTransferLocationsCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC Data Transfer Locations Codec. 
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcDataTransferLocationsCodec extends HpcCodec<HpcDataTransferLocations>
{ 

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

	/**
     * Default Constructor.
     * 
     * 
     */
    private HpcDataTransferLocationsCodec()
    {
    }   

    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcDataTransferRequest> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcDataTransferLocations dataTransferLocations,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
		
		// Extract the data from the domain object.
		HpcFileLocation source = dataTransferLocations.getSource();
		HpcFileLocation destination = dataTransferLocations.getDestination();
 
		// Set the data on the BSON document.
		if(source != null) {
		   document.put(DATA_TRANSFER_LOCATIONS_SOURCE_KEY, source);
		}
		if(destination != null) {
		   document.put(DATA_TRANSFER_LOCATIONS_DESTINATION_KEY, destination);
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcDataTransferLocations decode(BsonReader reader, 
			                               DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
	             getRegistry().get(Document.class).decode(reader, 
	            		                                  decoderContext);
		
		// Map the document to HpcDataTransferAccount instance.
		HpcDataTransferLocations dataTransferLocations = new HpcDataTransferLocations();
		dataTransferLocations.setSource(
			decodeFileLocation(
			    	  document.get(DATA_TRANSFER_LOCATIONS_SOURCE_KEY, Document.class), 
			    	  decoderContext, getRegistry()));	
		dataTransferLocations.setDestination(
				decodeFileLocation(
				    	  document.get(DATA_TRANSFER_LOCATIONS_DESTINATION_KEY, Document.class), 
				    	  decoderContext, getRegistry()));	
		
		return dataTransferLocations;
	}
	
	@Override
	public Class<HpcDataTransferLocations> getEncoderClass() 
	{
		return HpcDataTransferLocations.class;
	}
}

 