/**
 * HpcDataRegistrationDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationTask;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Registration DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataRegistrationDAO 
{    
    /**
     * Store a new data object list registration task (if dataObjectListRegistrationTask.getId() is provided NULL), 
     * or update an existing task.
     * Note: If a new task is inserted, dataObjectDownloadTask.getId() will be updated with the generated ID.
     *
     * @param dataObjectListRegistrationTask The data object list registration task to persist.
     * @throws HpcException on database error.
     */
    public void upsertDataObjectListRegistrationTask(
    		          HpcDataObjectListRegistrationTask dataObjectListRegistrationTask) 
    		          throws HpcException;
    
    /**
     * Get a data object list registration task.
     *
     * @param id The data object list registration task ID.
     * @return The registration task object, or null if not found.
     * @throws HpcException on database error.
     */
    public HpcDataObjectListRegistrationTask getDataObjectListRegistrationTask(String id) throws HpcException;
//    
//    *//**
//     * Delete a data object list registration task.
//     *
//     * @param id The data object list registration task ID.
//     * @throws HpcException on database error.
//     *//*
//    public void deleteDataObjectListRegistrationTask(String id) throws HpcException;
//    
//    /**
//     * Get data object list registration tasks. 
//     *
//     * @param status Get tasks in this status.
//     * @return A list of data object list registration tasks.
//     * @throws HpcException on database error.
//     */
//    public List<HpcDataObjectListRegistrationTask> getDataObjectListRegistrationTasks(
//    		                                              HpcDataObjectListRegistrationTaskStatus status) 
//    		                                              throws HpcException;
//    
//    *//**
//     * Store a new data object list registration result, or updated an task result.
//     *
//     * @param registrationTaskResult The registration task result to persist.
//     * @throws HpcException on database error.
//     *//*
//    public void upsertDataObjectListRegistrationResult(HpcDataObjectListRegistrationResult registrationResult) 
//    		                                          throws HpcException;
//    
//    *//**
//     * Get a data object list registration result.
//     *
//     * @param id The registration task ID.
//     * @return The registration task result object, or null if not found.
//     * @throws HpcException on database error.
//     *//*
//    public HpcDataObjectListRegistrationResult getDataObjectListRegistrationResult(String id) throws HpcException;*/
}

 