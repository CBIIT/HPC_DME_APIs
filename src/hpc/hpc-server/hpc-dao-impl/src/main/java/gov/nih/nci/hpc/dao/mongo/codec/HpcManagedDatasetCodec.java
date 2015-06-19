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

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
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
		Calendar created = managedDataset.getCreated();
		
		//List<HpcDataset> datasets = managedDataset.getDatasets();
 
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
		if(created != null) {
		   document.put(MANAGED_DATASET_CREATED_KEY, created.getTime());
		}
		
		//if(datasets != null && datasets.size() > 0) {
		  // document.put(MANAGED_DATA_DATASETS_KEY, datasets);
		//}

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
		
		Calendar created = Calendar.getInstance();
		created.setTime(document.get(MANAGED_DATASET_CREATED_KEY, Date.class));
		managedDataset.setCreated(created);
		
		/*
		List<Document> datasetDocuments = 
				       (List<Document>) document.get(MANAGED_DATA_DATASETS_KEY);
		if(datasetDocuments != null) {
		   for(Document datasetDocument : datasetDocuments) {
			   managedData.getDatasets().add(decode(datasetDocument, 
					                                decoderContext));
		   }
		}*/
		
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
     * Decode HpcDataset
     *
     * @param doc The HpcDataset document
     * @param decoderContext
     * @return Decoded HpcDataset object.
     */
   /* private HpcDataset decode(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataset.class).decode(docReader, 
		                                                  decoderContext);
	}*/
}

 