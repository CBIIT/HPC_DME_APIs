/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

public final class Constants {

	public static final String COLLECTION_PATH = "collection_path";
	public static final String CREATE_PARENT_COLLECTION = "create_parent_collection";
	public static final String PARENT_COLLECTION_PREFIX = "parent_collection_";
	public static final String FILE_SOURCE_ENDPOINT = "source_globus_endpoint";
	// public static final String DESTINATION_PATH = "Destination Path";
	public static final String FILE_SOURCE_FILE_PATH = "source_globus_path";
	public static final String FILE_NAME = "name";
	public static final String PATH = "object_path";
	public static final String PERMISSION = "Permission";
	public static final String USER_ID = "UserId";
	public static final String TYPE = "Type";
	public static final String GROUP_ID = "GroupId";
	public static final String OBJECT_PATH = "object_path";
	
	public static final String CLI_SUCCESS = "CLI_SUCCESS";
	//Authentication error
	public static final String CLI_0 = "Authetication Error";
	
	//Error reading authentication token files
	public static final String CLI_1 = "Invalid Credential Error";
	
	//Invalid user input
	public static final String CLI_2 = "User Input Error";
	
	//No input files to process
	public static final String CLI_3 = "Missing Input Error";
	
	//Failed to process collection
	public static final String CLI_4 = "Collection Download Error";
	
	//System Error
	public static final String CLI_5 = "Server Processing Error";
	
	
	public static final String CLI_6 = "CLI_6";
}
