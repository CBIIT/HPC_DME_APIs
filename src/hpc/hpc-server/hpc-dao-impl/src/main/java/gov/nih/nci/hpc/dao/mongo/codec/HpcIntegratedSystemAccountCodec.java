/**
 * HpcIntegratedSystemAccountCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
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

public class HpcIntegratedSystemAccountCodec extends HpcCodec<HpcIntegratedSystemAccount>
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
    private HpcIntegratedSystemAccountCodec() throws HpcException
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
    private HpcIntegratedSystemAccountCodec(HpcEncryptor encryptor) throws HpcException
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
    // Codec<HpcIntegratedSystemAccount> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
			           HpcIntegratedSystemAccount integratedSystemAccount,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		String username = integratedSystemAccount.getUsername();
		String password = integratedSystemAccount.getPassword();
		HpcIntegratedSystem integratedSystem = integratedSystemAccount.getIntegratedSystem();

		if(username != null) {
		   document.put(INTEGRATED_SYSTEM_ACCOUNT_USERNAME_KEY, 
				        encryptor.encrypt(username));
		}
		if(password != null) {
		   document.put(INTEGRATED_SYSTEM_ACCOUNT_PASSWORD_KEY, 
				        encryptor.encrypt(password));
		}
		if(integratedSystem != null) {
		   document.put(INTEGRATED_SYSTEM_ACCOUNT_INTEGRATED_SYSTEM_KEY, 
				        integratedSystem.value());
		}
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcIntegratedSystemAccount decode(BsonReader reader, 
			                                 DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
	             getRegistry().get(Document.class).decode(reader, 
	            		                                  decoderContext);
		
		// Map the document to HpcDataTransferAccount instance.
		HpcIntegratedSystemAccount integratedSystemAccount = new HpcIntegratedSystemAccount();
		integratedSystemAccount.setUsername(
			encryptor.decrypt(document.get(INTEGRATED_SYSTEM_ACCOUNT_USERNAME_KEY, 
				                           Binary.class)));
		integratedSystemAccount.setPassword(
			encryptor.decrypt(document.get(INTEGRATED_SYSTEM_ACCOUNT_PASSWORD_KEY, 
                                           Binary.class)));
		integratedSystemAccount.setIntegratedSystem(
		          HpcIntegratedSystem.fromValue(
		    	     document.getString(INTEGRATED_SYSTEM_ACCOUNT_INTEGRATED_SYSTEM_KEY)));
		
		return integratedSystemAccount;
	}
	
	@Override
	public Class<HpcIntegratedSystemAccount> getEncoderClass() 
	{
		return HpcIntegratedSystemAccount.class;
	}
}

 