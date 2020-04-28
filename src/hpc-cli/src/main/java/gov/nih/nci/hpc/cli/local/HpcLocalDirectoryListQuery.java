/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.cli.util.Paths;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HpcLocalDirectoryListQuery {

  private static String genFileSizeDisplayString(long sizeInBytes) {
    final String formattedFig =
      NumberFormat.getInstance(Locale.US).format(sizeInBytes);

    final String storageUnit = (sizeInBytes == 1) ? "byte" : "bytes";

    final String retDisplayStr = String.format(
      "Aggregate file size: %s %s", formattedFig, storageUnit);

    return retDisplayStr;
  }

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
		    logger.debug("getPathAttributes: fileLocation: "+fileLocation);
		    logger.debug("getPathAttributes: excludePattern: "+excludePattern);
		    logger.debug("getPathAttributes: includePattern: "+includePattern);
			List<File> dirContent = listDirectory(fileLocation, excludePattern, includePattern);
			getPathAttributes(pathAttributes, dirContent);
			HpcPathAttributes rootPath = new HpcPathAttributes();
			rootPath.setAbsolutePath(fileLocation);
			String name = fileLocation.substring(fileLocation.lastIndexOf("/") > 0 ? fileLocation.lastIndexOf("/") : 0,
					fileLocation.length());
			rootPath.setName(name);
			rootPath.setIsDirectory(true);
			rootPath.setPath(fileLocation);
			pathAttributes.add(rootPath);
		} catch (Exception e) {
		  logger.error(e.getMessage(), e);
			throw new HpcException("Failed to get path attributes: " + fileLocation,
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}

		return pathAttributes;
	}

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
	public List<HpcPathAttributes> getFileListPathAttributes(String localBasePath, String fileLocation, List<String> excludePattern,
			List<String> includePattern) throws HpcException {
		List<HpcPathAttributes> pathAttributes = new ArrayList<HpcPathAttributes>();

		try {
			List<String> files = readFileListfromFile(fileLocation);
			long totalSize = 0L;
			for(String filePath : files)
			{
				HpcPathAttributes filePathAttr = new HpcPathAttributes();
				String fullPath = localBasePath + File.separator + filePath;
				fullPath = fullPath.replace("\\", "/");
				filePathAttr.setAbsolutePath(fullPath);
				String name = filePath.substring(filePath.lastIndexOf("/") > 0 ? filePath.lastIndexOf("/") : 0,
						filePath.length());
				filePathAttr.setName(name);
				File fileToCheckDir = new File(filePath);
				filePathAttr.setIsDirectory(fileToCheckDir.isDirectory());
				filePathAttr.setPath(filePath);
				totalSize = totalSize + fileToCheckDir.length();
				System.out.println("Including: " + fullPath);
				pathAttributes.add(filePathAttr);
			}
			System.out.println("\nAggregate file size (bytes): " + totalSize);
		} catch (Exception e) {
			throw new HpcException("Failed to get path attributes: " + fileLocation,
					HpcErrorType.INVALID_REQUEST_INPUT, e);
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
					if (file.isDirectory())
						pathAttributes.setIsDirectory(true);
					attributes.add(pathAttributes);
				}
			}

		} catch (Exception e) {
			// Unexpected error. Eat this.
			logger.error("Failed to build directory listing", e);
		}
	}

	private List<String> readFileListfromFile(String fileName) {
		if (fileName == null || fileName.isEmpty())
			return null;
		BufferedReader reader = null;
		List<String> patterns = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = reader.readLine()) != null) {
				patterns.add(line);
			}

		} catch (IOException e) {
			throw new HpcCmdException("Failed to read files list due to: " + e.getMessage());
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
				}
		}
		return patterns;
	}

  public List<File> listDirectory(String directoryName, List<String> excludePattern,
      List<String> includePattern) throws HpcException {
    		File directory = new File(directoryName);
 //   File directory = new File(Paths.generateFileSystemResourceUri(directoryName));
    List<File> resultList = new ArrayList<File>();

    // get all the files from a directory
    File[] fList = directory.listFiles();
    if (fList == null) {
      System.out.println("Invalid source folder");
      throw new HpcException("Invalid source folder " + directoryName,
          HpcErrorType.DATA_TRANSFER_ERROR);
    }

    if (includePattern == null || includePattern.isEmpty()) {
      includePattern = new ArrayList<String>();
      includePattern.add("*");
      includePattern.add("*/**");
    }

    long totalSize = 0L;
    Paths paths = getFileList(directoryName, excludePattern, includePattern);
    for (String filePath : paths) {
      String fileName = filePath.replace("\\", File.separator).replace(
                                        "/", File.separator);
      System.out.println("Including: " + fileName);
		File file = new File(fileName);
//      File file = new File(Paths.generateFileSystemResourceUri(fileName));
      totalSize += file.length();
      resultList.add(file);
    }
    System.out.println("\n" + genFileSizeDisplayString(totalSize));

    return resultList;
  }


	private Paths getFileList(String basePath, List<String> excludePatterns, List<String> includePatterns) {
		Paths paths = new Paths();
		if (includePatterns == null || includePatterns.isEmpty()) {
			includePatterns = new ArrayList<String>();
			includePatterns.add("*");
		}

		List<String> patterns = new ArrayList<String>();
		patterns.addAll(includePatterns);
		if (excludePatterns != null) {
			for (String pattern : excludePatterns)
				patterns.add("!" + pattern);
		}
		patterns.add("!**/hpc*.log/**");
		logger.debug("basePath "+basePath);
		return paths.glob(basePath, patterns);

	}
}
