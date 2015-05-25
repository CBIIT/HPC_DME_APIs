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

import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.domain.HpcDatasetLocation;
import gov.nih.nci.hpc.domain.HpcDatasetType;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.BsonDocumentReader;
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
		String id = dataset.getId();
		String name = dataset.getName();
		HpcDatasetType type = dataset.getType();
		Double size = dataset.getSize();

		// Set the data on the BSON document.
		if(location != null) {
		   document.put(DATASET_LOCATION_KEY, location);
		}
		if(id != null) {
		   document.put(DATASET_ID_KEY, id);
		}
		if(name != null) {
		   document.put(DATASET_NAME_KEY, name);
		}
		if(type != null) {
		   document.put(DATASET_TYPE_KEY, type.value());
		}
		if(size != null) {
		   document.put(DATASET_SIZE_KEY, size);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcDataset decode(BsonReader reader, DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = getRegistry().get(Document.class).decode(reader, 
				                                                     decoderContext);
		
		// Map the document to HpcDataset instance.
		HpcDataset dataset = new HpcDataset();
		dataset.setLocation(decode(document.get(DATASET_LOCATION_KEY, 
				                                Document.class),
				                   decoderContext));
		dataset.setId(document.get(DATASET_ID_KEY, String.class));
		dataset.setName(document.get(DATASET_NAME_KEY, String.class));
		dataset.setType(HpcDatasetType.valueOf(
				        document.get(DATASET_TYPE_KEY, String.class)));
		dataset.setSize(document.get(DATASET_SIZE_KEY, Double.class));
		
		return dataset;
	}
	
	@Override
	public Class<HpcDataset> getEncoderClass() 
	{
		return HpcDataset.class;
	}
	
    /**
     * Decode HpcDatasetLocation
     *
     * @param doc The HpcDatasetLocation document
     * @param decoderContext
     * @return Decoded HpcDatasetLocation object.
     */
    private HpcDatasetLocation decode(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDatasetLocation.class).decode(docReader, 
		                                                          decoderContext);
	}
}

 