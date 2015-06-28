/**
 * HpcUserCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcNihAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;

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

/**
 * <p>
 * HPC User Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserCodec extends HpcCodec<HpcUser>
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
    public HpcUserCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcUser> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcUser user,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		HpcNihAccount nihAccount = user.getNihAccount();
		HpcDataTransferAccount dataTransferAccount = 
				                           user.getDataTransferAccount();
		Calendar created = user.getCreated();
		Calendar lastUpdated = user.getLastUpdated();
		
		
		// Set the data on the BSON document.
		if(nihAccount != null) {
		   document.put(USER_NIH_ACCOUNT_KEY, nihAccount);	
		}
		if(dataTransferAccount != null) {
		   document.put(USER_DATA_TRANSFER_ACCOUNT_KEY, dataTransferAccount);	
		}
		if(created != null) {
		   document.put(USER_CREATED_KEY, created.getTime());
		}
		if(lastUpdated != null) {
		   document.put(USER_LAST_UPDATED_KEY, lastUpdated.getTime());
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcUser decode(BsonReader reader, DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcUser user = new HpcUser();
		user.setNihAccount(decodeNihAccount(document.get(USER_NIH_ACCOUNT_KEY, 
                                                         Document.class),
                                            decoderContext));

		user.setDataTransferAccount(decodeDataTransferAccount(
				                    document.get(USER_DATA_TRANSFER_ACCOUNT_KEY, 
                                                 Document.class),
                                    decoderContext));
		
		Calendar created = Calendar.getInstance();
		created.setTime(document.get(USER_CREATED_KEY, Date.class));
		user.setCreated(created);
		
		Calendar lastUpdated = Calendar.getInstance();
		lastUpdated.setTime(document.get(USER_LAST_UPDATED_KEY, Date.class));
		user.setLastUpdated(lastUpdated);
		
		return user;
	}
	
	@Override
	public Class<HpcUser> getEncoderClass() 
	{
		return HpcUser.class;
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Decode HpcNihAccount
     *
     * @param doc The HpcNihAccount document.
     * @param decoderContext.
     * @return Decoded HpcNihAccount object.
     */
    private HpcNihAccount decodeNihAccount(Document doc, 
    		                               DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcNihAccount.class).decode(docReader, 
				                                             decoderContext);
	}
    
    /**
     * Decode HpcDataTransferAccount
     *
     * @param doc The HpcDataTransferAccount document.
     * @param decoderContext.
     * @return Decoded HpcDataTransferAcccount object.
     */
    private HpcDataTransferAccount decodeDataTransferAccount(
    		                       Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataTransferAccount.class).decode(docReader, 
		                                                              decoderContext);
	}
}

 