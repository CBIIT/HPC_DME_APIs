/**
 * HpcNotificationServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNotificationSubscription;
import gov.nih.nci.hpc.dao.HpcNotificationDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;

import org.springframework.beans.factory.annotation.Autowired;
/**
 * <p>
 * HPC Notification Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNotificationServiceImpl implements HpcNotificationService
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Notification DAO instance.
	@Autowired
    private HpcNotificationDAO notificationDAO = null;

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default constructor disabled.
     *
     */
    private HpcNotificationServiceImpl()
    {
    }

    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//

    //---------------------------------------------------------------------//
    // HpcSecurityService Interface Implementation
    //---------------------------------------------------------------------//

    @Override
    public void addUpdateNotificationSubscription(String userId,
    		                                      HpcNotificationSubscription notificationSubscription)
                                                 throws HpcException
    {
    	// Input validation.
    	if(!isValidNotificationSubscription(notificationSubscription)) {
    	   throw new HpcException("Invalid notification subscription",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Persist to the DB.
    	notificationDAO.upsert(userId, notificationSubscription);
    }
}

