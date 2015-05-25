/**
 * HpcManagedDataCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.domain.HpcManagedDataType;
import gov.nih.nci.hpc.domain.HpcManagedData;

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
 * HPC Managed Data Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDataCodec extends HpcCodec<HpcManagedData>
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
    public HpcManagedDataCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcManagedData> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcManagedData managedData,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		String id = managedData.getId();
		HpcManagedDataType type = managedData.getType();
		Calendar created = managedData.getCreated();
		List<HpcDataset> datasets = managedData.getDatasets();
 
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(MANAGED_DATA_ID_KEY, id);
		}
		if(type != null) {
		   document.put(MANAGED_DATA_TYPE_KEY, type.value());
		}
		if(created != null) {
		   document.put(MANAGED_DATA_CREATED_KEY, created.getTime());
		}
		if(datasets != null && datasets.size() > 0) {
		   document.put(MANAGED_DATA_DATASETS_KEY, datasets);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcManagedData decode(BsonReader reader, 
			                     DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcManagedData managedData = new HpcManagedData();
		managedData.setId(document.get(MANAGED_DATA_ID_KEY, String.class));
		managedData.setType( 
		       HpcManagedDataType.fromValue(document.get(MANAGED_DATA_TYPE_KEY, 
		    		                                     String.class)));
		Calendar created = Calendar.getInstance();
		created.setTime(document.get(MANAGED_DATA_CREATED_KEY, Date.class));
		managedData.setCreated(created);
		List<Document> datasetDocuments = 
				       (List<Document>) document.get(MANAGED_DATA_DATASETS_KEY);
		if(datasetDocuments != null) {
		   for(Document datasetDocument : datasetDocuments) {
			   managedData.getDatasets().add(decode(datasetDocument, 
					                                decoderContext));
		   }
		}
		
		return managedData;
	}
	
	@Override
	public Class<HpcManagedData> getEncoderClass() 
	{
		return HpcManagedData.class;
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
    private HpcDataset decode(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataset.class).decode(docReader, 
		                                                  decoderContext);
	}
}

 