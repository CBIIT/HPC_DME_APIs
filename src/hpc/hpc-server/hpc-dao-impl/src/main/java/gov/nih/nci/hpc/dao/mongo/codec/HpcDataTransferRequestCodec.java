/**
 * HpcDataTransferAccountCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import java.util.List;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccountType;
import gov.nih.nci.hpc.exception.HpcException;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Binary;

/**
 * <p>
 * HPC File Location Codec. 
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
						HpcDataTransferRequest hpcDataTransferRequest,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
		// Extract the data from the domain object.
		String id = hpcDataTransferRequest.getReport().getTaskID();
		HpcDataTransferReport hpcDataTransferReport = hpcDataTransferRequest.getReport();

 
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(TRANSFER_STATUS_REQUEST_KEY, id);
		}
//		if(hpcDataTransferRequest != null) {
//		   document.put(TRANSFER_STATUS_REQUEST_STATUS, hpcDataTransferRequest.getStatus());
//		}
		if(hpcDataTransferReport != null) {
		   document.put(TRANSFER_STATUS_REQUEST, hpcDataTransferReport);
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
		HpcDataTransferRequest hpcDataTransferRequest = new HpcDataTransferRequest();

		
		return hpcDataTransferRequest;
	}
	
	@Override
	public Class<HpcDataTransferRequest> getEncoderClass() 
	{
		return HpcDataTransferRequest.class;
	}
}

 