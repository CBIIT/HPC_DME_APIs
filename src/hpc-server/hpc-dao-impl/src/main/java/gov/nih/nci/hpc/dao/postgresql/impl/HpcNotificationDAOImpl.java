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

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.transaction.annotation.Transactional;

import gov.nih.nci.hpc.dao.HpcNotificationDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationTrigger;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Notification DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
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
                    "values (?, ?, ?::text[]) " +
            "on conflict(\"USER_ID\", \"EVENT_TYPE\") do update set " +
                    "\"NOTIFICATION_DELIVERY_METHODS\"=excluded.\"NOTIFICATION_DELIVERY_METHODS\"";
	
	private static final String DELETE_SUBSCRIPTION_SQL = 
			"delete from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" " +
	                "where \"USER_ID\" = ? and \"EVENT_TYPE\" = ?";

	private static final String GET_SUBSCRIPTIONS_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ?";
	
	private static final String GET_SUBSCRIPTION_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ? and \"EVENT_TYPE\" = ?";
	
	private static final String GET_SUBSCRIPTION_ID_SQL = 
		    "select \"ID\" from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ? and \"EVENT_TYPE\" = ?";
	
	private static final String INSERT_TRIGGER_SQL = 
		    "insert into public.\"HPC_NOTIFICATION_TRIGGER\" ( " +
                    "\"NOTIFICATION_SUBSCRIPTION_ID\", \"NOTIFICATION_TRIGGER\") values (?, ?::text[])";
	
	private static final String GET_TRIGGER_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_TRIGGER\" where \"NOTIFICATION_SUBSCRIPTION_ID\" = ?";
	
	private static final String DELETE_TRIGGER_SQL = 
		    "delete from public.\"HPC_NOTIFICATION_TRIGGER\" where \"NOTIFICATION_SUBSCRIPTION_ID\" = ?";

	private static final String GET_SUBSCRIBED_USERS_SQL = 
		    "select \"USER_ID\" from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"EVENT_TYPE\" = ?";
	
	private static final String GET_SUBSCRIBED_USERS_WITH_TRIGGER_SQL = 
			"select distinct \"USER_ID\" from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" subscription " +
	        "join public.\"HPC_NOTIFICATION_TRIGGER\" trigger " +
			"on subscription.\"ID\" = trigger.\"NOTIFICATION_SUBSCRIPTION_ID\" "+
			"where subscription.\"EVENT_TYPE\" = ? and trigger.\"NOTIFICATION_TRIGGER\" <@ ?::text[]";
	
	private static final String UPSERT_DELIVERY_RECEIPT_SQL = 
		    "insert into public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" ( " +
                    "\"EVENT_ID\", \"USER_ID\", \"NOTIFICATION_DELIVERY_METHOD\", \"DELIVERY_STATUS\", \"DELIVERED\") " +
                    "values (?, ?, ?, ?, ?) " +
            "on conflict(\"EVENT_ID\", \"USER_ID\", \"NOTIFICATION_DELIVERY_METHOD\") do update " +
                    "set \"DELIVERY_STATUS\"=excluded.\"DELIVERY_STATUS\", \"DELIVERED\"=excluded.\"DELIVERED\"";
	
	private static final String GET_DELIVERY_RECEIPTS_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" where \"USER_ID\" = ? " +
	        "order by \"EVENT_ID\" desc limit ? offset ?";
	
	private static final String GET_DELIVERY_RECEIPT_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" where \"USER_ID\" = ? and \"EVENT_ID\" = ?";

	private static final String GET_DELIVERY_RECEIPTS_COUNT_SQL = 
		    "select count(*) from public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" where \"USER_ID\" = ? ";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	@Qualifier("hpcPostgreSQLJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private RowMapper<HpcNotificationSubscription> notificationSubscriptionRowMapper = (rs, rowNum) -> 
	{
		HpcNotificationSubscription notificationSubscription = new HpcNotificationSubscription();
		notificationSubscription.setId(rs.getInt("ID"));
		notificationSubscription.setEventType(HpcEventType.fromValue(rs.getString("EVENT_TYPE")));
		String[] deliveryMethods = (String[]) rs.getArray("NOTIFICATION_DELIVERY_METHODS").getArray();
		for(String deliveryMethod : deliveryMethods) {
			notificationSubscription.getNotificationDeliveryMethods().add(
					    HpcNotificationDeliveryMethod.fromValue(deliveryMethod));
		}
        
        return notificationSubscription;
	};
	private RowMapper<HpcNotificationTrigger> notificationTriggerRowMapper = (rs, rowNum) ->
	{
		HpcNotificationTrigger notificationTrigger = new HpcNotificationTrigger();
		String[] triggers = (String[]) rs.getArray("NOTIFICATION_TRIGGER").getArray();
		for(String trigger : triggers) {
			notificationTrigger.getPayloadEntries().add(fromString(trigger));
		}
        
        return notificationTrigger;
	};
	private RowMapper<HpcNotificationDeliveryReceipt> notificationDeliveryReceiptRowMapper = (rs, rowNum) -> 
	{
		HpcNotificationDeliveryReceipt notificationDelivertReceipt = new HpcNotificationDeliveryReceipt();
    	Calendar delivered = Calendar.getInstance();
    	delivered.setTime(rs.getTimestamp("DELIVERED"));
		notificationDelivertReceipt.setDelivered(delivered);
		notificationDelivertReceipt.setDeliveryStatus(rs.getBoolean("DELIVERY_STATUS"));
		notificationDelivertReceipt.setEventId(rs.getInt("EVENT_ID"));
		notificationDelivertReceipt.setNotificationDeliveryMethod(
				     HpcNotificationDeliveryMethod.fromValue(rs.getString("NOTIFICATION_DELIVERY_METHOD")));
		notificationDelivertReceipt.setUserId(rs.getString("USER_ID"));
        
        return notificationDelivertReceipt;
	};
	private SingleColumnRowMapper<String> userIdRowMapper = new SingleColumnRowMapper<>();
	private SingleColumnRowMapper<Integer> notificationIdRowMapper = new SingleColumnRowMapper<>();
	
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
	@Transactional
	public void upsertSubscription(
			          String userId,
			          HpcNotificationSubscription notificationSubscription) throws HpcException
    {
		try {
			 String eventType = notificationSubscription.getEventType().value();
		     jdbcTemplate.update(UPSERT_SUBSCRIPTION_SQL,
		    		             userId, eventType,
		    		             deliveryMethodsToSQLTextArray(notificationSubscription.getNotificationDeliveryMethods()));
		     
		     // Update the notification triggers.
		     Integer notificationId = 
		    			jdbcTemplate.queryForObject(GET_SUBSCRIPTION_ID_SQL, notificationIdRowMapper, 
		    					                    userId, eventType);
		     jdbcTemplate.update(DELETE_TRIGGER_SQL, notificationId);
		     for(HpcNotificationTrigger trigger : notificationSubscription.getNotificationTriggers()) {
		         jdbcTemplate.update(INSERT_TRIGGER_SQL, notificationId, 
		    		    	            payloadEntriesToSQLTextArray(trigger.getPayloadEntries()));
		     }
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a notification subscription: " + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}		
	}
	
	@Override
	public List<HpcNotificationSubscription> getSubscriptions(String userId) throws HpcException
	{
		try {
			 // Get the subscriptions.
			 List<HpcNotificationSubscription> subscriptions =
				  jdbcTemplate.query(GET_SUBSCRIPTIONS_SQL, notificationSubscriptionRowMapper, userId);
		     
			 // Set the triggers.
			 for(HpcNotificationSubscription subscription : subscriptions) {
				 subscription.getNotificationTriggers().addAll(
				                 jdbcTemplate.query(GET_TRIGGER_SQL, notificationTriggerRowMapper,
						                            subscription.getId()));
			 }
			 
			 return subscriptions;
			 
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscriptions: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}		
	}
	
	@Override
    public HpcNotificationSubscription getSubscription(String userId, 
                                                       HpcEventType eventType) 
                                                      throws HpcException
    {
		try {
			 // Get the subscription.
			 HpcNotificationSubscription subscription =
			    jdbcTemplate.queryForObject(GET_SUBSCRIPTION_SQL, 
		    	                            notificationSubscriptionRowMapper,
		    	                            userId, eventType.value());
			 
			 // Set the triggers.
			 subscription.getNotificationTriggers().addAll(
					         jdbcTemplate.query(GET_TRIGGER_SQL, notificationTriggerRowMapper,
							                    subscription.getId()));
			 
			 return subscription;
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscription: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}				
		
    }
    
	@Override
    public List<String> getSubscribedUsers(HpcEventType eventType) 
                                          throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_SUBSCRIBED_USERS_SQL, userIdRowMapper, eventType.value());
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscribed users: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}				
    }
	
	@Override
    public List<String> getSubscribedUsers(HpcEventType eventType, List<HpcEventPayloadEntry> eventPayloadEntries) 
                                          throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_SUBSCRIBED_USERS_WITH_TRIGGER_SQL, userIdRowMapper,
		    		                   eventType.value(), payloadEntriesToSQLTextArray(eventPayloadEntries));
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscribed users: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}                                    
    }
    
    @Override
    public List<HpcNotificationDeliveryReceipt> 
           getDeliveryReceipts(String userId, int offset, int limit) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_DELIVERY_RECEIPTS_SQL, notificationDeliveryReceiptRowMapper,
		    		                   userId, limit, offset);
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscriptions: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}	
    }

    @Override
    public HpcNotificationDeliveryReceipt 
           getDeliveryReceipt(String userId, int eventId) throws HpcException
    {
		try {
		    List<HpcNotificationDeliveryReceipt> receipts = jdbcTemplate.query(GET_DELIVERY_RECEIPT_SQL, notificationDeliveryReceiptRowMapper,
		    		                   userId, eventId);
		    if(receipts != null && receipts.size() > 0)
		    	return receipts.get(0);
		    else
		    	return null;
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscriptions: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}	
    }

    @Override
    public int getDeliveryReceiptsCount(String userId) throws HpcException
    {
		try {
		     return jdbcTemplate.queryForObject(GET_DELIVERY_RECEIPTS_COUNT_SQL, Integer.class, userId);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to count notification delivery receipts: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}	
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  

    /** 
     * Map a collection of delivery methods to a SQL text array.
     * 
     * @param deliveryMethods A list of delivery methods.
     * @return array of text values.
     */
	 private String deliveryMethodsToSQLTextArray(List<HpcNotificationDeliveryMethod> deliveryMethods)
	 {
		 StringBuilder deliveryMethodsArray = new StringBuilder();
		 deliveryMethodsArray.append("{");
		 for(HpcNotificationDeliveryMethod deliveryMethod : deliveryMethods) {
			 if(deliveryMethodsArray.length() > 1) {
				deliveryMethodsArray.append(",");
			 }
			 deliveryMethodsArray.append("\"" + deliveryMethod.value() + "\"");
		 }
		 deliveryMethodsArray.append("}");

		 return deliveryMethodsArray.toString();
	 }
	 
    /** 
     * Map a collection of event payload entries to a SQL text array (as a String).
     * 
     * @param payloadEntries A list of payload entries.
     * @return SQL text array string.
     */
	 private String payloadEntriesToSQLTextArray(List<HpcEventPayloadEntry> payloadEntries)
	 {
		 StringBuilder payloadEntriesArray = new StringBuilder();
		 payloadEntriesArray.append("{");
		 for(HpcEventPayloadEntry payloadEntry : payloadEntries) {
			 if(payloadEntriesArray.length() > 1) {
				payloadEntriesArray.append(",");
			 }
			 payloadEntriesArray.append("\"" + payloadEntry.getAttribute() + "=" + payloadEntry.getValue() + "\"");
		 }
		 payloadEntriesArray.append("}");

		 return payloadEntriesArray.toString();
	 }
	 
	/** 
	 * Construct a payload entry object from string.
	 * 
	 * @param payloadEntryStr The payload entry string.
	 * @return payload entry object
	 */
	 private HpcEventPayloadEntry fromString(String payloadEntryStr)
	 {
		 HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
		 payloadEntry.setAttribute(payloadEntryStr.substring(0, payloadEntryStr.indexOf('=')));
		 payloadEntry.setValue(payloadEntryStr.substring(payloadEntryStr.indexOf('=') + 1));
		 return payloadEntry;
	 }
}

 