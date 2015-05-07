/**
 * HpcMetadataServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.metadata;

import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataDTO;
import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * <p>
 * HPC Metadata Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataServiceImpl implements HpcMetadataService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Metadata DAO instance.
    private HpcMetadataDAO metadataDAO = null;
    
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcMetadataServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param metadataDAO The metadata DAO instance.
     */
    private HpcMetadataServiceImpl(HpcMetadataDAO metadataDAO)
                                  throws HpcException
    {
    	if(metadataDAO == null) {
     	   throw new HpcException("Null HpcMetadataDAO instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.metadataDAO = metadataDAO;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataService Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
    public void addMetadata(HpcMetadataDTO metadata)
    {
		metadataDAO.createMetadata(metadata);
    }
	
	@Override
	public HpcMetadataDTO getMetadata(String id)
	{
		HpcMetadataDTO metadataDTO = new HpcMetadataDTO();
		metadataDTO.setId("53434.fhfh.dd");
		
		try {
			 GregorianCalendar gcal = new GregorianCalendar();
			 XMLGregorianCalendar xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
			 metadataDTO.setCreationDate(xgcal);
		} catch(Exception e) {
			
		}
		
		metadataDTO.setUserId("Eran Rosenberg. Yay");
		metadataDTO.setSize(24207177.00);
		
		return metadataDTO;
	}
}

 