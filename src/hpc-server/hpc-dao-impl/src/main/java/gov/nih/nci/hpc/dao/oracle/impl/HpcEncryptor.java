/**
 * HpcEncryptor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Encryptor. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcEncryptor
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Ciphers.
	private Cipher encryptCipher = null;
	private Cipher decryptCipher = null;
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcEncryptor() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param key The encryption key.
     * @throws HpcException If an encryption key was not provided.
     */
    private HpcEncryptor(String key) throws HpcException
    {
    	if(key == null) {
    	   throw new HpcException("Null Key",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
    	try {
    		 encryptCipher = Cipher.getInstance("AES");
    		 encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey);
    		 
    		 decryptCipher = Cipher.getInstance("AES");
    		 decryptCipher.init(Cipher.DECRYPT_MODE, aesKey);
    		 
    	} catch(Exception e) {
    		    throw new HpcException("Failed to instantiate an AES Cipher", 
    		    	                   HpcErrorType.UNEXPECTED_ERROR, e);
    	} 
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Encrypt text.
     * 
     * @param text The text to encrypt.
     * @return The encrypted text.
     */
	public byte[] encrypt(String text)
	{
		try {
		     return encryptCipher.doFinal(text.getBytes());
		     
		} catch(Exception e) {
			    logger.error("Failed to encrypt: " + e);
		}
		
		return null;    
	}
 
    /**
     * Decrypt text.
     * 
     * @param binary The binary to decrypt.
     * @return The decrypted text.
     */
	public String decrypt(byte[] binary) 
	{
		try {
		     return new String(decryptCipher.doFinal(binary));
		     
		} catch(Exception e) {
			    logger.error("Failed to decrypt: " + e);
		}
		
		return null; 
	}
}

 