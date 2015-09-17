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

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeDataTransferLocations;
import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;

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
 * @version $Id$
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
		String requesterNciUserId = dataTransferRequest.getRequesterNciUserId();
		String fileId = dataTransferRequest.getFileId();
		String dataTransferId = dataTransferRequest.getDataTransferId();
		HpcDataTransferLocations locations = dataTransferRequest.getLocations();
		HpcDataTransferStatus status = dataTransferRequest.getStatus();
		HpcDataTransferReport report = dataTransferRequest.getReport();
 
		// Set the data on the BSON document.
		if(requesterNciUserId != null) {
		   document.put(DATA_TRANSFER_REQUEST_REQUESTER_NCI_USER_ID_KEY, requesterNciUserId);
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
		dataTransferRequest.setRequesterNciUserId(
			document.getString(DATA_TRANSFER_REQUEST_REQUESTER_NCI_USER_ID_KEY));
		dataTransferRequest.setFileId(
			document.getString(DATA_TRANSFER_REQUEST_FILE_ID_KEY));
		dataTransferRequest.setDataTransferId(
			document.getString(DATA_TRANSFER_REQUEST_DATA_TRANSFER_ID_KEY));
		dataTransferRequest.setLocations(
			decodeDataTransferLocations(
		    	  document.get(DATA_TRANSFER_REQUEST_LOCATIONS_KEY, Document.class), 
		    	  decoderContext, getRegistry()));
		String dataTransferStatusStr = document.getString(DATA_TRANSFER_REQUEST_STATUS_KEY);
		dataTransferRequest.setStatus(dataTransferStatusStr != null ?
				                      HpcDataTransferStatus.fromValue(dataTransferStatusStr) :
				                      null);
		dataTransferRequest.setReport(
			decodeDataTransferReport(
			    	  document.get(DATA_TRANSFER_REQUEST_REPORT_KEY, Document.class), 
			    	  decoderContext, getRegistry()));		
		
		return dataTransferRequest;
	}
	
	@Override
	public Class<HpcDataTransferRequest> getEncoderClass() 
	{
		return HpcDataTransferRequest.class;
	}
}

 