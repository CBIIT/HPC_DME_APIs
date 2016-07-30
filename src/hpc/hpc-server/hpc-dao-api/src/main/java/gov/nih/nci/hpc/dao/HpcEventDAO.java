/**
 * HpcEventDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Event DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcEventDAO 
{    
    /**
     * Store a new notification event to the repository.
     *
     * @param event The event to be added.
     * 
     * @throws HpcException
     */
    public void insertEvent(HpcEvent event) throws HpcException;
    
    /**
     * Get all (active) events.
     *
     * @return List<HpcEvent>
     * 
     * @throws HpcException
     */
    public List<HpcEvent> getEvents() throws HpcException;
    
    /**
     * Delete an event
     *
     * @param eventId The eventId to delete.
     * 
     * @throws HpcException
     */
    public void deleteEvent(int eventId) throws HpcException;
    
    /**
     * Store an event to the event history table.
     *
     * @param event The event to be added.
     * 
     * @throws HpcException
     */
    public void insertEventHistory(HpcEvent event) throws HpcException;
}

 