/**
 * HpcJAXBUtil.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;

/**
 * <p>
 * JAXB utility.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

class HpcJAXBUtil 
{   
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
	
    /**
     * Clone (Deep Copy) a JAXB object.
     *
     * @param object the Object to be cloned.
     * @return A cloned new instance of 'object'.
     * 
     * @throws HpcException
     */
  	@SuppressWarnings("unchecked")
	public static <T> T cloneJAXB(T object) throws HpcException
  	{
  		if(object != null) { 
    	   return cloneJAXB(object, (Class<T>) object.getClass());
    	}
  		
  		return null;
  	}
  	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
  	
    /**
     * Clone (Deep Copy) a JAXB object.
     *
     * @param object the Object to be cloned.
     * @param clazz The object class. 
     * @return A cloned new instance of 'object'
     */
    private static <T> T cloneJAXB(T object, Class<T> clazz) throws HpcException
    {
    	try {
  	         JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
  	         JAXBElement<T> contentObject = new JAXBElement<T>(new QName(clazz.getSimpleName()), 
  	        		                                                     clazz, object);
  	         JAXBSource source = new JAXBSource(jaxbContext, contentObject);
  	         return jaxbContext.createUnmarshaller().unmarshal(source, clazz).getValue();
  	         
  	  	} catch(JAXBException e) {
  	           throw new HpcException("Failed to clone a JAXB Object", 
  	          		                  HpcErrorType.UNEXPECTED_ERROR, e);
  	  	}
  	}
}

 