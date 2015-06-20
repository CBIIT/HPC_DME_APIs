/**
 * HpcFileCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;

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

public class HpcFileCodec extends HpcCodec<HpcFile>
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
    public HpcFileCodec() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcFile> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcFile file,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		
		String id = file.getId();
		HpcFileType type = file.getType();
		Double size = file.getSize();
		HpcFileLocation source = file.getSource();
		HpcFileLocation location = file.getLocation();
		HpcFileMetadata metadata = file.getMetadata();
		
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(FILE_ID_KEY, id);
		}
		if(type != null) {
		   document.put(FILE_TYPE_KEY, type.value());
		}
		if(size != null) {
		   document.put(FILE_SIZE_KEY, size);
		}
		if(source != null) {
		   document.put(FILE_SOURCE_KEY, source);
		}
		if(source != null) {
		   document.put(FILE_LOCATION_KEY, location);
		}
		if(source != null) {
		   document.put(FILE_METADATA_KEY, metadata);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcFile decode(BsonReader reader, DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = getRegistry().get(Document.class).decode(reader, 
				                                                     decoderContext);
		
		// Map the document to HpcDataset instance.
		HpcFile file = new HpcFile();
		file.setId(document.get(FILE_ID_KEY, String.class));
		file.setType(HpcFileType.valueOf(
				        document.get(FILE_TYPE_KEY, String.class)));
		file.setSize(document.get(FILE_SIZE_KEY, Double.class));
		file.setSource(decodeFileLocation(document.get(FILE_SOURCE_KEY, 
                                                       Document.class),
                                          decoderContext));
		file.setLocation(decodeFileLocation(document.get(FILE_LOCATION_KEY, 
                                                         Document.class),
                                            decoderContext));
		file.setMetadata(decodeFileMetadata(document.get(FILE_METADATA_KEY, 
                                                         Document.class),
                                            decoderContext));
		
		return file;
	}
	
	@Override
	public Class<HpcFile> getEncoderClass() 
	{
		return HpcFile.class;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Decode HpcFileLocation
     *
     * @param doc The HpcFileLocation document
     * @param decoderContext
     * @return Decoded HpcDatasetLocation object.
     */
    private HpcFileLocation decodeFileLocation(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcFileLocation.class).decode(docReader, 
		                                                       decoderContext);
	}
    
    /**
     * Decode HpcFileMetadata
     *
     * @param doc The HpcFileMetadata document
     * @param decoderContext
     * @return Decoded HpcDatasetLocation object.
     */
    private HpcFileMetadata decodeFileMetadata(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcFileMetadata.class).decode(docReader, 
		                                                       decoderContext);
	}
}

 