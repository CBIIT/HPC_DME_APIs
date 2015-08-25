/**
 * HpcVersionedFileMetadataCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeFileMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataVersion;

import java.util.Calendar;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC File Metadata Version Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcFileMetadataVersionCodec extends HpcCodec<HpcFileMetadataVersion>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcFileMetadataVersionCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcVersionedFileMetadata> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcFileMetadataVersion metadataVersion,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		HpcFileMetadata metadata = metadataVersion.getMetadata();
		Calendar created = metadataVersion.getCreated();
		int version = metadataVersion.getVersion();
 
		// Set the data on the BSON document.
		if(metadata != null) {
		   document.put(FILE_METADATA_VERSION_METADATA_KEY, metadata);
		}
		if(created != null) {
		   document.put(FILE_METADATA_VERSION_CREATED_KEY, created.getTime());
		}
		document.put(FILE_METADATA_VERSION_VERSION_KEY, version);

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcFileMetadataVersion decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcFileMetadataVersion metadataVersion = new HpcFileMetadataVersion();
		metadataVersion.setMetadata(
				 decodeFileMetadata(document.get(FILE_METADATA_VERSION_METADATA_KEY, 
                                                 Document.class),
                                    decoderContext, getRegistry()));
		if(document.getDate(FILE_METADATA_VERSION_CREATED_KEY) != null) {
		   Calendar created = Calendar.getInstance();
		   created.setTime(document.getDate(FILE_METADATA_VERSION_CREATED_KEY));
		   metadataVersion.setCreated(created);
		}
		metadataVersion.setVersion(document.getInteger(FILE_METADATA_VERSION_VERSION_KEY));
		
		return metadataVersion;
	}
	
	@Override
	public Class<HpcFileMetadataVersion> getEncoderClass() 
	{
		return HpcFileMetadataVersion.class;
	}
}

 