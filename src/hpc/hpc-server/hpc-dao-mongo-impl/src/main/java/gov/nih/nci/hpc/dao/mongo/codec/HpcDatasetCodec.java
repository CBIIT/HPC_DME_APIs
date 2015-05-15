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
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * <p>
 * HPC Managed Datasets BSON Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetCodec implements Codec<HpcDataset>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // BSON Document keys.
    private final static String ID_KEY = "id";
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
	
	// The codec registry.
	private CodecRegistry codecRegistry;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDatasetCodec() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor w/ codec registry.
     * 
     * @param codecRegistry registry..
     */
    public HpcDatasetCodec(CodecRegistry codecRegistry)                              
    {
    	this.codecRegistry = codecRegistry;
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
		String id = dataset.getId();
		String name = dataset.getName();
		HpcDatasetType type = dataset.getType();
		Double size = dataset.getSize();

		// Set the data on the BSON document.
		if(location != null) {
		document.put(LOCATION_KEY, location);
		}
		if(id != null) {
		document.put(ID_KEY, id);
		}
		if(name != null) {
		document.put(NAME_KEY, name);
		}
		if(type != null) {
		document.put(TYPE_KEY, type);
		}
		if(size != null) {
		document.put(SIZE_KEY, size);
		}

		codecRegistry.get(Document.class).encode(writer, document, encoderContext);
	}
 
	@Override
	public HpcDataset decode(BsonReader reader, DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = codecRegistry.get(Document.class).decode(reader, decoderContext);
		
		// Map the document to HpcDataset instance.
		HpcDataset dataset = new HpcDataset();
		dataset.setLocation(document.get(LOCATION_KEY, HpcDatasetLocation.class));
		dataset.setId(document.get(ID_KEY, String.class));
		dataset.setName(document.get(NAME_KEY, String.class));
		dataset.setType(document.get(TYPE_KEY, HpcDatasetType.class));
		dataset.setSize(document.get(SIZE_KEY, Double.class));
		
		return dataset;
	}
	
	@Override
	public Class<HpcDataset> getEncoderClass() 
	{
		return HpcDataset.class;
	}
}

 