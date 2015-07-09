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

import java.util.List;

import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;

import org.bson.BsonDocumentReader;
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
 * @version $Id:  $
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
		String registratorNihUserId = 
				          projectMetadata.getRegistratorNihUserId();
		String labBranch = projectMetadata.getLabBranch();
		String division = projectMetadata.getDivision();
		String center = projectMetadata.getCenter();
		String organization = projectMetadata.getOrganization();
		String internalProjectId = projectMetadata.getInternalProjectId();
		String experimentId = projectMetadata.getExperimentId();
		List<HpcMetadataItem> metadataItems = projectMetadata.getMetadataItems();
		
		// Set the data on the BSON document.
		if(name != null) {
		   document.put(NAME_KEY, 
				   name);
		}
		if(description != null) {
			   document.put(DESCRIPTION_KEY, description);
		}
		
		if(primaryInvestigatorNihUserId != null) {
			   document.put(
			   PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY, 
			   primaryInvestigatorNihUserId);
		}
		if(registratorNihUserId != null) {
		   document.put(REGISTRATOR_NIH_USER_ID_KEY, 
				        registratorNihUserId);
		}
		if(labBranch != null) {
		   document.put(LAB_BRANCH_KEY, labBranch);
		}
		if(division != null) {
			   document.put(DIVISION_KEY, division);
			}
		if(center != null) {
			   document.put(CENTER_KEY, center);
			}
		if(organization != null) {
			   document.put(ORGANIZATION_KEY, organization);
			}
		if(internalProjectId != null) {
			   document.put(PROJECT_INTERNAL_PROJECT_ID_KEY, internalProjectId);
			}
		if(experimentId != null) {
			   document.put(PROJECT_EXPERIMENT_ID_KEY, experimentId);
			}

		if(metadataItems != null && metadataItems.size() > 0) {
		   document.put(METADATA_ITEMS_KEY, 
				        metadataItems);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	@SuppressWarnings("unchecked")
	public HpcProjectMetadata decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcProjectMetadata projectMetadata = new HpcProjectMetadata();
		projectMetadata.setName(
				   document.get(NAME_KEY, 
                                String.class));
		projectMetadata.setDescription(
				document.get(DESCRIPTION_KEY, 
					         String.class));
		projectMetadata.setPrimaryInvestigatorNihUserId(
		    document.get(PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY, 
			             String.class));
		projectMetadata.setRegistratorNihUserId(
		    document.get(REGISTRATOR_NIH_USER_ID_KEY, 
				    	 String.class));
		projectMetadata.setLabBranch(
			document.get(LAB_BRANCH_KEY, 
		                 String.class));
		projectMetadata.setDivision(
				document.get(DIVISION_KEY, 
			                 String.class));
		projectMetadata.setCenter(
				document.get(CENTER_KEY, 
			                 String.class));
		projectMetadata.setOrganization(
				document.get(ORGANIZATION_KEY, 
			                 String.class));
		projectMetadata.setInternalProjectId(
				document.get(PROJECT_INTERNAL_PROJECT_ID_KEY, 
			                 String.class));
		projectMetadata.setExperimentId(
				document.get(PROJECT_EXPERIMENT_ID_KEY, 
			                 String.class));
		
		// Map the collections.
		List<Document> metadataItemDocuments = 
		    (List<Document>) document.get(METADATA_ITEMS_KEY);
		if(metadataItemDocuments != null) {
		   for(Document metadataItemDocument : metadataItemDocuments) {
			   projectMetadata.getMetadataItems().add(
					                  decodeMetadataItem(metadataItemDocument, 	
					                                     decoderContext));
		   }
		}
		
		return projectMetadata;
	}
	
	@Override
	public Class<HpcProjectMetadata> getEncoderClass() 
	{
		return HpcProjectMetadata.class;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Decode HpcMetadataItem
     *
     * @param doc The HpcMetadataItem document
     * @param decoderContext
     * @return Decoded HpcMetadataItem object.
     */
    private HpcMetadataItem decodeMetadataItem(Document doc, 
    		                                   DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcMetadataItem.class).decode(docReader, 
		                                                       decoderContext);
	}
}

 