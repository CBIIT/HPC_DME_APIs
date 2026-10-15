/**
 * HpcException.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathPermissions;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * The HPC exception.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcUtil {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Group name space encoding.
	private static final String GROUP_NAME_SPACE_CODE = "_SPC_";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcUtil.class.getName());

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
		String absolutePath = org.springframework.util.StringUtils.trimTrailingCharacter(path, '/').replaceAll("/+",
				"/");

		StringBuilder buf = new StringBuilder();
		if (absolutePath.isEmpty() || absolutePath.charAt(0) != '/') {
			buf.append('/');
		}
		buf.append(absolutePath);
		return buf.toString();
	}

	/**
	 * Execute an external command. The command is executed directly (not through a
	 * shell), so each argument is passed to the program as is.
	 *
	 * @param command          The command to execute - the program followed by its
	 *                         arguments.
	 * @param sudoPassword     (Optional) if provided, the command will be executed
	 *                         w/ 'sudo' using the provided password (written to
	 *                         sudo's standard input).
	 * @param environment      (Optional) environment variables. If provided, the
	 *                         command is executed with only these variables set.
	 * @param workingDirectory (Optional) The working directory to execute the
	 *                         command.
	 * @return The command's output
	 * @throws HpcException If exec failed.
	 */
	public static String exec(List<String> command, String sudoPassword, Map<String, String> environment,
			File workingDirectory) throws HpcException {
		if (command == null || command.isEmpty() || command.stream().anyMatch(StringUtils::isEmpty)) {
			throw new HpcException("Null / empty command", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		String commandString = String.join(" ", command);
		boolean sudo = !StringUtils.isEmpty(sudoPassword);

		List<String> execCommand = new ArrayList<>();
		if (sudo) {
			execCommand.addAll(Arrays.asList("sudo", "-S", "-p", "", "--"));
		}
		execCommand.addAll(command);

		ProcessBuilder processBuilder = new ProcessBuilder(execCommand);
		if (environment != null) {
			processBuilder.environment().clear();
			processBuilder.environment().putAll(environment);
		}
		if (workingDirectory != null) {
			processBuilder.directory(workingDirectory);
		}

		try {
			Process process = processBuilder.start();

			// Read the error output concurrently, so the command doesn't block on a full
			// pipe buffer.
			FutureTask<String> errorOutputTask = new FutureTask<>(
					() -> IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
			Thread errorOutputReader = new Thread(errorOutputTask, "hpc-exec-stderr");
			errorOutputReader.setDaemon(true);
			errorOutputReader.start();

			try (OutputStream processInput = process.getOutputStream()) {
				if (sudo) {
					processInput.write((sudoPassword + "\n").getBytes(StandardCharsets.UTF_8));
				}
			} catch (IOException e) {
				// The process may exit before reading its input. The exit code and error
				// output determine the result.
				logger.debug("command [{}] - failed to write process input: {}", commandString, e.getMessage());
			}

			String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
			int exitCode = process.waitFor();
			String errorOutput = errorOutputTask.get();

			if (exitCode != 0) {
				String message = StringUtils.isEmpty(errorOutput) ? output : errorOutput;
				logger.error("command [{}] exec error: {}", commandString, message);
				throw new HpcException(message, HpcErrorType.UNEXPECTED_ERROR);
			}

			return output;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new HpcException("command [" + commandString + "] exec failed: " + e.getMessage(),
					HpcErrorType.UNEXPECTED_ERROR, e);

		} catch (IOException | ExecutionException e) {
			throw new HpcException("command [" + commandString + "] exec failed: " + e.getMessage(),
					HpcErrorType.UNEXPECTED_ERROR, e);
		}
	}

	/**
	 * iRODS not allowing spaces in group names. Encode group name by replacing
	 * spaces with a sequence of characters representing 'space.
	 *
	 * @param groupName The group name to encode
	 * @return The encoded group name
	 */
	public static String encodeGroupName(String groupName) {
		return groupName.replace(" ", GROUP_NAME_SPACE_CODE);
	}

	/**
	 * Decode group name.
	 *
	 * @param groupName The group name to encode
	 * @return The encoded group name
	 */
	public static String decodeGroupName(String groupName) {
		return groupName.replace(GROUP_NAME_SPACE_CODE, " ");
	}

	/**
	 * Map a list of paths to a comma separated string
	 * 
	 * @param paths A list of paths.
	 * @return comma separated string.
	 */
	public static String toPathsString(List<String> paths) {
		StringBuilder pathsStr = new StringBuilder();
		paths.forEach(path -> pathsStr.append(path + ","));
		return pathsStr.toString();
	}

	/**
	 * Map a comma separated string of paths to a list
	 * 
	 * @param pathsStr A comma separated string of paths.
	 * @return list of paths.
	 */
	public static List<String> fromPathsString(String pathsStr) {
		List<String> paths = new ArrayList<>();
		if (!StringUtils.isEmpty(pathsStr)) {
			for (String path : pathsStr.split(",")) {
				paths.add(path);
			}
		}
		return paths;
	}

	/*
	 * Enum fromValue implementation that doesn't throw an exception if value
	 * provided not found.
	 *
	 * @param enumType The enum class
	 * 
	 * @param name The value string to convert
	 * 
	 * @return The Enum value of the name
	 */
	public static <T extends Enum<T>> T fromValue(Class<T> enumType, String name) {
		if (enumType == null || StringUtils.isEmpty(name)) {
			return null;
		}

		try {
			return T.valueOf(enumType, name);

		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Performs Math.toIntExact w/o throwing overflow exception, but rather
	 * returning max int
	 *
	 * @param value the long value
	 * @return the value of the long value
	 */
	public static int toIntExact(long value) {
		try {
			return Math.toIntExact(value);

		} catch (ArithmeticException e) {
			return Integer.MAX_VALUE;
		}
	}

	private static final String[] SI_UNITS = { "B", "KB", "MB", "GB", "TB", "PB", "EB" };
	private static final String[] BINARY_UNITS = { "B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB" };

	public static String humanReadableByteCount(final double bytes, final boolean useSIUnits) {
		final String[] units = useSIUnits ? SI_UNITS : BINARY_UNITS;
		final int base = useSIUnits ? 1000 : 1024;

		// When using the smallest unit no decimal point is needed, because it's
		// the exact number.
		if (bytes < base) {
			return bytes + " " + units[0];
		}

		final int exponent = (int) (Math.log(bytes) / Math.log(base));
		final String unit = units[exponent];
		return String.format("%.1f %s", bytes / Math.pow(base, exponent), unit);
	}

	/**
	 * Get path attributes of local file (on the DME server file system)
	 *
	 * @param fileLocation The local file location.
	 * @return The path attributes.
	 * @throws HpcException on service failure.
	 */
	public static HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation) throws HpcException {
		try {
			HpcPathAttributes pathAttributes = new HpcPathAttributes();
			pathAttributes.setIsDirectory(false);
			pathAttributes.setIsFile(false);
			pathAttributes.setSize(0);
			pathAttributes.setIsAccessible(true);

			Path path = FileSystems.getDefault().getPath(fileLocation.getFileId());
			pathAttributes.setExists(Files.exists(path));
			if (pathAttributes.getExists()) {
				pathAttributes.setIsAccessible(Files.isReadable(path));
				pathAttributes.setIsDirectory(Files.isDirectory(path));
				pathAttributes.setIsFile(Files.isRegularFile(path));

				HpcPathPermissions pathPermissions = new HpcPathPermissions();
				pathPermissions.setUserId((Integer) Files.getAttribute(path, "unix:uid"));
				pathPermissions.setGroupId((Integer) Files.getAttribute(path, "unix:gid"));
				pathPermissions.setPermissions(PosixFilePermissions.toString(Files.getPosixFilePermissions(path)));

				PosixFileAttributes posixAttributes = Files.readAttributes(path, PosixFileAttributes.class);
				pathPermissions.setOwner(posixAttributes.owner().getName());
				pathPermissions.setGroup(posixAttributes.group().getName());

				pathAttributes.setPermissions(pathPermissions);
			}
			if (pathAttributes.getIsFile()) {
				pathAttributes.setSize(Files.size(path));
			}

			return pathAttributes;

		} catch (IOException e) {
			throw new HpcException("Failed to get local file attributes: [" + e.getMessage() + "] " + fileLocation,
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

}
