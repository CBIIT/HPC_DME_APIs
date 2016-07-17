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
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
	
	private static final String INSERT_EVENT_SQL = 
			"insert into public.\"HPC_EVENT\" ( " +
	                "\"USER_ID\", \"TYPE\", \"PAYLOAD\", \"CREATED\") " +
	                "values (?, ?, ?, ?)"; 
	
	private static final String GET_EVENTS_SQL = 
		    "select * from public.\"HPC_EVENT\"";
	
	private static final String DELETE_EVENT_SQL = 
		    "delete from public.\"HPC_EVENT\" where \"ID\" = ?";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;
	
	// Row mappers.
	private HpcNotificationSubscriptionRowMapper notificationSubscriptionRowMapper = 
			                                     new HpcNotificationSubscriptionRowMapper();
	private HpcEventRowMapper eventRowMapper = new HpcEventRowMapper();
	
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
	public void insertEvent(HpcEvent event) throws HpcException
	{
		try {
		     jdbcTemplate.update(INSERT_EVENT_SQL,
		    		             event.getUserId(),
		    		             event.getType().value(),
		    		             toJSON(event.getNotificationPayloadEntries()),
		    		             event.getCreated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to insert an event " + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
	}
	
	@Override
    public List<HpcEvent> getEvents() throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_EVENTS_SQL, eventRowMapper);
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get notification events: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	    	
    }
    
    @Override
    public void deleteEvent(int eventId) throws HpcException
    {
		try {
		     jdbcTemplate.update(DELETE_EVENT_SQL, eventId);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a notification event " + 
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
	
	// HpcNotificationEvent Row to Object mapper.
	private class HpcEventRowMapper implements RowMapper<HpcEvent>
	{
		@Override
		public HpcEvent mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcEvent event = new HpcEvent();
			event.setId(rs.getInt("ID"));
			event.setUserId(rs.getString("USER_ID"));
			event.setType(HpcEventType.fromValue(rs.getString("TYPE")));
			event.getNotificationPayloadEntries().addAll(fromJSON(rs.getString("PAYLOAD")));
			
        	Calendar created = new GregorianCalendar();
        	created.setTime(rs.getDate("CREATED"));
        	event.setCreated(created);
            
            return event;
		}
	}
	
    /** 
     * Convert payload entries into a JSON string
     * 
     * @param payloadEntries List of payload entries.
     * @return A JSON representation of the payload entries.
     */
	@SuppressWarnings("unchecked")
	private String toJSON(List<HpcNotificationPayloadEntry> payloadEntries)
	{
		if(payloadEntries == null || payloadEntries.isEmpty()) {
		   return "";
		}
		
		JSONObject jsonPayload = new JSONObject();
		for(HpcNotificationPayloadEntry payloadEntry : payloadEntries) {
			jsonPayload.put(payloadEntry.getAttribute(), payloadEntry.getValue());
		}
		
		return jsonPayload.toJSONString();
	}
	  
    /** 
     * Convert JSON string to payload entries.
     * 
     * @param jsonPayloadStr The Payload Entries JSON String.
     * @return List<HpcNotificationPayloadEntry>
     */
	private List<HpcNotificationPayloadEntry> fromJSON(String jsonPayloadStr)
	{
		List<HpcNotificationPayloadEntry> payloadEntries = new ArrayList<>();
		if(jsonPayloadStr == null || jsonPayloadStr.isEmpty()) {
		   return payloadEntries;
		}
		
		// Parse the JSON string.
		JSONObject jsonPayload = null;
		try {
			 jsonPayload = (JSONObject) (new JSONParser().parse(jsonPayloadStr));
			 
		} catch(ParseException e) {
			    return payloadEntries;
		}
		
		// Map all attributes to payload entries.
		for(Object attribue : jsonPayload.keySet()) {
			HpcNotificationPayloadEntry entry = new HpcNotificationPayloadEntry();
			entry.setAttribute(attribue.toString());
			entry.setValue(jsonPayload.get(attribue).toString());
			payloadEntries.add(entry);
		}

		return payloadEntries;
	}	  
}

 