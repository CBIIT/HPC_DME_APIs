/**
 * HpcGlobusDirectoryBrowser.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.globus.impl;

import java.util.HashMap;
import java.util.Map;

import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;

import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;

/**
 * <p>
 * Globus directory browser. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcGlobusDirectoryBrowser 
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Retry template. Used to automatically retry Globus service calls.
	@Autowired
	private RetryTemplate retryTemplate = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcGlobusDirectoryBrowser()
    {
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Call the Globus list directory content service. It lists the entries (files / directories)
     * immediately under the directory. No traversing / recursive calls to sub-directories.
     * See: https://docs.globus.org/api/transfer/file_operations/#list_directory_contents
     *
     * @param dirLocation The directory endpoint/path.
     * @param client Globus client API instance.
     * @return The file size in bytes.
     * @throws Exception on service failure.
     */
    public Result list(HpcFileLocation dirLocation, 
    		           JSONTransferAPIClient client)
    		          throws Exception
    {
		Map<String, String> params = new HashMap<String, String>();
		params.put("path", dirLocation.getFileId());
		
		return retryTemplate.execute(arg0 -> 
		{
			String resource = BaseTransferAPIClient.endpointPath(dirLocation.getFileContainerId()) + "/ls";
		 	return client.getResult(resource, params);
		});
    }
    
    /**
     * Scan a Globus directory. Each file in the directory tree structure (including all sub directories
     * recursively) is visited.
     *
     * @param dirContent The directory content (as returned from list())
     * @param client Globus client API instance.
     * @param visitor A provided callback to invoke on each file visited in the directory tree.
     * @throws Exception on service failure.
     */
    public void scan(Result dirContent, JSONTransferAPIClient client, HpcGlobusFileVisitor visitor)
                    throws Exception
    {
         JSONArray jsonFiles = dirContent.document.getJSONArray("DATA");
         if(jsonFiles != null) {
            // Iterate through the directory files, and sum up the files size.
        	int filesNum = jsonFiles.length();
            for(int i = 0; i < filesNum; i++) {
            	JSONObject jsonFile = jsonFiles.getJSONObject(i);
            	String jsonFileType = jsonFile.getString("type");
            	if(jsonFileType != null) {
            	   if(jsonFileType.equals("file")) {
            		  // This is a file. Visit it.
            	      visitor.onFile(jsonFile);
            	      continue;
            	   } else if(jsonFileType.equals("dir")) {
            		         // It's a sub directory. Make a recursive call, to visit its files/sub-directories
            		         HpcFileLocation subDirLocation = new HpcFileLocation();
            		         subDirLocation.setFileContainerId(dirContent.document.getString("endpoint"));
            		         subDirLocation.setFileId(dirContent.document.getString("path") +
            		        		                '/' + jsonFile.getString("name"));
            		         
            		         scan(list(subDirLocation, client), client, visitor);
            	   }
            	}
            }
         }
    }	
}

 