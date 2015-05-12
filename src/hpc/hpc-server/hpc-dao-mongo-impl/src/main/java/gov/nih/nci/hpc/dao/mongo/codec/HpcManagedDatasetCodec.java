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

/**
 * <p>
 * HPC Metadata DTO Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetCodec 
             implements CollectibleCodec<HpcManagedDatasetBsonDocument>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Mongo DB name.
    //private final static String DB_NAME = "hpc"; 
    
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
    private HpcManagedDatasetCodec() throws HpcException
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
    public HpcManagedDatasetCodec(Codec<Document> documentCodec) 
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
    // CollectibleCodec<HpcManagedDatasetBsonDocument> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
					   HpcManagedDatasetBsonDocument metadataDocument,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// TODO 
		/*
		// Extract the data from the DTO.
		ObjectId objectId = metadataDocument.getObjectId();
		Double size = metadataDocument.getDTO().getSize();
		String userId = metadataDocument.getDTO().getUserId();
 
		// Set the data on the BSON document.
		if(objectId != null) {
		   document.put("_id", objectId);
		}
 
		if(size != null) {
		   document.put("size", size);
		}
 
		if(userId != null) {
		   document.put("user_id", userId);
		}
		 */
		documentCodec.encode(writer, document, encoderContext);
 
	}
 
	@Override
	public Class<HpcManagedDatasetBsonDocument> getEncoderClass() 
	{
		return HpcManagedDatasetBsonDocument.class;
	}
 
	@Override
	public HpcManagedDatasetBsonDocument decode(BsonReader reader, 
			                                    DecoderContext decoderContext) 
	{
		Document document = documentCodec.decode(reader, decoderContext);
		
		HpcManagedDatasetBsonDocument metadataDocument = new HpcManagedDatasetBsonDocument();
 
		metadataDocument.setObjectId(document.getObjectId("_id"));
 /* TODO
		metadataDocument.getDTO().setUserId(document.getString("user_id"));
 
		metadataDocument.getDTO().setSize(document.getDouble("size"));
		*/
		return metadataDocument;
	}
 
	@Override
	public HpcManagedDatasetBsonDocument generateIdIfAbsentFromDocument(
			                    HpcManagedDatasetBsonDocument metadataDocument) 
	{
		return documentHasId(metadataDocument) ? metadataDocument : 
			                                     new HpcManagedDatasetBsonDocument();
	}
 
	@Override
	public boolean documentHasId(HpcManagedDatasetBsonDocument metadataDocument) 
	{
		return metadataDocument.getObjectId() != null;
	}
 
	@Override
	public BsonValue getDocumentId(HpcManagedDatasetBsonDocument metadataDocument) 
	{
	    return new BsonString(metadataDocument.getObjectId().toHexString());
	}
}

 