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
import gov.nih.nci.hpc.dto.types.HpcDataCenter;

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
    // Constants
    //---------------------------------------------------------------------//    
    
    // BSON Document keys.
    private final static String DATA_CENTER_KEY = "data_center";
    private final static String PATH_KEY = "path"; 
    
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
		HpcDataCenter dataCenter = location.getDataCenter();
		String path = location.getPath();

		// Set the data on the BSON document.
		if(dataCenter != null) {
		   document.put(DATA_CENTER_KEY, dataCenter.value());
		}
		if(path != null) {
		   document.put(PATH_KEY, path);
		}
		
		getRegistry().get(Document.class).encode(writer, document, encoderContext);
	}
 
	@Override
	public HpcDatasetLocation decode(BsonReader reader, 
			                         DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = getRegistry().get(Document.class).decode(reader, decoderContext);
		
		// Map the document to HpcDataset instance.
		HpcDatasetLocation location = new HpcDatasetLocation();
		location.setDataCenter(HpcDataCenter.valueOf(
				               document.get(DATA_CENTER_KEY, String.class)));
		location.setPath(document.get(PATH_KEY, String.class));
		
		return location;
	}
	
	@Override
	public Class<HpcDatasetLocation> getEncoderClass() 
	{
		return HpcDatasetLocation.class;
	}
}

 