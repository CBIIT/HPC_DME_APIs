/**
 * HpcFilePrimaryMetadataCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;

import java.util.Calendar;
import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC File Primary Metadata Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcFilePrimaryMetadataCodec extends HpcCodec<HpcFilePrimaryMetadata>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcFilePrimaryMetadataCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcFilePrimaryMetadata> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcFilePrimaryMetadata filePrimaryMetadata,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		HpcPIIContent dataContainsPII = filePrimaryMetadata.getDataContainsPII();
		HpcPHIContent dataContainsPHI = filePrimaryMetadata.getDataContainsPHI();
		HpcEncryptionStatus dataEncrypted = filePrimaryMetadata.getDataEncrypted();
		HpcCompressionStatus dataCompressed = filePrimaryMetadata.getDataCompressed();
		String fundingOrganization = filePrimaryMetadata.getFundingOrganization();
		String principalInvestigatorNihUserId = 
				        filePrimaryMetadata.getPrincipalInvestigatorNihUserId();
		String creatorName = filePrimaryMetadata.getCreatorName();
		String registrarNihUserId = 
				        filePrimaryMetadata.getRegistrarNihUserId();
		String labBranch = filePrimaryMetadata.getLabBranch();
		String description = filePrimaryMetadata.getDescription();
		String principalInvestigatorDOC = filePrimaryMetadata.getPrincipalInvestigatorDOC();
		String registrarDOC = filePrimaryMetadata.getRegistrarDOC();
		Calendar originallyCreated = filePrimaryMetadata.getOriginallyCreated();
		List<HpcMetadataItem> metadataItems = filePrimaryMetadata.getMetadataItems();
		
		// Set the data on the BSON document.
		if(dataContainsPII != null) {
		   document.put(FILE_PRIMARY_METADATA_DATA_CONTAINS_PII_KEY, 
				        dataContainsPII.value());
		}
		if(dataContainsPHI != null) {
		   document.put(FILE_PRIMARY_METADATA_DATA_CONTAINS_PHI_KEY, 
				        dataContainsPHI.value());
		}
		if(dataEncrypted != null) {
		   document.put(FILE_PRIMARY_METADATA_DATA_ENCRYPTED_KEY, 
				        dataEncrypted.value());
		}
		if(dataCompressed != null) {
		   document.put(FILE_PRIMARY_METADATA_DATA_COMPRESSED_KEY, 
					    dataCompressed.value());
		}
		if(fundingOrganization != null) {
		   document.put(FILE_PRIMARY_METADATA_FUNDING_ORGANIZATION_KEY, 
					    fundingOrganization);
		}
		if(principalInvestigatorNihUserId != null) {
		   document.put(
		   FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_NIH_USER_ID_KEY, 
		   principalInvestigatorNihUserId);
		}
		if(creatorName != null) {
		   document.put(FILE_PRIMARY_METADATA_CREATOR_NAME_KEY, 
				        creatorName);
		}
		if(registrarNihUserId != null) {
		   document.put(FILE_PRIMARY_METADATA_REGISTRAR_NIH_USER_ID_KEY, 
				        registrarNihUserId);
		}
		if(labBranch != null) {
		   document.put(FILE_PRIMARY_METADATA_LAB_BRANCH_KEY, labBranch);
		}
		if(description != null) {
		   document.put(FILE_PRIMARY_METADATA_DESCRIPTION_KEY, description);
		}
		
		if(principalInvestigatorDOC != null) {
		   document.put(FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_DOC_KEY, 
				        principalInvestigatorDOC);
		}	
		if(registrarDOC != null) {
		   document.put(FILE_PRIMARY_METADATA_REGISTRAR_DOC_KEY, registrarDOC);
		}	
		if(originallyCreated != null) {
		   document.put(FILE_PRIMARY_METADATA_ORIGINALLY_CREATED_KEY, 
				        originallyCreated.getTime());
		}	
		if(metadataItems != null && !metadataItems.isEmpty()) {
		   document.put(FILE_PRIMARY_METADATA_METADATA_ITEMS_KEY, 
				        metadataItems);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	@SuppressWarnings("unchecked")
	public HpcFilePrimaryMetadata decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcFilePrimaryMetadata filePrimaryMetadata = new HpcFilePrimaryMetadata();
		filePrimaryMetadata.setDataContainsPII(HpcPIIContent.valueOf(
				   document.getString(FILE_PRIMARY_METADATA_DATA_CONTAINS_PII_KEY)));
		filePrimaryMetadata.setDataContainsPHI(HpcPHIContent.valueOf(
				   document.getString(FILE_PRIMARY_METADATA_DATA_CONTAINS_PHI_KEY)));
		filePrimaryMetadata.setDataEncrypted(HpcEncryptionStatus.valueOf(
				   document.getString(FILE_PRIMARY_METADATA_DATA_ENCRYPTED_KEY)));
		filePrimaryMetadata.setDataCompressed(HpcCompressionStatus.valueOf(
				   document.getString(FILE_PRIMARY_METADATA_DATA_COMPRESSED_KEY)));
		filePrimaryMetadata.setFundingOrganization(
				   document.getString(FILE_PRIMARY_METADATA_FUNDING_ORGANIZATION_KEY));
		filePrimaryMetadata.setPrincipalInvestigatorNihUserId(
		    document.getString(FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_NIH_USER_ID_KEY));
		filePrimaryMetadata.setCreatorName(
		    document.getString(FILE_PRIMARY_METADATA_CREATOR_NAME_KEY));
		filePrimaryMetadata.setRegistrarNihUserId(
		    document.getString(FILE_PRIMARY_METADATA_REGISTRAR_NIH_USER_ID_KEY));
		filePrimaryMetadata.setLabBranch(
			document.getString(FILE_PRIMARY_METADATA_LAB_BRANCH_KEY));
		filePrimaryMetadata.setDescription(
			document.getString(FILE_PRIMARY_METADATA_DESCRIPTION_KEY));
		
		filePrimaryMetadata.setPrincipalInvestigatorDOC(
			document.getString(FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_DOC_KEY));
		filePrimaryMetadata.setRegistrarDOC(
			document.getString(FILE_PRIMARY_METADATA_REGISTRAR_DOC_KEY));
		if(document.getDate(FILE_PRIMARY_METADATA_ORIGINALLY_CREATED_KEY) != null) {
			Calendar originallyCreated = Calendar.getInstance();
			originallyCreated.setTime(document.getDate(
					  FILE_PRIMARY_METADATA_ORIGINALLY_CREATED_KEY));
			filePrimaryMetadata.setOriginallyCreated(originallyCreated);
		}
		
		// Map the collections.
		List<Document> metadataItemDocuments = 
		    (List<Document>) document.get(FILE_PRIMARY_METADATA_METADATA_ITEMS_KEY);
		if(metadataItemDocuments != null) {
		   for(Document metadataItemDocument : metadataItemDocuments) {
			   filePrimaryMetadata.getMetadataItems().add(
					                  decodeMetadataItem(metadataItemDocument, 	
					                                     decoderContext, getRegistry()));
		   }
		}
		
		return filePrimaryMetadata;
	}
	
	@Override
	public Class<HpcFilePrimaryMetadata> getEncoderClass() 
	{
		return HpcFilePrimaryMetadata.class;
	}
}

 