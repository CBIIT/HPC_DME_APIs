/**
 * HpcDataTransferAccountCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferType;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC File Location Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataTransferAccountCodec extends HpcCodec<HpcDataTransferAccount>
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
    public HpcDataTransferAccountCodec() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcDataTransferAccount> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcDataTransferAccount dataTransferAccount,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		String username = dataTransferAccount.getUsername();
		String password = dataTransferAccount.getPassword();
		HpcDataTransferType dataTransferType = dataTransferAccount.getDataTransferType();

		if(username != null) {
		   document.put(DATA_TRANSFER_ACCOUNT_USERNAME_KEY, username);
		}
		if(password != null) {
		   document.put(DATA_TRANSFER_ACCOUNT_PASSWORD_KEY, password);
		}
		if(dataTransferType != null) {
		   document.put(DATA_TRANSFER_ACCOUNT_DATA_TRANSFER_TYPE_KEY, 
				        dataTransferType.value());
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcDataTransferAccount decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
	             getRegistry().get(Document.class).decode(reader, 
	            		                                  decoderContext);
		
		// Map the document to HpcDataTransferAccount instance.
		HpcDataTransferAccount dataTransferAccount = new HpcDataTransferAccount();
		dataTransferAccount.setUsername(document.get(DATA_TRANSFER_ACCOUNT_USERNAME_KEY, 
				                                     String.class));
		dataTransferAccount.setPassword(document.get(DATA_TRANSFER_ACCOUNT_PASSWORD_KEY, 
                                                     String.class));
		
		dataTransferAccount.setDataTransferType(
		    HpcDataTransferType.valueOf(
		    		       document.get(DATA_TRANSFER_ACCOUNT_DATA_TRANSFER_TYPE_KEY, 
                           String.class)));
		
		return dataTransferAccount;
	}
	
	@Override
	public Class<HpcDataTransferAccount> getEncoderClass() 
	{
		return HpcDataTransferAccount.class;
	}
}

 