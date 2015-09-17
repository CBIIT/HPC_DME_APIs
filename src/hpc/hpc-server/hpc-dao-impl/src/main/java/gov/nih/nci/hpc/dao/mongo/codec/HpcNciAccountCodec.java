/**
 * HpcNciAccountCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.user.HpcNciAccount;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC NCI Account Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNciAccountCodec extends HpcCodec<HpcNciAccount>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcNciAccountCodec() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcNciAccount> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcNciAccount nciAccount,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		String userId = nciAccount.getUserId();
		String firstName = nciAccount.getFirstName();
		String lastName = nciAccount.getLastName();

		// Set the data on the BSON document.
		if(userId != null) {
		   document.put(NCI_ACCOUNT_USER_ID_KEY, userId);
		}
		if(firstName != null) {
		   document.put(NCI_ACCOUNT_FIRST_NAME_KEY, firstName);
		}
		if(lastName != null) {
		   document.put(NCI_ACCOUNT_LAST_NAME_KEY, lastName);
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcNciAccount decode(BsonReader reader, 
			                    DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = getRegistry().get(Document.class).decode(reader, 
				                                                     decoderContext);
		
		// Map the document to HpcNciAccount instance.
		HpcNciAccount nciAccount = new HpcNciAccount();
		nciAccount.setUserId(document.getString(NCI_ACCOUNT_USER_ID_KEY));
		nciAccount.setFirstName(document.getString(NCI_ACCOUNT_FIRST_NAME_KEY));
		nciAccount.setLastName(document.getString(NCI_ACCOUNT_LAST_NAME_KEY));

		return nciAccount;
	}
	
	@Override
	public Class<HpcNciAccount> getEncoderClass() 
	{
		return HpcNciAccount.class;
	}
}

 