/**
 * HpcException.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * The HPC exception.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcUtil {
	// ---------------------------------------------------------------------//
	// constructors
	// ---------------------------------------------------------------------//

	/** Default constructor disabled */
	private HpcUtil() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Normalize a path. 1. It begins with '/' 2. No trailing '/' unless root. 3. No
	 * duplicate '/'.
	 *
	 * @param path The path.
	 * @return The normalized path.
	 */
	public static String toNormalizedPath(String path) {
		// Normalize the path - i.e. remove duplicate and trailing '/'
		String absolutePath = StringUtils.trimTrailingCharacter(path, '/').replaceAll("/+", "/");

		StringBuilder buf = new StringBuilder();
		if (absolutePath.isEmpty() || absolutePath.charAt(0) != '/') {
			buf.append('/');
		}
		buf.append(absolutePath);
		return buf.toString();
	}

	/**
	 * Execute a (shell) command.
	 *
	 * @param command The command to execute.
	 * @return The command's output
	 * @throws HpcException If exec failed.
	 */
	public static String exec(String command) throws HpcException {
		if (StringUtils.isEmpty(command)) {
			throw new HpcException("Null / empty command", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			if (process.waitFor() > 0) {
				String message = null;
				if (process.getErrorStream() != null) {
					message = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
					if (StringUtils.isEmpty(message) && process.getInputStream() != null) {
						message = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
					}
					throw new HpcException("command [" + command + "] exec failed: " + message,
							HpcErrorType.UNEXPECTED_ERROR);
				}
			} else if (process.getInputStream() != null) {
				return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new HpcException("command [" + command + "] exec failed: " + e.getMessage(),
					HpcErrorType.UNEXPECTED_ERROR, e);
			
		} catch (IOException e) {
			throw new HpcException("command [" + command + "] exec failed: " + e.getMessage(),
					HpcErrorType.UNEXPECTED_ERROR, e);
		}
		return null;
	}
}
