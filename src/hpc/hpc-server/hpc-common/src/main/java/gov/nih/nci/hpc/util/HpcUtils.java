/**
 * HpcUtils.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.util;

/**
 * <p>
 * General utility class.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUtils 
{   
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default constructor is disabled.
     * 
     */
    private HpcUtils()
    {
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
	
    /**
     * Convert an HPC-DM pattern to SQL 'LIKE' pattern
     *
     * @param hpcPattern The HPC pattern to convert.
     * @return The converted SQL LIKE pattern.
     */
    public static String toSqlLikePattern(String hpcPattern) 
    {
    	if(hpcPattern == null) {
    	   return null;	
    	}
    	
        String sqlLikePattern = hpcPattern;
        sqlLikePattern = sqlLikePattern.replace("%", "\\%");
        sqlLikePattern = sqlLikePattern.replace("_", "\\_");
        sqlLikePattern = sqlLikePattern.replace("*", "%");
        sqlLikePattern = sqlLikePattern.replace("?", "_");
        
        return sqlLikePattern;
    } 
}
    
     