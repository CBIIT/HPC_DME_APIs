/**
 * HpcManagedDatasetCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.model.HpcManagedDataset;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;

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

public class HpcManagedDatasetCodec extends HpcCodec<HpcManagedDataset>
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
    public HpcManagedDatasetCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcManagedDataset> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcManagedDataset managedDataset,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		String id = managedDataset.getId();
		String name = managedDataset.getName();
		String primaryInvestigatorId = managedDataset.getPrimaryInvestigatorId();
		String creatorId = managedDataset.getCreatorId();
		String registratorId = managedDataset.getRegistratorId();
		String labBranch = managedDataset.getLabBranch();
		String description = managedDataset.getDescription();
		String comments = managedDataset.getComments();
		Calendar created = managedDataset.getCreated();
		List<HpcFile> files = managedDataset.getFiles();
		List<HpcDataTransferRequest> uploadRequests = 
				                           managedDataset.getUploadRequests();
		List<HpcDataTransferRequest> downloadRequests = 
                                             managedDataset.getDownloadRequests();
 
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(MANAGED_DATASET_ID_KEY, id);
		}
		if(name != null) {
		   document.put(MANAGED_DATASET_NAME_KEY, name);
		}
		if(primaryInvestigatorId != null) {
		   document.put(MANAGED_DATASET_PRIMARY_INVESTIGATOR_ID_KEY, 
			            primaryInvestigatorId);
		}
		if(creatorId != null) {
		   document.put(MANAGED_DATASET_CREATOR_ID_KEY, creatorId);
		}
		if(registratorId != null) {
		   document.put(MANAGED_DATASET_REGISTRATOR_ID_KEY, registratorId);
		}
		if(labBranch != null) {
		   document.put(MANAGED_DATASET_LAB_BRANCH_KEY, labBranch);
		}
		if(labBranch != null) {
		   document.put(MANAGED_DATASET_DESCRIPTION_KEY, description);
		}
		if(labBranch != null) {
		   document.put(MANAGED_DATASET_COMMENTS_KEY, comments);
		}
		if(created != null) {
		   document.put(MANAGED_DATASET_CREATED_KEY, created.getTime());
		}
		if(files != null && files.size() > 0) {
		   document.put(MANAGED_DATASET_FILES_KEY, files);
		}
		if(uploadRequests != null && uploadRequests.size() > 0) {
		   document.put(MANAGED_DATASET_UPLOAD_REQUESTS_KEY, files);
		}
		if(downloadRequests != null && downloadRequests.size() > 0) {
		   document.put(MANAGED_DATASET_DOWNLOAD_REQUESTS_KEY, files);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcManagedDataset decode(BsonReader reader, 
			                        DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		
		// Map the attributes
		HpcManagedDataset managedDataset = new HpcManagedDataset();
		managedDataset.setId(document.get(MANAGED_DATASET_ID_KEY, 
				                          String.class));
		managedDataset.setName(document.get(MANAGED_DATASET_NAME_KEY, 
				                            String.class));
		managedDataset.setPrimaryInvestigatorId(
			   document.get(MANAGED_DATASET_PRIMARY_INVESTIGATOR_ID_KEY, 
					        String.class));
		managedDataset.setCreatorId(
			   document.get(MANAGED_DATASET_CREATOR_ID_KEY, 
				            String.class));
		managedDataset.setRegistratorId(
				          document.get(MANAGED_DATASET_REGISTRATOR_ID_KEY, 
				    	               String.class));
		managedDataset.setLabBranch(document.get(MANAGED_DATASET_LAB_BRANCH_KEY, 
				                                 String.class));
		managedDataset.setDescription(
				          document.get(MANAGED_DATASET_DESCRIPTION_KEY, 
                                       String.class));
		managedDataset.setComments(
		                  document.get(MANAGED_DATASET_COMMENTS_KEY, 
                                       String.class));
		
		Calendar created = Calendar.getInstance();
		created.setTime(document.get(MANAGED_DATASET_CREATED_KEY, Date.class));
		managedDataset.setCreated(created);
		
		// Map the collections.
		List<Document> fileDocuments = 
				       (List<Document>) document.get(MANAGED_DATASET_FILES_KEY);
		if(fileDocuments != null) {
		   for(Document fileDocument : fileDocuments) {
			   managedDataset.getFiles().add(decodeFile(fileDocument, 	
					                                    decoderContext));
		   }
		}
		List<Document> uploadRequestDocuments = 
			(List<Document>) document.get(MANAGED_DATASET_UPLOAD_REQUESTS_KEY);
		if(uploadRequestDocuments != null) {
		   for(Document uploadRequestDocument : uploadRequestDocuments) {
		       managedDataset.getUploadRequests().add(
		    		  decodeDataTransferRequest(uploadRequestDocument, 
		    				                    decoderContext));
		   }
		}
		List<Document> downloadRequestDocuments = 
				(List<Document>) document.get(MANAGED_DATASET_DOWNLOAD_REQUESTS_KEY);
			if(downloadRequestDocuments != null) {
			   for(Document downloadRequestDocument : downloadRequestDocuments) {
			       managedDataset.getDownloadRequests().add(
			    		  decodeDataTransferRequest(downloadRequestDocument, 
			    				                    decoderContext));
			   }
		}
		
		return managedDataset;
	}
	
	@Override
	public Class<HpcManagedDataset> getEncoderClass() 
	{
		return HpcManagedDataset.class;
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
    
    /**
     * Decode HpcDataTransferRequest
     *
     * @param doc The HpcDataTransferRequest document
     * @param decoderContext
     * @return Decoded HpcFile object.
     */
    private HpcDataTransferRequest 
            decodeDataTransferRequest(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataTransferRequest.class).decode(docReader, 
		                                                              decoderContext);
	}
}

 