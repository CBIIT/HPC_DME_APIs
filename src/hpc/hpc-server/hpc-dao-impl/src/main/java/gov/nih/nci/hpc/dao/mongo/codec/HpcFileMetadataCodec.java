/**
 * HpcFileMetadataCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;

import org.bson.BsonDocumentReader;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC File Metadata Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcFileMetadataCodec extends HpcCodec<HpcFileMetadata>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcFileMetadataCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcFileMetadata> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcFileMetadata fileMetadata,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		HpcFilePrimaryMetadata primaryMetadata = 
				                      fileMetadata.getPrimaryMetadata();
 
		// Set the data on the BSON document.
		if(primaryMetadata != null) {
		   document.put(FILE_METADATA_PRIMARY_METADATA_KEY, primaryMetadata);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcFileMetadata decode(BsonReader reader, 
			                      DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcFileMetadata fileMetadata = new HpcFileMetadata();
		fileMetadata.setPrimaryMetadata(
			decodeFilePrimaryMetadata(document.get(FILE_METADATA_PRIMARY_METADATA_KEY, 
                                                   Document.class),
                                      decoderContext));
		
		return fileMetadata;
	}
	
	@Override
	public Class<HpcFileMetadata> getEncoderClass() 
	{
		return HpcFileMetadata.class;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Decode HpcFilePrimaryMetadata
     *
     * @param doc The HpcFilePrimaryMetadata document
     * @param decoderContext
     * @return Decoded HpcFilePrimaryMetadata object.
     */
    private HpcFilePrimaryMetadata decodeFilePrimaryMetadata(
    		                                 Document doc, 
    		                                 DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcFilePrimaryMetadata.class).decode(docReader, 
		                                                              decoderContext);
	}
}

 