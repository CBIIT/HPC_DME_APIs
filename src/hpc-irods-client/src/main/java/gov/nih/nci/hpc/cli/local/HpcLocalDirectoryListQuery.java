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
	public List<HpcPathAttributes> getPathAttributes(String fileLocation, Pattern excludePattern) throws HpcException {
		List<HpcPathAttributes> pathAttributes = new ArrayList<HpcPathAttributes>();

		// Invoke the Globus directory listing service.
		try {
			List<File> dirContent = listDirectory(fileLocation, excludePattern);
			getPathAttributes(pathAttributes, dirContent);
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
					attributes.add(pathAttributes);
				}
			}

		} catch (Exception e) {
			// Unexpected error. Eat this.
			logger.error("Failed to build directory listing", e);
		}
	}

	public static List<File> listDirectory(String directoryName, Pattern excludePattern) {
		File directory = new File(directoryName);

		List<File> resultList = new ArrayList<File>();

		// get all the files from a directory
		File[] fList = directory.listFiles();
//		resultList.addAll(Arrays.asList(fList));
		for (File file : fList) {
			if(isMatch(file.getName(), excludePattern))
			{
				System.out.println("Excluding file: " + file.getAbsolutePath());
				continue;
			}
			if (file.isFile()) {
				resultList.add(file);
				System.out.println("Including file:" + file.getAbsolutePath());
			} else if (file.isDirectory()) {
				resultList.addAll(listDirectory(file.getAbsolutePath(), excludePattern));
			}
		}
		return resultList;
	}
	
	private static boolean isMatch(String fileName, Pattern excludePattern)
	{
		if(excludePattern == null)
			return false;
		
		  Matcher matcher = excludePattern.matcher(fileName);
		  return matcher.matches();
	}
}
