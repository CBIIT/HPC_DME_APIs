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
import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeFileLocation;
import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeFileMetadata;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;

import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

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
		List<String> projectIds = file.getProjectIds();
		
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
		if(location != null) {
		   document.put(FILE_LOCATION_KEY, location);
		}
		if(metadata != null) {
		   document.put(FILE_METADATA_KEY, metadata);
		}
		if(projectIds != null && !projectIds.isEmpty()) {
		   document.put(FILE_PROJECT_IDS_KEY, projectIds);
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
		file.setId(document.getString(FILE_ID_KEY));
		file.setType(HpcFileType.fromValue(
				        document.getString(FILE_TYPE_KEY)));
		file.setSize(document.getDouble(FILE_SIZE_KEY));
		file.setSource(decodeFileLocation(document.get(FILE_SOURCE_KEY, 
                                                       Document.class),
                                          decoderContext, getRegistry()));
		file.setLocation(decodeFileLocation(document.get(FILE_LOCATION_KEY, 
                                                         Document.class),
                                            decoderContext, getRegistry()));
		file.setMetadata(decodeFileMetadata(document.get(FILE_METADATA_KEY, 
                                                         Document.class),
                                            decoderContext, getRegistry()));
		@SuppressWarnings("unchecked")
		List<String> projectIds = 
		             (List<String>) document.get(FILE_PROJECT_IDS_KEY);
		if(projectIds != null && !projectIds.isEmpty()) {
		   file.getProjectIds().addAll(projectIds);
		}

		return file;
	}
	
	@Override
	public Class<HpcFile> getEncoderClass() 
	{
		return HpcFile.class;
	}
}

 