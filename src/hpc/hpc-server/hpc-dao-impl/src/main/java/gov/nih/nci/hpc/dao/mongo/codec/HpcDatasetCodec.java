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

import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.BsonDocumentReader;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>
 * HPC Dataset Codec. 
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
		HpcFileSet fileSet = dataset.getFileSet();
		List<HpcDataTransferRequest> uploadRequests = 
				                           dataset.getUploadRequests();
		List<HpcDataTransferRequest> downloadRequests = 
				                             dataset.getDownloadRequests();
 
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(DATASET_ID_KEY, id);
		}
		if(fileSet != null) {
		   document.put(DATASET_FILE_SET_KEY, fileSet);
		}
		if(uploadRequests != null && uploadRequests.size() > 0) {
		   document.put(DATASET_UPLOAD_REQUESTS_KEY, uploadRequests);
		}
		if(downloadRequests != null && downloadRequests.size() > 0) {
		   document.put(DATASET_DOWNLOAD_REQUESTS_KEY, downloadRequests);
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
		
		// Map the attributes
		HpcDataset dataset = new HpcDataset();
		dataset.setId(document.get(DATASET_ID_KEY, String.class));
		dataset.setFileSet(decodeFileSet(document.get(DATASET_FILE_SET_KEY, 
                                                      Document.class),
                                         decoderContext));
		List<Document> uploadRequestDocuments = 
			(List<Document>) document.get(DATASET_UPLOAD_REQUESTS_KEY);
		if(uploadRequestDocuments != null) {
		   for(Document uploadRequestDocument : uploadRequestDocuments) {
		       dataset.getUploadRequests().add(
		    		      decodeDataTransferRequest(uploadRequestDocument, 
		    				                        decoderContext));
		   }
		}
		List<Document> downloadRequestDocuments = 
				(List<Document>) document.get(DATASET_DOWNLOAD_REQUESTS_KEY);
			if(downloadRequestDocuments != null) {
			   for(Document downloadRequestDocument : downloadRequestDocuments) {
			       dataset.getDownloadRequests().add(
			    		      decodeDataTransferRequest(downloadRequestDocument, 
			    			     	                    decoderContext));
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
     * Decode HpcFileSet
     *
     * @param doc The HpcFile document
     * @param decoderContext
     * @return Decoded HpcFileSet object.
     */
    private HpcFileSet decodeFileSet(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcFileSet.class).decode(docReader, 
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

 