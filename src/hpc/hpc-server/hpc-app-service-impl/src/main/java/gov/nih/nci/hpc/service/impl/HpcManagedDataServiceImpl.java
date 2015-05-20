/**
 * HpcManagedDatasetsServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.service.HpcManagedDataService;
import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.dto.types.HpcManagedDataType;
import gov.nih.nci.hpc.domain.HpcManagedData;
import gov.nih.nci.hpc.dao.HpcManagedDataDAO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;

import java.util.List;
import java.util.UUID;
import java.util.GregorianCalendar;

/**
 * <p>
 * HPC Managed Data Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDataServiceImpl implements HpcManagedDataService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Data DAO instance.
    private HpcManagedDataDAO managedDataDAO = null;
    
    // XML type factory
    private DatatypeFactory xmlTypeFactory = null;
    
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
    private HpcManagedDataServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param managedDatDAO The managed dat DAO instance.
     */
    private HpcManagedDataServiceImpl(HpcManagedDataDAO managedDataDAO)
                                     throws HpcException
    {
    	if(managedDataDAO == null) {
     	   throw new HpcException("Null HpcManagedDataDAO instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	try {
    		 xmlTypeFactory = DatatypeFactory.newInstance();
    		
    	} catch(DatatypeConfigurationException e) {
    		    throw new HpcException(
    		    		     "Failed to instantiate DatatypeFactory",
    		    		     HpcErrorType.JAXB_ERROR, e);
    	}
    	
    	this.managedDataDAO = managedDataDAO;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcManagedDataService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void add(HpcManagedDataType type,
    		        List<HpcDataset> datasets) throws HpcException
    {
    	// Create the domain object.
    	HpcManagedData managedData = new HpcManagedData();
    	
    	// Generate and set an ID.
    	managedData.setId(UUID.randomUUID().toString());
    	
    	// Set the creation time to now.
    	GregorianCalendar now = new GregorianCalendar();
    	
    	// Populate type and datasets
    	managedData.setType(type);
    	managedData.setCreated(xmlTypeFactory.newXMLGregorianCalendar(now));
    	for(HpcDataset dataset : datasets) {
    		managedData.getDatasets().add(dataset);
    	}
    	
    	// Persist to Mongo.
    	managedDataDAO.add(managedData);
    }
}

 