/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.cli.util.Paths;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

public class HpcLocalDirectoryListQuery {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Globus transfer status strings.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * Get attributes of a file/directory.
	 *
	 * @param fileLocation
	 *            The endpoint/path to check.
	 * @param client
	 *            Globus client API instance.
	 * @param getSize
	 *            If set to true, the file/directory size will be returned.
	 * @return The path attributes.
	 * @throws HpcException
	 *             on data transfer system failure.
	 */
	public List<HpcPathAttributes> getPathAttributes(String fileLocation, List<String> excludePattern,
			List<String> includePattern) throws HpcException {
		List<HpcPathAttributes> pathAttributes = new ArrayList<HpcPathAttributes>();

		try {
			List<File> dirContent = listDirectory(fileLocation, excludePattern, includePattern);
			getPathAttributes(pathAttributes, dirContent);
			HpcPathAttributes rootPath = new HpcPathAttributes();
			rootPath.setAbsolutePath(fileLocation);
			String name = fileLocation.substring(fileLocation.lastIndexOf("/") > 0 ? fileLocation.lastIndexOf("/") : 0, fileLocation.length());
			rootPath.setName(name);
			rootPath.setIsDirectory(true);
			rootPath.setPath(fileLocation);
			pathAttributes.add(rootPath);
		} catch (Exception e) {
			throw new HpcException("[GLOBUS] Failed to get path attributes: " + fileLocation,
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}

		return pathAttributes;
	}

	private void getPathAttributes(List<HpcPathAttributes> attributes, List<File> dirContent) {
		try {
			if (dirContent != null) {
				for (File file : dirContent) {
					HpcPathAttributes pathAttributes = new HpcPathAttributes();
					pathAttributes.setName(file.getName());
					pathAttributes.setPath(file.getPath());
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
					pathAttributes.setUpdatedDate(sdf.format(file.lastModified()));
					pathAttributes.setAbsolutePath(file.getAbsolutePath());
					if(file.isDirectory())
						pathAttributes.setIsDirectory(true);
					attributes.add(pathAttributes);
				}
			}

		} catch (Exception e) {
			// Unexpected error. Eat this.
			logger.error("Failed to build directory listing", e);
		}
	}

	public static List<File> listDirectory(String directoryName, List<String> excludePattern,
			List<String> includePattern) throws HpcException {
		File directory = new File(directoryName);

		List<File> resultList = new ArrayList<File>();

		// get all the files from a directory
		File[] fList = directory.listFiles();
		if(fList == null)
		{
			System.out.println("Invalid source folder");
			throw new HpcException("Invalid source folder " + directoryName,
					HpcErrorType.DATA_TRANSFER_ERROR);
		}

		Paths paths = getFileList(directoryName, excludePattern, includePattern);
		for (String file : paths) {
			String fileName = file.replace("\\", File.separator);
			fileName = fileName.replace("/", File.separator);
			System.out.println("Including: "+fileName);
			resultList.add(new File(fileName));
		}
		return resultList;
	}

	private static Paths getFileList(String basePath, List<String> excludePatterns, List<String> includePatterns) {
		Paths paths = new Paths();
		if (includePatterns == null || includePatterns.isEmpty())
		{
			includePatterns = new ArrayList<String>();
			includePatterns.add("*");
		}
		
		List<String> patterns = new ArrayList<String>();
		patterns.addAll(includePatterns);
		if(excludePatterns != null)
		{
			for (String pattern : excludePatterns)
				patterns.add("!"+pattern);
		}
		return paths.glob(basePath, patterns);
		
	}
}
