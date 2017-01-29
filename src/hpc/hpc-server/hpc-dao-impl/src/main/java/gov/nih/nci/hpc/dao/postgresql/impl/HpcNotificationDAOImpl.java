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
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

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
                    "\"USER_ID\", \"EVENT_TYPE\", \"NOTIFICATION_DELIVERY_METHODS\", \"NOTIFICATION_TRIGGERS\") " +
                    "values (?, ?, ?, ?) " +
            "on conflict(\"USER_ID\", \"EVENT_TYPE\") do update set " +
                    "\"NOTIFICATION_DELIVERY_METHODS\"=excluded.\"NOTIFICATION_DELIVERY_METHODS\"," +
                    "\"NOTIFICATION_TRIGGERS\"=excluded.\"NOTIFICATION_TRIGGERS\"";
	
	private static final String DELETE_SUBSCRIPTION_SQL = 
			"delete from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" " +
	                "where \"USER_ID\" = ? and \"EVENT_TYPE\" = ?";

	private static final String GET_SUBSCRIPTIONS_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ?";
	
	private static final String GET_SUBSCRIPTION_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ? and \"EVENT_TYPE\" = ?";

	private static final String GET_SUBSCRIPTION_USERS_SQL = 
		    "select \"USER_ID\" from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"EVENT_TYPE\" = ?";
	
	private static final String GET_SUBSCRIPTION_USERS_WITH_TRIGGER_SQL = 
			GET_SUBSCRIPTION_USERS_SQL + " and \"NOTIFICATION_TRIGGERS\" <@ ?::text[]";
	
	private static final String UPSERT_DELIVERY_RECEIPT_SQL = 
		    "insert into public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" ( " +
                    "\"EVENT_ID\", \"USER_ID\", \"NOTIFICATION_DELIVERY_METHOD\", \"DELIVERY_STATUS\", \"DELIVERED\") " +
                    "values (?, ?, ?, ?, ?) " +
            "on conflict(\"EVENT_ID\", \"USER_ID\", \"NOTIFICATION_DELIVERY_METHOD\") do update " +
                    "set \"DELIVERY_STATUS\"=excluded.\"DELIVERY_STATUS\", \"DELIVERED\"=excluded.\"DELIVERED\"";
	
	private static final String GET_DELIVERY_RECEIPTS_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" where \"USER_ID\" = ? " +
	        "order by \"EVENT_ID\" limit ? offset ?";
	
	private static final String GET_DELIVERY_RECEIPT_SQL = 
		    "select * from public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" where \"USER_ID\" = ? and \"EVENT_ID\" = ?";

	private static final String GET_DELIVERY_RECEIPTS_COUNT_SQL = 
		    "select count(*) from public.\"HPC_NOTIFICATION_DELIVERY_RECEIPT\" where \"USER_ID\" = ? ";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private HpcNotificationSubscriptionRowMapper notificationSubscriptionRowMapper = 
			                                     new HpcNotificationSubscriptionRowMapper();
	private HpcNotificationDeliveryReceiptRowMapper notificationDeliveryReceiptRowMapper = 
			                                     new HpcNotificationDeliveryReceiptRowMapper();
	private SingleColumnRowMapper<String> userIdRowMapper = new SingleColumnRowMapper<>();
	
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
		     jdbcTemplate.update(UPSERT_SUBSCRIPTION_SQL,
		    		             userId,
		    		             notificationSubscription.getEventType().value(),
		    		             deliveryMethodsToSQLArray(notificationSubscription.getNotificationDeliveryMethods()),
		                         payloadEntriesToSQLArray(notificationSubscription.getNotificationTriggers()));
		     
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
    public List<String> getSubscribedUsers(HpcEventType eventType) 
                                          throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_SUBSCRIPTION_USERS_SQL, userIdRowMapper, eventType.value());
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscribed users: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}				
    }
	
	@Override
    public List<String> getSubscribedUsers(HpcEventType eventType, List<HpcEventPayloadEntry> eventPayloadEntries) 
                                          throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_SUBSCRIPTION_USERS_WITH_TRIGGER_SQL, userIdRowMapper,
		    		                   eventType.value(), payloadEntriesToSQLTextArray(eventPayloadEntries));
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification subscribed users: " + 
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
		    	    	               HpcErrorType.DATABASE_ERROR, e);
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
		        throw new HpcException("Failed to get notification subscriptions: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
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
			String[] deliveryMethods = (String[]) rs.getArray("NOTIFICATION_DELIVERY_METHODS").getArray();
			for(String deliveryMethod : deliveryMethods) {
				notificationSubscription.getNotificationDeliveryMethods().add(
						    HpcNotificationDeliveryMethod.fromValue(deliveryMethod));
			}
            
            return notificationSubscription;
		}
	}
	
	// HpcNotificationDeliveryReceipt Row to Object mapper.
	private class HpcNotificationDeliveryReceiptRowMapper implements RowMapper<HpcNotificationDeliveryReceipt>
	{
		@Override
		public HpcNotificationDeliveryReceipt mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcNotificationDeliveryReceipt notificationDelivertReceipt = new HpcNotificationDeliveryReceipt();
        	Calendar delivered = new GregorianCalendar();
        	delivered.setTime(rs.getTimestamp("DELIVERED"));
			notificationDelivertReceipt.setDelivered(delivered);
			notificationDelivertReceipt.setDeliveryStatus(rs.getBoolean("DELIVERY_STATUS"));
			notificationDelivertReceipt.setEventId(rs.getInt("EVENT_ID"));
			notificationDelivertReceipt.setNotificationDeliveryMethod(
					     HpcNotificationDeliveryMethod.fromValue(rs.getString("NOTIFICATION_DELIVERY_METHOD")));
			notificationDelivertReceipt.setUserId(rs.getString("USER_ID"));
            
            return notificationDelivertReceipt;
		}
	}
	
    /** 
     * Map a collection of delivery methods to a SQL array.
     * 
     * @param deliveryMethods A list of delivery methods.
     * @return SQL array of text values.
     * @throws HpcException on SQL exception.
     */
	 private Array deliveryMethodsToSQLArray(List<HpcNotificationDeliveryMethod> deliveryMethods)
	                                        throws HpcException
	 {
		 String[] deliveryMethodsStr = new String[deliveryMethods.size()];
		 int i = 0;
		 for(HpcNotificationDeliveryMethod deliveryMethod : deliveryMethods) {
		     deliveryMethodsStr[i++] = deliveryMethod.value();
		 }
		 
		 try {
		      return jdbcTemplate.getDataSource().getConnection().createArrayOf("text", deliveryMethodsStr);
		      
		} catch(SQLException se) {
			    throw new HpcException("Failed to create SQL array of delivery methods: " + 
                                       se.getMessage(),
		                               HpcErrorType.DATABASE_ERROR, se);
		}
	 }
	 
    /** 
     * Map a collection of event payload entries to a SQL array.
     * 
     * @param payloadEntries A list of payload entries.
     * @return SQL array of text values.
     * @throws HpcException on SQL exception.
     */
	 private Array payloadEntriesToSQLArray(List<HpcEventPayloadEntry> payloadEntries)
	                                       throws HpcException
	 {
		 if(payloadEntries.isEmpty()) {
			return null;
		 }
		 
		 String[] payloadEntriesStr = new String[payloadEntries.size()];
		 int i = 0;
		 for(HpcEventPayloadEntry payloadEntry : payloadEntries) {
			 payloadEntriesStr[i++] = toText(payloadEntry);
		 }
		 
		 try {
		      return jdbcTemplate.getDataSource().getConnection().createArrayOf("text", payloadEntriesStr);
		      
		} catch(SQLException se) {
			    throw new HpcException("Failed to create SQL array of payload entries: " + 
                                       se.getMessage(),
		                               HpcErrorType.DATABASE_ERROR, se);
		}
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
			 if(payloadEntriesArray.length() > 0) {
				 payloadEntriesArray.append(",");
			 }
			 payloadEntriesArray.append(toText(payloadEntry));
		 }
		 payloadEntriesArray.append("}");

		 return payloadEntriesArray.toString();
	 }
	 
    /** 
     * Convert a payload entry to a string (to be stored in the DB)
     * 
     * @param payloadEntry A payload entry.
     * @return payload entry text.
     */
	 private String toText(HpcEventPayloadEntry payloadEntry)
	 {
		 return payloadEntry.getAttribute() + "=" + payloadEntry.getValue();
	 }
}

 