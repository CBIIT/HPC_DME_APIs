/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.globus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.globusonline.transfer.APIError;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

public class HpcGlobusDirectoryListQuery {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Globus transfer status strings.
	private static final String FAILED_STATUS = "FAILED";
	private static final String SUCCEEDED_STATUS = "SUCCEEDED";

	private static final String NOT_DIRECTORY_GLOBUS_CODE = "ExternalError.DirListingFailed.NotDirectory";

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Globus connection instance.
	private HpcGlobusConnection globusConnection = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	public HpcGlobusDirectoryListQuery(String nexusURL, String globusURL) {
		globusConnection = new HpcGlobusConnection(nexusURL, globusURL);
	}

	public Object authenticate(String userName, String password) throws HpcException {
		return globusConnection.authenticate(userName, password);
	}

	public Object authenticateWithToken(String userName, String token) throws HpcException {
		return globusConnection.authenticate2(userName, token);
	}

	public List<HpcPathAttributes> getPathAttributes(Object authenticatedToken, HpcFileLocation fileLocation)
			throws HpcException {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
		autoActivate(fileLocation.getFileContainerId(), client);
		return getPathAttributes(fileLocation, client);
	}

	private Calendar convertToLexicalTime(String timeStr) {
		if (timeStr == null || "null".equalsIgnoreCase(timeStr))
			return null;
		else
			return DatatypeConverter.parseDateTime(timeStr.trim().replace(' ', 'T'));
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
	private List<HpcPathAttributes> getPathAttributes(HpcFileLocation fileLocation, JSONTransferAPIClient client)
			throws HpcException {
		List<HpcPathAttributes> pathAttributes = new ArrayList<HpcPathAttributes>();

		// Invoke the Globus directory listing service.
		try {
			Result dirContent = listDirectoryContent(fileLocation, client);
			getPathAttributes(pathAttributes, dirContent, client);
		} catch (APIError error) {
			if (error.statusCode == 502) {
				if (error.code.equals(NOT_DIRECTORY_GLOBUS_CODE)) {
					// Path exists as a single file
					// pathAttributes.setExists(true);
					// pathAttributes.setIsFile(true);
				} else {
					throw new HpcException("Error at Globus endpoint " + fileLocation.getFileContainerId()
					+ ", file location: " + fileLocation.getFileId()
					+ ": " + error.statusMessage, HpcErrorType.DATA_TRANSFER_ERROR);
				}
			} else if (error.statusCode == 403) {
				// Permission denied.
				// pathAttributes.setExists(true);
				// pathAttributes.setIsAccessible(false);
			} // else path was not found.

		} catch (Exception e) {
			throw new HpcException("[GLOBUS] Failed to get path attributes: " + fileLocation,
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}

		return pathAttributes;
	}

	private void getPathAttributes(List<HpcPathAttributes> attributes, Result dirContent,
			JSONTransferAPIClient client) {
		try {
			JSONArray jsonFiles = dirContent.document.getJSONArray("DATA");
			if (jsonFiles != null) {
				// Iterate through the directory files, and sum up the files
				// size.
				int filesNum = jsonFiles.length();
				long size = 0;
				for (int i = 0; i < filesNum; i++) {
					HpcPathAttributes pathAttributes = new HpcPathAttributes();
					JSONObject jsonFile = jsonFiles.getJSONObject(i);
					String jsonFileType = jsonFile.getString("type");
					if (jsonFileType != null) {
						if (jsonFileType.equals("file")) {
							pathAttributes.setName(jsonFile.getString("name"));
							pathAttributes
									.setPath(dirContent.document.getString("path") + '/' + jsonFile.getString("name"));
							pathAttributes.setUpdatedDate(jsonFile.getString("last_modified"));
							attributes.add(pathAttributes);
							// This is a file. Add its size to the total;
							continue;
						} else if (jsonFileType.equals("dir")) {
							// It's a sub directory. Make a recursive call, to
							// add its size.
							HpcFileLocation subDirLocation = new HpcFileLocation();
							subDirLocation.setFileContainerId(dirContent.document.getString("endpoint"));
							subDirLocation.setFileId(
									dirContent.document.getString("path") + '/' + jsonFile.getString("name"));
							getPathAttributes(attributes, listDirectoryContent(subDirLocation, client), client);
						}
					}
				}
			}

		} catch (Exception e) {
			// Unexpected error. Eat this.
			logger.error("Failed to build directory listing", e);
		}
	}

	/**
	 * Call the Globus list directory content service. See:
	 * https://docs.globus.org/api/transfer/file_operations/#
	 * list_directory_contents
	 *
	 * @param dirLocation
	 *            The directory endpoint/path.
	 * @param client
	 *            Globus client API instance.
	 * @return The file size in bytes.
	 * @throws APIError
	 *             on Globus failure.
	 * @throws HpcException
	 *             on service failure.
	 */
	private Result listDirectoryContent(HpcFileLocation dirLocation, JSONTransferAPIClient client)
			throws APIError, HpcException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("path", dirLocation.getFileId());
		try {
      final String resource = UriComponentsBuilder.fromHttpUrl(
        BaseTransferAPIClient.endpointPath(dirLocation.getFileContainerId()))
        .path("/ls").build().encode().toUri().toURL().toExternalForm();
			return client.getResult(resource, params);

		} catch (APIError apiError) {
			throw apiError;
		} catch (Exception e) {
			throw new HpcException("[GLOBUS] Failed to invoke list directory content service: " + dirLocation,
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}

	private void autoActivate(String endpointName, JSONTransferAPIClient client) throws HpcException {
		try {
      final String resource = UriComponentsBuilder.fromHttpUrl(
        BaseTransferAPIClient.endpointPath(endpointName))
        .path("/autoactivate").queryParam("if_expires_in", "100").build()
        .encode().toUri().toURL().toExternalForm();
			client.postResult(resource, null, null);

		} catch (Exception e) {
			throw new HpcException("[GLOBUS] Endpoint doesn't exist or is inactive. Make sure the endpoint UUID "
					+ "is correct and active: " + endpointName, HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}

}
