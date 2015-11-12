/**
 * HpcMetadataEntryParam.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dto.metadata;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.exception.HpcException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
	
/**
 * <p>
 * HPC Metada Entry to be used as JAX-RS parameter.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataEntryParam extends HpcMetadataEntry
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	private final static long serialVersionUID = 1L;
	
	// Parsing exception.
	private HpcException jsonParsingException = null;
	
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor to enable HpcMetadataEntry as a JAX-RS parameter.
     * 
     * @param metadataEntryJSONString A metadata entry JSON string.
     * 
     */
	public HpcMetadataEntryParam(String metadataEntryJSONString) 
	{
		super();
		
		// JSON -> POJO.
		try {
             JSONObject jsonMetadataEntry = 
            		    ((JSONObject) new JSONParser().parse(metadataEntryJSONString));
             setAttribute((String) jsonMetadataEntry.get("attribute"));
             setValue((String) jsonMetadataEntry.get("value"));
             setUnit((String) jsonMetadataEntry.get("unit"));
             
		} catch(ParseException e) {
			    jsonParsingException = 
			    	new HpcException("Invalid metadata entry: " + metadataEntryJSONString,
			    			         HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
	
    /**
     * Get the JSON parsing exception.
     *
     * @return null if the JSON was parsed successfully.
     */
	public HpcException getJSONParsingException()
	{
		return jsonParsingException;
	}
}

 