/**
 * HpcManagedDatasetsDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import gov.nih.nci.hpc.dao.HpcManagedDatasetsDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodecProvider;
import gov.nih.nci.hpc.domain.HpcManagedDatasets;
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
 * HPC Managed Datasets DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetsDAOImpl implements HpcManagedDatasetsDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Mongo DB name.
    private final static String DB_NAME = "hpc"; 
    
    // Mongo DB name.
    private final static String MANAGED_DATASETS_COLLECTION_NAME = 
    		                    "managedDatasets"; 
    
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
    private HpcManagedDatasetsDAOImpl() throws HpcException
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
     * @throws HpcException If a MongoClient instance was not provided.
     */
    private HpcManagedDatasetsDAOImpl(String mongoHost,
    		                          HpcCodecProvider hpcCodecProvider) 
    		                         throws HpcException
    {
    	if(mongoHost == null || hpcCodecProvider == null) {
    	   throw new HpcException("Null Mongo Host or HpcCodecProvider instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    
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
    
    //---------------------------------------------------------------------//
    // HpcManagedDatasetsDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void add(HpcManagedDatasets managedDatasets) throws HpcException
    {
		HpcSingleResultCallback<Void> callback = 
				                      new HpcSingleResultCallback<Void>();
		getManagedDatasetsCollection().insertOne(
				                       managedDatasets, 
				                       callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the managed datasets Mongo collection.
     *
     * @return A The metadata Mongo collection.
     */
    private MongoCollection<HpcManagedDatasets> getManagedDatasetsCollection()  
    {
    	MongoDatabase database = mongoClient.getDatabase(DB_NAME); 
    	return database.getCollection(MANAGED_DATASETS_COLLECTION_NAME, 
    			                      HpcManagedDatasets.class);
    }  
}

 