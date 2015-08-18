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

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeDataTransferRequest;
import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeFileSet;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.model.HpcDataset;

import java.util.Calendar;
import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

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
		Calendar created = dataset.getCreated();
		Calendar lastUpdated = dataset.getLastUpdated();
 
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
		if(created != null) {
		   document.put(DATASET_CREATED_KEY, created.getTime());
		}
		if(lastUpdated != null) {
		   document.put(DATASET_LAST_UPDATED_KEY, lastUpdated.getTime());
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	@SuppressWarnings("unchecked")
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
		dataset.setId(document.getString(DATASET_ID_KEY));
		dataset.setFileSet(decodeFileSet(document.get(DATASET_FILE_SET_KEY, 
                                                      Document.class),
                                         decoderContext,
                                         getRegistry()));
		List<Document> uploadRequestDocuments = 
			(List<Document>) document.get(DATASET_UPLOAD_REQUESTS_KEY);
		if(uploadRequestDocuments != null) {
		   for(Document uploadRequestDocument : uploadRequestDocuments) {
		       dataset.getUploadRequests().add(
		    		      decodeDataTransferRequest(uploadRequestDocument, 
		    				                        decoderContext,
		    				                        getRegistry()));
		   }
		}
		List<Document> downloadRequestDocuments = 
				(List<Document>) document.get(DATASET_DOWNLOAD_REQUESTS_KEY);
			if(downloadRequestDocuments != null) {
			   for(Document downloadRequestDocument : downloadRequestDocuments) {
			       dataset.getDownloadRequests().add(
			    		      decodeDataTransferRequest(downloadRequestDocument, 
			    			     	                    decoderContext,
			    			     	                    getRegistry()));
			   }
		}
		
		Calendar created = Calendar.getInstance();
		created.setTime(document.getDate(DATASET_CREATED_KEY));
		dataset.setCreated(created);
		
		Calendar lastUpdated = Calendar.getInstance();
		lastUpdated.setTime(document.getDate(DATASET_LAST_UPDATED_KEY));
		dataset.setLastUpdated(lastUpdated);
		
		return dataset;
	}
	
	@Override
	public Class<HpcDataset> getEncoderClass() 
	{
		return HpcDataset.class;
	}
}

 