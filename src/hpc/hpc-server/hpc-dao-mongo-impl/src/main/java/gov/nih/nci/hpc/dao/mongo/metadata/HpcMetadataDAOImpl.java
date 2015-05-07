/**
 * HpcMetadataDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.metadata;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.ServerAddress;
import com.mongodb.async.SingleResultCallback;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

/**
 * <p>
 * HPC Metadata DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataDAOImpl implements HpcMetadataDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Mongo DB name.
    private final static String DB_NAME = "hpc"; 
    
    // Mongo DB name.
    private final static String METADATA_COLLECTION_NAME = "metadata"; 
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	// The mongo client instance.
	private MongoClient mongoClient = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcMetadataDAOImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param mongoHost The Mongo hostname.
     * 
     * @throws HpcException If a MongoClient instance was not provided.
     */
    private HpcMetadataDAOImpl(String mongoHost) throws HpcException
    {
    	if(mongoHost == null) {
    	   throw new HpcException("Null Mongo Host instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    
    	// Creating the list of Mongo hosts.
    	Vector<ServerAddress> mongoHosts= new Vector<ServerAddress>();
    	mongoHosts.add(new ServerAddress(mongoHost));
    	
    	// Get the default CodecRegistry. Need to connect a client temprarily.
    	ClusterSettings clusterSettings = 
    			        ClusterSettings.builder().hosts(mongoHosts).build();
    	MongoClientSettings settings = 
    	     MongoClientSettings.builder().clusterSettings(clusterSettings).build();
    	mongoClient = MongoClients.create(settings);
    	CodecRegistry defaultCodecRegistry = 
    			      mongoClient.getSettings().getCodecRegistry();
    	mongoClient.close();
    	
    	// Instantiate the HPC Codecs.
    	HpcMetadataDTOCodec metadataDTOCodec = 
                   new HpcMetadataDTOCodec(defaultCodecRegistry.get(Document.class));
    	
    	// Instantiate a Codec Registry that includes the default + 
    	// the Hpc codecs.
    	CodecRegistry hpcCodecRegistry = 
    		 CodecRegistries.fromRegistries(
                                 defaultCodecRegistry, 
                                 CodecRegistries.fromCodecs(metadataDTOCodec));
    	
    	// Instantiate a MongoClient with the HPC codecs in its registry.
    	settings = 
       	     MongoClientSettings.builder().clusterSettings(clusterSettings).
       	                                   codecRegistry(hpcCodecRegistry).build();
    	mongoClient = MongoClients.create(settings);
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
    public void createMetadata(HpcMetadataDTO metadataDTO)
    {
		HpcMetadataBsonDocument metadataDocument = 
				                        new HpcMetadataBsonDocument();
		metadataDocument.setMetadataDTO(metadataDTO);
		getMetadataCollection().insertOne(metadataDocument, 
				                          new SingleResultCallback<Void>() {
		    @Override
		    public void onResult(final Void result, final Throwable t) {
		        //TODO - check for exceptions here.
		    }
		});
    }
	
	@Override
	public HpcMetadataDTO getMetadata(String id)
	{

		
		return null;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the metadata Mongo Colelction.
     *
     * @return A The metadata Mongo collection.
     */
    private MongoCollection<HpcMetadataBsonDocument> getMetadataCollection()  
    {
    	MongoDatabase database = mongoClient.getDatabase(DB_NAME); 
    	return database.getCollection(METADATA_COLLECTION_NAME, 
    			                      HpcMetadataBsonDocument.class);
    }  
	

}

 