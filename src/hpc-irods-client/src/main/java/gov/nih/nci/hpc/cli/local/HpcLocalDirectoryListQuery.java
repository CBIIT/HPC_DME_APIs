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

		// Invoke the Globus directory listing service.
		try {
			List<File> dirContent = listDirectory(fileLocation, excludePattern, includePattern);
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

		// resultList.addAll(Arrays.asList(fList));
		Paths paths = getFileList(directoryName, excludePattern, includePattern);
		for (String file : paths) {
			System.out.println("Including: "+file);
			resultList.add(new File(file));
		}
		return resultList;
	}

	private static boolean isExcludePattern(String fileName, List<Pattern> excludePatterns, List<Pattern> includePatterns) {
		if ((includePatterns == null || includePatterns.isEmpty())
				&& (excludePatterns == null || excludePatterns.isEmpty()))
			return false;

		boolean include = false;
		if (includePatterns != null && !includePatterns.isEmpty()) {
			for (Pattern pattern : includePatterns) {
				Matcher matcher = pattern.matcher(fileName);
				if (matcher.matches()) {
					include = true;
					break;
				}
			}
		} else
			include = true;

		boolean exclude = false;
		if (excludePatterns != null && !excludePatterns.isEmpty() && include) {

			for (Pattern pattern : excludePatterns) {
				Matcher matcher = pattern.matcher(fileName);
				if (matcher.matches()) {
					exclude = true;
					break;
				}
			}
		}
		
		if(fileName.indexOf(".metadata.json") >0)
			return true;
		
		return exclude;
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
//		System.out.println(patterns);
		if(excludePatterns != null)
		{
			for (String pattern : excludePatterns)
				patterns.add("!"+pattern);
		}
		patterns.add("!.metadata.json");
		return paths.glob(basePath, patterns);
		
	}
	
	private static boolean isExclude(String fileName, List<String> excludePatterns, List<String> includePatterns) {
		if ((includePatterns == null || includePatterns.isEmpty())
				&& (excludePatterns == null || excludePatterns.isEmpty()))
			return false;

		boolean include = true;
		if (includePatterns != null && !includePatterns.isEmpty()) {
			for (String pattern : includePatterns) {
				//Pattern regpattern = Pattern.compile(pattern, Pattern.UNICODE_CASE  | Pattern.CASE_INSENSITIVE);
				Pattern regpattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
				Matcher matcher = regpattern.matcher(fileName);
				include =matcher.matches();
				System.out.println("fileName: "+fileName + " " +include);
				if(include)
					break;
//				if(pattern.startsWith("*") || pattern.startsWith("."))
//				{
//					String matchingExt = pattern.substring(pattern.indexOf("*")+1, pattern.length());
//					if(fileName.endsWith(matchingExt))
//					{
//						include = true;
//						break;
//					}
//						
//				}else
//				{
//					if(fileName.indexOf(pattern) != -1)
//					{
//						include = true;
//						break;
//					}
//				}
			}
		} else
			include = true;

		if(!include)
			return true;
		
		boolean exclude = false;
		if (excludePatterns != null && !excludePatterns.isEmpty() && include) {
			for (String pattern : excludePatterns) {
				Pattern regpattern = Pattern.compile(pattern, Pattern.LITERAL);
				Matcher matcher = regpattern.matcher(fileName);
				exclude =matcher.matches();
				System.out.println("fileName: "+fileName + " " +include);
				if(exclude)
					break;
				
//				if(pattern.startsWith("*") || pattern.startsWith("."))
//				{
//					String matchingExt = pattern.substring(pattern.indexOf("*")+1, pattern.length());
//					if(fileName.endsWith(matchingExt))
//					{
//						exclude = true;
//						break;
//					}
//						
//				}else
//				{
//					if(fileName.indexOf(pattern) != -1)
//					{
//						exclude = true;
//						break;
//					}
//				}
			}
		}
		
		if(fileName.indexOf(".metadata.json") >0)
			return true;
		
		return exclude;
	}

}
