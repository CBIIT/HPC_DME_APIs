/**
 * HpcSingleResultCallback.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import com.mongodb.async.SingleResultCallback;

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.lang.InterruptedException;

/**
 * <p>
 * HPC Single Result Callback. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcSingleResultCallback<T> implements SingleResultCallback<T>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Async operation timeout (seconds).
    private final static long TIMEOUT = 60; 
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	// The exception.
	private HpcException exception = null;
	
	// The result.
	private T result = null;
	
	// The countdown latch.
	CountDownLatch countDownLatch = new CountDownLatch(1);
	
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcSingleResultCallback()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Throws the exception throws during the async call.
     *
     * @Throws HpcException Throws the exception thrown during the async mongo call (if any).
     */
    public void throwException() throws HpcException                          
    {
    	try {
    		 if(!countDownLatch.await(TIMEOUT, TimeUnit.SECONDS)) {
    	        throw new HpcException("Mongo async operation timed out", 
    			                       HpcErrorType.MONGO_ERROR);
    	     }
    	} catch(InterruptedException e) {
    		    throw new HpcException("Mongo async operation timed out", 
	                                   HpcErrorType.MONGO_ERROR, e); 
    	}
    	
    	if(exception != null) {
    	   throw exception;
    	}
    }   
    
    /**
     * Get the async call result.
     *
     * @return The result.
     * @Throws HpcException Throws the exception thrown during the async mongo call (if any).
     */
    public T getResult() throws HpcException                            
    {
    	throwException();
    	return result;
    } 
    
    //---------------------------------------------------------------------//
    // SingleResultCallback<T> Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void onResult(final T result, final Throwable t) {
    	if(t != null) {
    	   exception = new HpcException("MongoDB exception", 
	                                    HpcErrorType.MONGO_ERROR, t);
    	}
    	
    	this.result = result;
    	countDownLatch.countDown();
    }
}

 