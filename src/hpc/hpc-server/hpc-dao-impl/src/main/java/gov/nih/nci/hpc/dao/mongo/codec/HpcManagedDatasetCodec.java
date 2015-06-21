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
import gov.nih.nci.hpc.domain.dataset.HpcDataset;
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
		HpcDataset dataset = managedDataset.getDataset();
		List<HpcDataTransferRequest> uploadRequests = 
				                           managedDataset.getUploadRequests();
		List<HpcDataTransferRequest> downloadRequests = 
                                             managedDataset.getDownloadRequests();
 
		// Set the data on the BSON document.
		if(dataset != null) {
		   document.put(MANAGED_DATASET_DATASET_KEY, dataset);
		}
		if(uploadRequests != null && uploadRequests.size() > 0) {
		   document.put(MANAGED_DATASET_UPLOAD_REQUESTS_KEY, uploadRequests);
		}
		if(downloadRequests != null && downloadRequests.size() > 0) {
		   document.put(MANAGED_DATASET_DOWNLOAD_REQUESTS_KEY, downloadRequests);
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
		managedDataset.setDataset(decodeDataset(
				                  document.get(MANAGED_DATASET_DATASET_KEY, 
                                               Document.class),
                                  decoderContext));
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
     * Decode HpcDataset
     *
     * @param doc The HpcDataset document
     * @param decoderContext
     * @return Decoded HpcDataset object.
     */
    private HpcDataset decodeDataset(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataset.class).decode(docReader, 
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

 