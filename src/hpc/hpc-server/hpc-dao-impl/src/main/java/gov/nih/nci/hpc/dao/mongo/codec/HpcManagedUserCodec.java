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

import gov.nih.nci.hpc.domain.model.HpcManagedUser;
import gov.nih.nci.hpc.domain.user.HpcUser;

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

/**
 * <p>
 * HPC Managed User Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedUserCodec extends HpcCodec<HpcManagedUser>
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
    public HpcManagedUserCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcManagedUser> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcManagedUser managedUser,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		String id = managedUser.getId();
		Calendar created = managedUser.getCreated();
		Calendar lastUpdated = managedUser.getLastUpdated();
		HpcUser user = managedUser.getUser();
		
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(MANAGED_USER_ID_KEY, id);
		}
		if(created != null) {
		   document.put(MANAGED_USER_CREATED_KEY, created.getTime());
		}
		if(lastUpdated != null) {
		   document.put(MANAGED_USER_LAST_UPDATED_KEY, lastUpdated.getTime());
		}
		if(user != null) {
		   document.put(MANAGED_USER_USER_KEY, user);	
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcManagedUser decode(BsonReader reader, 
           	                     DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcManagedUser managedUser = new HpcManagedUser();
		managedUser.setId(document.get(MANAGED_USER_ID_KEY, String.class));
		
		Calendar created = Calendar.getInstance();
		created.setTime(document.get(MANAGED_USER_CREATED_KEY, Date.class));
		managedUser.setCreated(created);
		
		Calendar lastUpdated = Calendar.getInstance();
		lastUpdated.setTime(document.get(MANAGED_USER_LAST_UPDATED_KEY, 
				                         Date.class));
		managedUser.setLastUpdated(lastUpdated);
		managedUser.setUser(decode(document.get(MANAGED_USER_USER_KEY, 
                                                Document.class),
                                   decoderContext));
		
		return managedUser;
	}
	
	@Override
	public Class<HpcManagedUser> getEncoderClass() 
	{
		return HpcManagedUser.class;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Decode HpcUser
     *
     * @param doc The HpcUser document.
     * @param decoderContext.
     * @return Decoded HpcUser object.
     */
    private HpcUser decode(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcUser.class).decode(docReader, decoderContext);
	}
}

 