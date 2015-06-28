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

import gov.nih.nci.hpc.domain.dataset.HpcDataset;
import gov.nih.nci.hpc.domain.dataset.HpcFile;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.BsonDocumentReader;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * HPC Managed Dataset Codec. 
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
 
		// Extract the data from the domain object.
		String id = dataset.getId();
		String name = dataset.getName();
		String primaryInvestigatorId = dataset.getPrimaryInvestigatorId();
		String creatorId = dataset.getCreatorId();
		String registratorId = dataset.getRegistratorId();
		String labBranch = dataset.getLabBranch();
		String description = dataset.getDescription();
		String comments = dataset.getComments();
		Calendar created = dataset.getCreated();
		List<HpcFile> files = dataset.getFiles();
 
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(DATASET_ID_KEY, id);
		}
		if(name != null) {
		   document.put(DATASET_NAME_KEY, name);
		}
		if(primaryInvestigatorId != null) {
		   document.put(DATASET_PRIMARY_INVESTIGATOR_ID_KEY, 
			            primaryInvestigatorId);
		}
		if(creatorId != null) {
		   document.put(DATASET_CREATOR_ID_KEY, creatorId);
		}
		if(registratorId != null) {
		   document.put(DATASET_REGISTRATOR_ID_KEY, registratorId);
		}
		if(labBranch != null) {
		   document.put(DATASET_LAB_BRANCH_KEY, labBranch);
		}
		if(description != null) {
		   document.put(DATASET_DESCRIPTION_KEY, description);
		}
		if(comments != null) {
		   document.put(DATASET_COMMENTS_KEY, comments);
		}
		if(created != null) {
		   document.put(DATASET_CREATED_KEY, created.getTime());
		}
		if(files != null && files.size() > 0) {
		   document.put(DATASET_FILES_KEY, files);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcDataset decode(BsonReader reader, 
			                 DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcDataset dataset = new HpcDataset();
		dataset.setId(document.get(DATASET_ID_KEY, String.class));
		dataset.setName(document.get(DATASET_NAME_KEY, String.class));
		dataset.setPrimaryInvestigatorId(
		           document.get(DATASET_PRIMARY_INVESTIGATOR_ID_KEY, 
					            String.class));
		dataset.setCreatorId(document.get(DATASET_CREATOR_ID_KEY, String.class));
		dataset.setRegistratorId(document.get(DATASET_REGISTRATOR_ID_KEY, 
				    	                      String.class));
		dataset.setLabBranch(document.get(DATASET_LAB_BRANCH_KEY, 
		                                  String.class));
		dataset.setDescription(document.get(DATASET_DESCRIPTION_KEY, 
				                            String.class));
		dataset.setComments(document.get(DATASET_COMMENTS_KEY, String.class));
		
		Calendar created = Calendar.getInstance();
		created.setTime(document.get(DATASET_CREATED_KEY, Date.class));
		dataset.setCreated(created);
		
		List<Document> fileDocuments = 
				       (List<Document>) document.get(DATASET_FILES_KEY);
		if(fileDocuments != null) {
		   for(Document fileDocument : fileDocuments) {
			   dataset.getFiles().add(decodeFile(fileDocument, decoderContext));
		   }
		}
		
		return dataset;
	}
	
	@Override
	public Class<HpcDataset> getEncoderClass() 
	{
		return HpcDataset.class;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Decode HpcFile
     *
     * @param doc The HpcFile document
     * @param decoderContext
     * @return Decoded HpcFile object.
     */
    private HpcFile decodeFile(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcFile.class).decode(docReader, 
		                                               decoderContext);
	}
}

 