/**
 * HpcMongoDB.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.driver;

import gov.nih.nci.hpc.dao.mongo.codec.HpcCodecProvider;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.ServerAddress;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;
import java.util.Map;

/**
 * <p>
 * HPC MongoDB. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMongoDB 
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	// The mongo client instance.
	private MongoClient mongoClient = null;
	
	// The mongo DB name instance.
	private String dbName = null;
	
	// Map of MongoDB collection classes to names.
	private Map<Class, String> collections = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcMongoDB() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param mongoHost The Mongo hostname.
     * @param hpcCodecProvider The HPC codec provider instance.
     * 
     * @throws HpcException If a HpcCodecProvider instance was not provided.
     */
    private HpcMongoDB(String dbName, String mongoHost,
    		           HpcCodecProvider hpcCodecProvider,
    		           Map<Class, String> collections) throws HpcException
    {
    	if(dbName == null || mongoHost == null || hpcCodecProvider == null ||
    	   collections == null) {
    	   throw new HpcException("Null Mongo Host/Name/HpcCodecProvider/Colelctions",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    
    	this.dbName = dbName;
    	this.collections = collections;
    	
    	// Creating the list of Mongo hosts.
    	Vector<ServerAddress> mongoHosts = new Vector<ServerAddress>();
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
    	
    	// Instantiate a Codec Registry that includes the default + 
    	// the Hpc codec provider.
    	CodecRegistry hpcCodecRegistry = 
    		 CodecRegistries.fromRegistries(
    				         CodecRegistries.fromProviders(hpcCodecProvider),
                             defaultCodecRegistry);
    	
    	// Instantiate a MongoClient with the HPC codecs in its registry.
    	settings = 
       	     MongoClientSettings.builder().clusterSettings(clusterSettings).
       	                         codecRegistry(hpcCodecRegistry).build();
    	mongoClient = MongoClients.create(settings);
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get the managed data Mongo collection.
     *
     * @return A The managed data Mongo collection.
     * @throws HpcException If colelction is not found.
     */
    public <T> MongoCollection<T> getCollection(final Class<T> clazz) 
                                               throws HpcException
    {
    	MongoDatabase database = mongoClient.getDatabase(dbName); 
    	String collection = collections.get(clazz);
    	if(collection == null) {
    	   throw new HpcException("Collection not found: " + clazz,
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	return database.getCollection(collection, clazz);
    }
                
}

 