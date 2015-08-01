/**
 * HpcNihAccountCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.user.HpcNihAccount;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC NIH Account Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNihAccountCodec extends HpcCodec<HpcNihAccount>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcNihAccountCodec() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcNihAccount> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcNihAccount nihAccount,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		String userId = nihAccount.getUserId();
		String firstName = nihAccount.getFirstName();
		String lastName = nihAccount.getLastName();

		// Set the data on the BSON document.
		if(userId != null) {
		   document.put(NIH_ACCOUNT_USER_ID_KEY, userId);
		}
		if(firstName != null) {
		   document.put(NIH_ACCOUNT_FIRST_NAME_KEY, firstName);
		}
		if(lastName != null) {
		   document.put(NIH_ACCOUNT_LAST_NAME_KEY, lastName);
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcNihAccount decode(BsonReader reader, 
			                    DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = getRegistry().get(Document.class).decode(reader, 
				                                                     decoderContext);
		
		// Map the document to HpcNihAccount instance.
		HpcNihAccount nihAccount = new HpcNihAccount();
		nihAccount.setUserId(document.getString(NIH_ACCOUNT_USER_ID_KEY));
		nihAccount.setFirstName(document.getString(NIH_ACCOUNT_FIRST_NAME_KEY));
		nihAccount.setLastName(document.getString(NIH_ACCOUNT_LAST_NAME_KEY));

		return nihAccount;
	}
	
	@Override
	public Class<HpcNihAccount> getEncoderClass() 
	{
		return HpcNihAccount.class;
	}
}

 