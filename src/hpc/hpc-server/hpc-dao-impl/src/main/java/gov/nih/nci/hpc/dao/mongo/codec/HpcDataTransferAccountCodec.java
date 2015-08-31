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

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccountType;
import gov.nih.nci.hpc.exception.HpcException;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Binary;

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
	
	// Encryptor.
	HpcEncryptor encryptor = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDataTransferAccountCodec() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param encryptor An encryptor instance.
     * 
     * @throws HpcException If an encryptor instance was not provided.
     */
    private HpcDataTransferAccountCodec(HpcEncryptor encryptor) throws HpcException
    {
    	if(encryptor == null) {
    	   throw new HpcException("Null Encryptor instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.encryptor = encryptor;
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
		HpcDataTransferAccountType accountType = dataTransferAccount.getAccountType();

		if(username != null) {
		   document.put(DATA_TRANSFER_ACCOUNT_USERNAME_KEY, 
				        encryptor.encrypt(username));
		}
		if(password != null) {
		   document.put(DATA_TRANSFER_ACCOUNT_PASSWORD_KEY, 
				        encryptor.encrypt(password));
		}
		if(accountType != null) {
		   document.put(DATA_TRANSFER_ACCOUNT_ACCOUNT_TYPE_KEY, 
				        accountType.value());
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
		dataTransferAccount.setUsername(
			encryptor.decrypt(document.get(DATA_TRANSFER_ACCOUNT_USERNAME_KEY, 
				                           Binary.class)));
		dataTransferAccount.setPassword(
			encryptor.decrypt(document.get(DATA_TRANSFER_ACCOUNT_PASSWORD_KEY, 
                                           Binary.class)));
		dataTransferAccount.setAccountType(
		    HpcDataTransferAccountType.fromValue(
		    	   document.getString(DATA_TRANSFER_ACCOUNT_ACCOUNT_TYPE_KEY)));
		
		return dataTransferAccount;
	}
	
	@Override
	public Class<HpcDataTransferAccount> getEncoderClass() 
	{
		return HpcDataTransferAccount.class;
	}
}

 