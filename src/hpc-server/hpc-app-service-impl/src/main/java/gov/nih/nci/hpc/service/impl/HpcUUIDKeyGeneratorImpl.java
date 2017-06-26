/**
 * HpcUUIDKeyGeneratorImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import java.util.UUID;

/**
 * <p>
 * HPC Key generator w/ UUID.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUUIDKeyGeneratorImpl implements HpcKeyGenerator
{         
    //---------------------------------------------------------------------//
    // HpcKeyGenerator Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String generateKey()
    {
    	return UUID.randomUUID().toString();
    }
    
    @Override
    public boolean validateKey(String key)
    {
    	try {
   	     	 if(key == null || UUID.fromString(key) == null) {
   	            return false;
   	     	 }
   	     
    	} catch(IllegalArgumentException e) {
   		        return false;
    	}
    	
    	return true;
    }
}

 