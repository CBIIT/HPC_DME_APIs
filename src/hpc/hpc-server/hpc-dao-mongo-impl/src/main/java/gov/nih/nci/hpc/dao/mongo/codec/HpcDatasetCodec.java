/**
 * HpcDatasetCodec.java
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
import gov.nih.nci.hpc.dto.types.HpcDatasetType;

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
 * HPC Dataset Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetCodec extends HpcCodec<HpcDataset>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // BSON Document keys.
    private final static String INDEX_KEY = "index";
    private final static String LOCATION_KEY = "location"; 
    private final static String NAME_KEY = "name"; 
    private final static String TYPE_KEY = "type"; 
    private final static String SIZE_KEY = "size"; 
    
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
    public HpcDatasetCodec() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcDataset> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcDataset dataset,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		HpcDatasetLocation location = dataset.getLocation();
		Integer index = dataset.getIndex();
		String name = dataset.getName();
		HpcDatasetType type = dataset.getType();
		Double size = dataset.getSize();

		// Set the data on the BSON document.
		if(location != null) {
		   document.put(LOCATION_KEY, location);
		}
		if(index != null) {
		   document.put(INDEX_KEY, index);
		}
		if(name != null) {
		   document.put(NAME_KEY, name);
		}
		if(type != null) {
		   document.put(TYPE_KEY, type.value());
		}
		if(size != null) {
		   document.put(SIZE_KEY, size);
		}

		getRegistry().get(Document.class).encode(writer, document, encoderContext);
	}
 
	@Override
	public HpcDataset decode(BsonReader reader, DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = getRegistry().get(Document.class).decode(reader, decoderContext);
		
		// Map the document to HpcDataset instance.
		HpcDataset dataset = new HpcDataset();
		dataset.setLocation(document.get(LOCATION_KEY, HpcDatasetLocation.class));
		dataset.setIndex(document.get(INDEX_KEY, Integer.class));
		dataset.setName(document.get(NAME_KEY, String.class));
		dataset.setType(HpcDatasetType.valueOf(
				        document.get(TYPE_KEY, String.class)));
		dataset.setSize(document.get(SIZE_KEY, Double.class));
		
		return dataset;
	}
	
	@Override
	public Class<HpcDataset> getEncoderClass() 
	{
		return HpcDataset.class;
	}
}

 