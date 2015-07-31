/**
 * HpcDataTransferRequestCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;

import org.bson.BsonDocumentReader;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC Data Transfer Request Codec. 
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcDataTransferRequestCodec.java 300 2015-07-07 17:18:19Z narram $
 */

public class HpcDataTransferRequestCodec extends HpcCodec<HpcDataTransferRequest>
{ 

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//


	/**
     * Default Constructor.
     * 
     * 
     */
    private HpcDataTransferRequestCodec()
    {
    }   

    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcDataTransferRequest> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcDataTransferRequest dataTransferRequest,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
		
		// Extract the data from the domain object.
		String requesterNihUserId = dataTransferRequest.getRequesterNihUserId();
		String fileId = dataTransferRequest.getFileId();
		String dataTransferId = dataTransferRequest.getDataTransferId();
		HpcDataTransferLocations locations = dataTransferRequest.getLocations();
		HpcDataTransferStatus status = dataTransferRequest.getStatus();
		HpcDataTransferReport report = dataTransferRequest.getReport();
 
		// Set the data on the BSON document.
		if(requesterNihUserId != null) {
		   document.put(DATA_TRANSFER_REQUEST_REQUESTER_NIH_USER_ID_KEY, requesterNihUserId);
		}
		if(fileId != null) {
		   document.put(DATA_TRANSFER_REQUEST_FILE_ID_KEY, fileId);
		}
		if(dataTransferId != null) {
		   document.put(DATA_TRANSFER_REQUEST_DATA_TRANSFER_ID_KEY, dataTransferId);
		}
		if(locations != null) {
		   document.put(DATA_TRANSFER_REQUEST_LOCATIONS_KEY, locations);
		}		
		if(status != null) {
		   document.put(DATA_TRANSFER_REQUEST_STATUS_KEY, status.value());
		}
		if(report != null) {
		   document.put(DATA_TRANSFER_REQUEST_REPORT_KEY, report);
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcDataTransferRequest decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
	             getRegistry().get(Document.class).decode(reader, 
	            		                                  decoderContext);
		
		// Map the document to HpcDataTransferAccount instance.
		HpcDataTransferRequest dataTransferRequest = new HpcDataTransferRequest();
		dataTransferRequest.setRequesterNihUserId(
			document.getString(DATA_TRANSFER_REQUEST_REQUESTER_NIH_USER_ID_KEY));
		dataTransferRequest.setFileId(
			document.getString(DATA_TRANSFER_REQUEST_FILE_ID_KEY));
		dataTransferRequest.setDataTransferId(
			document.getString(DATA_TRANSFER_REQUEST_DATA_TRANSFER_ID_KEY));
		dataTransferRequest.setLocations(
		    decodeDataTransferLocations(
		    	  document.get(DATA_TRANSFER_REQUEST_LOCATIONS_KEY, Document.class), 
		    	  decoderContext));
		dataTransferRequest.setStatus(
			HpcDataTransferStatus.valueOf(
		           document.getString(DATA_TRANSFER_REQUEST_STATUS_KEY)));
		dataTransferRequest.setReport(
			decodeDataTransferReport(
			    	  document.get(DATA_TRANSFER_REQUEST_REPORT_KEY, Document.class), 
			    	  decoderContext));		
		
		return dataTransferRequest;
	}
	
	@Override
	public Class<HpcDataTransferRequest> getEncoderClass() 
	{
		return HpcDataTransferRequest.class;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Decode HpcDataTransferLocations
     *
     * @param doc The HpcDataTransferLocations document
     * @param decoderContext
     * @return Decoded HpcDataTransferLocations object.
     */
    private HpcDataTransferLocations decodeDataTransferLocations(
    		                                   Document doc, 
    		                                   DecoderContext decoderContext)
    {
    	if(doc == null) {
    	   return null;
    	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataTransferLocations.class).decode(docReader, 
		                                                                decoderContext);
	}	
	
    /**
     * Decode HpcDataTransferReport
     *
     * @param doc The HpcDataTransferReport document
     * @param decoderContext
     * @return Decoded HpcDataTransferReport object.
     */
    private HpcDataTransferReport decodeDataTransferReport(
    		                                Document doc, 
    		                                DecoderContext decoderContext)
    {
    	if(doc == null) {
     	   return null;
     	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataTransferReport.class).decode(docReader, 
		                                                             decoderContext);
	}	
}

 