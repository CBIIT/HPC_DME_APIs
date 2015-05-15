/**
 * HpcManagedDatasetsBsonCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.dto.types.HpcDatasetLocation;
import gov.nih.nci.hpc.dto.types.HpcDatasetType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * <p>
 * HPC Managed Datasets BSON Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetsBsonCodec 
             implements CollectibleCodec<HpcManagedDatasetsBson>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // BSON Document keys.
    private final static String MONGO_ID_KEY = "_id";
    private final static String DATASETS_KEY = "datasets"; 
    private final static String ID_KEY = "id";
    private final static String LOCATION_KEY = "location"; 
    private final static String NAME_KEY = "name"; 
    private final static String TYPE_KEY = "type"; 
    private final static String SIZE_KEY = "size"; 
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	// The Document codec.
	private Codec<Document> documentCodec;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcManagedDatasetsBsonCodec() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor w/ Document codec
     * 
     * @param documentCodec Document Codec.
     * 
     * @throws HpcException If documentCodec is null.
     */
    public HpcManagedDatasetsBsonCodec(Codec<Document> documentCodec) 
                                      throws HpcException
    {
    	if(documentCodec == null) {
    	   throw new HpcException("Null Document Codec", 
    			                  HpcErrorType.INVALID_INPUT);
    	}
    	
    	this.documentCodec = documentCodec;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // CollectibleCodec<HpcManagedDatasetsBson> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
					   HpcManagedDatasetsBson managedDatasets,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the BSON POJO.
		ObjectId objectId = managedDatasets.getObjectId();
		List<HpcDataset> datasets = 
				         managedDatasets.getManagedDatasets().getDatasets();
 
		// Set the data on the BSON document.
		if(objectId != null) {
		   document.put(MONGO_ID_KEY, objectId);
		}
		if(datasets != null && datasets.size() > 0) {
		   document.put(DATASETS_KEY, toDocumentsList(datasets));
		}

		documentCodec.encode(writer, document, encoderContext);
	}
 
	@Override
	public HpcManagedDatasetsBson decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = documentCodec.decode(reader, decoderContext);
		
		// Map the BSON Document to a BSON POJO.
		HpcManagedDatasetsBson managedDatasets = new HpcManagedDatasetsBson();
		managedDatasets.setObjectId(document.getObjectId(MONGO_ID_KEY));
		List<HpcDataset> datasets = 
				         (List<HpcDataset>) document.get(DATASETS_KEY);
		if(datasets != null) {
		   for(HpcDataset dataset : datasets) {
			   managedDatasets.getManagedDatasets().getDatasets().add(dataset);
		   }
		}
		
		return managedDatasets;
	}
	
	@Override
	public Class<HpcManagedDatasetsBson> getEncoderClass() 
	{
		return HpcManagedDatasetsBson.class;
	}
 
	@Override
	public HpcManagedDatasetsBson generateIdIfAbsentFromDocument(
			                      HpcManagedDatasetsBson managedDatasets) 
	{
		return documentHasId(managedDatasets) ? 
				             managedDatasets : new HpcManagedDatasetsBson();
	}
 
	@Override
	public boolean documentHasId(HpcManagedDatasetsBson managedDatasets) 
	{
		return managedDatasets.getObjectId() != null;
	}
 
	@Override
	public BsonValue getDocumentId(HpcManagedDatasetsBson managedDatasets) 
	{
	    return new BsonString(managedDatasets.getObjectId().toHexString());
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Encode List<HpcDataset>
     * 
     * @param datasets The list to encode
     * @return The encoded list.
     */
    private List<Document> toDocumentsList(List<HpcDataset> datasets)
    {
    	List<Document> documents = new ArrayList<Document>();
    	for(HpcDataset dataset : datasets) {
    		documents.add(toDocument(dataset));
    	}
    	
    	return documents;
    }
    
    /**
     * Encode HpcDataset.
     * 
     * @param dataset The dataset to encode.
     * @return The encoded dataset document.
     */
    private Document toDocument(HpcDataset dataset) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		HpcDatasetLocation location = dataset.getLocation();
		String id = dataset.getId();
		String name = dataset.getName();
		HpcDatasetType type = dataset.getType();
		Double size = dataset.getSize();

		// Set the data on the BSON document.
		if(location != null) {
		document.put(LOCATION_KEY, location);
		}
		if(id != null) {
		document.put(ID_KEY, id);
		}
		if(name != null) {
		document.put(NAME_KEY, name);
		}
		if(type != null) {
		document.put(TYPE_KEY, type);
		}
		if(size != null) {
		document.put(SIZE_KEY, size);
		}
		
		return document;
	}  
    
    /**
     * Decode HpcDataset.
     * 
     * @param document The document to decode.
     * @return The decoded dataset POJO.
     */
	public HpcDataset toHpcDataset(Document document)
	{
		HpcDataset dataset = new HpcDataset();
		dataset.setLocation(document.get(LOCATION_KEY, HpcDatasetLocation.class));
		dataset.setId(document.get(ID_KEY, String.class));
		dataset.setName(document.get(NAME_KEY, String.class));
		dataset.setType(document.get(TYPE_KEY, HpcDatasetType.class));
		dataset.setSize(document.get(SIZE_KEY, Double.class));
		
		return dataset;
	}
}

 