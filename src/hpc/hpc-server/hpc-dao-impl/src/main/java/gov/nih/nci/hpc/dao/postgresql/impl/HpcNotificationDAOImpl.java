/**
 * HpcNotificationDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcNotificationDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC Notification DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNotificationDAOImpl implements HpcNotificationDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String UPSERT_SUBSCRIPTION_SQL = 
		    "insert into public.\"HPC_NOTIFICATION_SUBSCRIPTION\" ( " +
                    "\"USER_ID\", \"EVENT_TYPE\", \"NOTIFICATION_DELIVERY_METHODS\") " +
                    "values (?, ?, ?) " +
            "on conflict(\"USER_ID\", \"EVENT_TYPE\") do update " +
                    "set \"NOTIFICATION_DELIVERY_METHODS\"=excluded.\"NOTIFICATION_DELIVERY_METHODS\"";
	
	private static final String DELETE_SUBSCRIPTION_SQL = 
			"delete from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" " +
	                "where \"USER_ID\" = ? and \"EVENT_TYPE\" = ?";

	private static final String GET_SUBSCRIPTIONS_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ?";
	
	private static final String GET_SUBSCRIPTION_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ? and \"EVENT_TYPE\" = ?";
	
	private static final String UPSERT_DELIVERY_RECEIPT_SQL = 
		    "insert into public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" ( " +
                    "\"EVENT_ID\", \"USER_ID\", \"NOTIFICATION_DELIVERY_METHOD\", \"DELIVERY_STATUS\", \"DELIVERED\") " +
                    "values (?, ?, ?, ?, ?) " +
            "on conflict(\"EVENT_ID\", \"USER_ID\", \"NOTIFICATION_DELIVERY_METHOD\") do update " +
                    "set \"DELIVERY_STATUS\"=excluded.\"DELIVERY_STATUS\", \"DELIVERED\"=excluded.\"DELIVERED\"";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private HpcNotificationSubscriptionRowMapper notificationSubscriptionRowMapper = 
			                                     new HpcNotificationSubscriptionRowMapper();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcNotificationDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcNotificationDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsertSubscription(
			          String userId,
			          HpcNotificationSubscription notificationSubscription) throws HpcException
    {
		try {
			 // Map the delivery methods to a single string.
			 StringBuilder deliveryMethods = new StringBuilder();
			 for(HpcNotificationDeliveryMethod deliveryMethod : 
				 notificationSubscription.getNotificationDeliveryMethods()) {
				 deliveryMethods.append(deliveryMethod.value() + ",");
			 }
			 
		     jdbcTemplate.update(UPSERT_SUBSCRIPTION_SQL,
		    		             userId,
		    		             notificationSubscription.getEventType().value(),
		    		             deliveryMethods.toString());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a notification subscription: " + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
	@Override
    public void deleteSubscription(String userId, HpcEventType eventType) 
	                              throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_SUBSCRIPTION_SQL,
		    		             userId, eventType.value());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a notification subscription: " + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}		
	}
	
	@Override
	public List<HpcNotificationSubscription> getSubscriptions(String userId) throws HpcException
	{
		try {
		     return jdbcTemplate.query(GET_SUBSCRIPTIONS_SQL, notificationSubscriptionRowMapper,
		    		                   userId);
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscriptions: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
	}
	
	@Override
    public HpcNotificationSubscription getSubscription(String userId, 
                                                       HpcEventType eventType) 
                                                      throws HpcException
    {
		try {
		     return jdbcTemplate.queryForObject(GET_SUBSCRIPTION_SQL, 
		    		                            notificationSubscriptionRowMapper,
		    		                            userId, eventType.value());
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscription: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}				
		
    }
    
    @Override
    public void upsertDeliveryReceipt(HpcNotificationDeliveryReceipt deliveryReceipt) 
                                     throws HpcException
    {
		try {
		     jdbcTemplate.update(UPSERT_DELIVERY_RECEIPT_SQL,
		    		             deliveryReceipt.getEventId(),
		    		             deliveryReceipt.getUserId(),
		    		             deliveryReceipt.getNotificationDeliveryMethod().value(),
		    		             deliveryReceipt.getDeliveryStatus(),
		    		             deliveryReceipt.getDelivered());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a notification delivery receipt: " + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}                                    
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  

	// HpcNotificationSubscription Row to Object mapper.
	private class HpcNotificationSubscriptionRowMapper implements RowMapper<HpcNotificationSubscription>
	{
		@Override
		public HpcNotificationSubscription mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcNotificationSubscription notificationSubscription = new HpcNotificationSubscription();
			notificationSubscription.setEventType(HpcEventType.fromValue(rs.getString("EVENT_TYPE")));
			String deliveryMethods = rs.getString("NOTIFICATION_DELIVERY_METHODS");
			for(String deliveryMethod : deliveryMethods.split(",")) {
				notificationSubscription.getNotificationDeliveryMethods().add(
						    HpcNotificationDeliveryMethod.fromValue(deliveryMethod));
			}
            
            return notificationSubscription;
		}
	}
}

 