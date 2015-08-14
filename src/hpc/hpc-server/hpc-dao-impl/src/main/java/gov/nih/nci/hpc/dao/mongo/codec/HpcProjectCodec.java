/**
 * HpcProjectCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeProjectMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcProject;

import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC Project Codec. 
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcProjectCodec extends HpcCodec<HpcProject>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcProjectCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcProject> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcProject project,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		String id = project.getId();
		List<String> datasetIds = project.getDatasetIds();
		HpcProjectMetadata metadata = project.getMetadata();
 
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(DATASET_ID_KEY, id);
		}
		if(metadata != null) {
			   document.put(PROJECT_METADATA_KEY, metadata);
		}
		if(datasetIds != null) {
		   document.put(PROJECT_DATASET_IDS_KEY, datasetIds);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	@SuppressWarnings("unchecked")
	public HpcProject decode(BsonReader reader, 
			                 DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		
		// Map the attributes
		HpcProject project = new HpcProject();
		project.setId(document.getString(DATASET_ID_KEY));
		project.setMetadata(decodeProjectMetadata(
				   document.get(PROJECT_METADATA_KEY, Document.class), 
				   decoderContext, getRegistry()));
		List<String> datasetIds = 
			(List<String>) document.get(PROJECT_DATASET_IDS_KEY);
		project.getDatasetIds().addAll(datasetIds);
		
		return project;
	}
	
	@Override
	public Class<HpcProject> getEncoderClass() 
	{
		return HpcProject.class;
	}
 }

 