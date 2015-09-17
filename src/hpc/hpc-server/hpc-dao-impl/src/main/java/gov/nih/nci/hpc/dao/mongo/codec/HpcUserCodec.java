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

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeDataTransferAccount;
import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeNciAccount;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;

import java.util.Calendar;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

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
		HpcNciAccount nciAccount = user.getNciAccount();
		HpcDataTransferAccount dataTransferAccount = 
				                           user.getDataTransferAccount();
		Calendar created = user.getCreated();
		Calendar lastUpdated = user.getLastUpdated();
		
		
		// Set the data on the BSON document.
		if(nciAccount != null) {
		   document.put(USER_NCI_ACCOUNT_KEY, nciAccount);	
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
		user.setNciAccount(decodeNciAccount(document.get(USER_NCI_ACCOUNT_KEY, 
                                                         Document.class),
                                            decoderContext, getRegistry()));

		user.setDataTransferAccount(decodeDataTransferAccount(
				                    document.get(USER_DATA_TRANSFER_ACCOUNT_KEY, 
                                                 Document.class),
                                    decoderContext, getRegistry()));
		
		if(document.getDate(USER_CREATED_KEY) != null) {
		   Calendar created = Calendar.getInstance();
		   created.setTime(document.getDate(USER_CREATED_KEY));
		   user.setCreated(created);
		}
		
		if(document.getDate(USER_LAST_UPDATED_KEY) != null) {
		   Calendar lastUpdated = Calendar.getInstance();
		   lastUpdated.setTime(document.getDate(USER_LAST_UPDATED_KEY));
		   user.setLastUpdated(lastUpdated);
		}
		
		return user;
	}
	
	@Override
	public Class<HpcUser> getEncoderClass() 
	{
		return HpcUser.class;
	}
}

 