/**
 * HpcMetadataItemCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC Metadata Item Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataItemCodec extends HpcCodec<HpcMetadataItem>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcMetadataItemCodec() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcMetadataItem> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcMetadataItem item,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		String key = item.getKey();
		String value = item.getValue();

		if(key != null) {
		   document.put(METADATA_ITEM_KEY_KEY, key);
		}
		if(value != null) {
		   document.put(METADATA_ITEM_VALUE_KEY, value);
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcMetadataItem decode(BsonReader reader, 
			                      DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
	             getRegistry().get(Document.class).decode(reader, 
	            		                                  decoderContext);
		
		// Map the document to HpcDataset instance.
		HpcMetadataItem item = new HpcMetadataItem();
		item.setKey(document.get(METADATA_ITEM_KEY_KEY, String.class));
		item.setValue(document.get(METADATA_ITEM_VALUE_KEY, String.class));
		
		return item;
	}
	
	@Override
	public Class<HpcMetadataItem> getEncoderClass() 
	{
		return HpcMetadataItem.class;
	}
}

 