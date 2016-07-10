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
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationType;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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
	public static final String UPSERT_SQL = 
		   "insert into public.\"HPC_NOTIFICATION_SUBSCRIPTION\" ( " +
                   "\"USER_ID\", \"NOTIFICATION_TYPE\", \"NOTIFICATION_DELIVERY_METHODS\") " +
                   "values (?, ?, ?) " +
           "on conflict(\"USER_ID\", \"NOTIFICATION_TYPE\") do update " +
                   "set \"NOTIFICATION_DELIVERY_METHODS\"=excluded.\"NOTIFICATION_DELIVERY_METHODS\"";

	public static final String GET_SQL = "select * from public.\"HPC_NOTIFICATION_SUBSCRIPTION\" where \"USER_ID\" = ?";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;
	
	// Row mapper.
	private HpcNotificationSubscriptionRowMapper rowMapper = new HpcNotificationSubscriptionRowMapper();
	
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
	public void upsert(String userId,
			           HpcNotificationSubscription notificationSubscription) throws HpcException
    {
		try {
			 // Map the delivery methods to a single string.
			 StringBuilder deliveryMethods = new StringBuilder();
			 for(HpcNotificationDeliveryMethod deliveryMethod : 
				 notificationSubscription.getNotificationDeliveryMethods()) {
				 deliveryMethods.append(deliveryMethod.value() + " ");
			 }
			 
		     jdbcTemplate.update(UPSERT_SQL,
		    		             userId,
		    		             notificationSubscription.getNotificationType().value(),
		    		             deliveryMethods.toString());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a notification subscription: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  

	// HpcNotificationSubscription Table to Object mapper.
	private class HpcNotificationSubscriptionRowMapper implements RowMapper<HpcNotificationSubscription>
	{
		@Override
		public HpcNotificationSubscription mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcNotificationSubscription notificationSubscription = new HpcNotificationSubscription();
			notificationSubscription.setNotificationType(HpcNotificationType.fromValue(rs.getString("NOTIFICATION_TYPE")));
			String deliveryMethods = rs.getString("NOTIFICATION_DELIVERY_METHODS");
			for(String deliveryMethod : deliveryMethods.split(" ")) {
				notificationSubscription.getNotificationDeliveryMethods().add(
						    HpcNotificationDeliveryMethod.fromValue(deliveryMethod));
			}
            
            return notificationSubscription;
		}
	}
}

 