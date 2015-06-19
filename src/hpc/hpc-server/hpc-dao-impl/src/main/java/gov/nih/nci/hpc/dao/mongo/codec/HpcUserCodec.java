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

import gov.nih.nci.hpc.domain.user.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.BsonDocumentReader;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		// Extract the data from the POJO.
		String nihUserId = user.getNihUserId();
		String firstName = user.getFirstName();
		String lastName = user.getLastName();
		HpcDataTransferAccount dataTransferAccount = 
				               user.getDataTransferAccount();

		// Set the data on the BSON document.
		if(nihUserId != null) {
		   document.put(USER_NIH_USER_ID_KEY, nihUserId);
		}
		if(firstName != null) {
		   document.put(USER_FIRST_NAME_KEY, firstName);
		}
		if(lastName != null) {
		   document.put(USER_LAST_NAME_KEY, lastName);
		}
		if(dataTransferAccount != null) {
		   document.put(USER_DATA_TRANSFER_ACCOUNT_KEY, dataTransferAccount);
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcUser decode(BsonReader reader, DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = getRegistry().get(Document.class).decode(reader, 
				                                                     decoderContext);
		
		// Map the document to HpcUser instance.
		HpcUser user = new HpcUser();
		user.setNihUserId(document.get(USER_NIH_USER_ID_KEY, String.class));
		user.setFirstName(document.get(USER_FIRST_NAME_KEY, String.class));
		user.setLastName(document.get(USER_LAST_NAME_KEY, String.class));
		user.setDataTransferAccount(decode(document.get(USER_DATA_TRANSFER_ACCOUNT_KEY, 
				                                        Document.class),
				                           decoderContext));
				                
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
     * Decode HpcDataTransferAccount
     *
     * @param doc The HpcDataTransferAccount document.
     * @param decoderContext.
     * @return Decoded HpcDataTransferAcccount object.
     */
    private HpcDataTransferAccount decode(Document doc, DecoderContext decoderContext)
    {
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, 
    				                                  getRegistry()));
		return getRegistry().get(HpcDataTransferAccount.class).decode(docReader, 
		                                                              decoderContext);
	}
}

 