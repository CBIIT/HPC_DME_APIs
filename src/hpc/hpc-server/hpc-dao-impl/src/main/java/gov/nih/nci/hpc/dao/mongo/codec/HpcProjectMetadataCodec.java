/**
 * HpcProjectMetadataCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;

import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC Project Metadata Codec. 
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcProjectMetadataCodec extends HpcCodec<HpcProjectMetadata>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcProjectMetadataCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcProjectMetadata> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcProjectMetadata projectMetadata,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		String name = projectMetadata.getName();
		String description = projectMetadata.getDescription();
		String primaryInvestigatorNihUserId = 
				      projectMetadata.getPrimaryInvestigatorNihUserId();
		String registrarNihUserId = projectMetadata.getRegistrarNihUserId();
		String labBranch = projectMetadata.getLabBranch();
		String doc = projectMetadata.getDoc();
		String fundingOrganization = projectMetadata.getFundingOrganization();
		String internalProjectId = projectMetadata.getInternalProjectId();
		String experimentId = projectMetadata.getExperimentId();
		List<HpcMetadataItem> metadataItems = projectMetadata.getMetadataItems();
		
		// Set the data on the BSON document.
		if(name != null) {
		   document.put(PROJECT_METADATA_NAME_KEY, name);
		}
		if(description != null) {
		   document.put(PROJECT_METADATA_DESCRIPTION_KEY, description);
		}
		if(primaryInvestigatorNihUserId != null) {
		   document.put(PROJECT_METADATA_PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY, 
			            primaryInvestigatorNihUserId);
		}
		if(registrarNihUserId != null) {
		   document.put(PROJECT_METADATA_REGISTRAR_NIH_USER_ID_KEY, 
				        registrarNihUserId);
		}
		if(labBranch != null) {
		   document.put(PROJECT_METADATA_LAB_BRANCH_KEY, labBranch);
		}
		if(doc != null) {
		   document.put(PROJECT_METADATA_DOC_KEY, doc);
		}
		if(fundingOrganization != null) {
		   document.put(PROJECT_METADATA_FUNDING_ORGANIZATION_KEY, 
				        fundingOrganization);
		}
		if(internalProjectId != null) {
		   document.put(PROJECT_METADATA_INTERNAL_PROJECT_ID_KEY, internalProjectId);
		}
		if(experimentId != null) {
		   document.put(PROJECT_METADATA_EXPERIMENT_ID_KEY, experimentId);
		}
		if(metadataItems != null && metadataItems.size() > 0) {
		   document.put(PROJECT_METADATA_ITEMS_KEY, 
				        metadataItems);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcProjectMetadata decode(BsonReader reader, 
			                         DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcProjectMetadata projectMetadata = new HpcProjectMetadata();
		projectMetadata.setName(document.getString(PROJECT_METADATA_NAME_KEY));
		projectMetadata.setDescription(document.getString(
				                       PROJECT_METADATA_DESCRIPTION_KEY));
		projectMetadata.setPrimaryInvestigatorNihUserId(
		                document.getString(
		                PROJECT_METADATA_PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY));
		projectMetadata.setRegistrarNihUserId(
		                document.getString(PROJECT_METADATA_REGISTRAR_NIH_USER_ID_KEY));
		projectMetadata.setLabBranch(document.getString(PROJECT_METADATA_LAB_BRANCH_KEY));
		projectMetadata.setDoc(document.getString(PROJECT_METADATA_DOC_KEY));
		projectMetadata.setFundingOrganization(
				        document.getString(PROJECT_METADATA_FUNDING_ORGANIZATION_KEY));
		projectMetadata.setInternalProjectId(
				        document.getString(PROJECT_METADATA_INTERNAL_PROJECT_ID_KEY));
		projectMetadata.setExperimentId(
				        document.getString(PROJECT_METADATA_EXPERIMENT_ID_KEY));
		
		// Map the collections.
		@SuppressWarnings("unchecked")
		List<Document> metadataItemDocuments = 
		    (List<Document>) document.get(PROJECT_METADATA_ITEMS_KEY);
		if(metadataItemDocuments != null) {
		   for(Document metadataItemDocument : metadataItemDocuments) {
			   projectMetadata.getMetadataItems().add(
					  decodeMetadataItem(metadataItemDocument, 	
					                     decoderContext, getRegistry()));
		   }
		}
		
		return projectMetadata;
	}
	
	@Override
	public Class<HpcProjectMetadata> getEncoderClass() 
	{
		return HpcProjectMetadata.class;
	}
}

 