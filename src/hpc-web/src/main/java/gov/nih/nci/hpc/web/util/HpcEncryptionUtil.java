/**
 * HpcEncryptionUtil.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.web.util;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * <p>
 * HPC Encryption Util.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public final class HpcEncryptionUtil {

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//
	private HpcEncryptionUtil() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Encrypt text.
	 * 
	 * @param key
	 *            The key used to encrypt.
	 * @param text
	 *            The text to encrypt.
	 * @return The encrypted text.
	 */
	public static byte[] encrypt(String key, String text) throws Exception {
		try {
			final Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
			final Cipher encryptCipher = Cipher.getInstance("AES");
			encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey);
			return encryptCipher.doFinal(text.getBytes());

		} catch (Exception e) {
			throw new Exception("Failed to encrypt: " + e);
		}
	}

	/**
	 * Decrypt text.
	 * 
	 * @param key
	 *            The key used to decrypt.
	 * @param binary
	 *            The binary to decrypt.
	 * @return The decrypted text.
	 * @throws Exception
	 */
	public static String decrypt(String key, byte[] binary) throws Exception {
		try {
			final Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
			final Cipher decryptCipher = Cipher.getInstance("AES");
			decryptCipher.init(Cipher.DECRYPT_MODE, aesKey);
			return new String(decryptCipher.doFinal(binary));

		} catch (Exception e) {
			throw new Exception("Failed to decrypt: " + e);
		}
	}
}
