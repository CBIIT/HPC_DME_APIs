/**
 * HpcNotificationDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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

public class HpcNotificationDAOImpl implements HpcNotificationDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPSERT_SUBSCRIPTION_SQL = "merge into HPC_NOTIFICATION_SUBSCRIPTION using dual on (USER_ID = ? and EVENT_TYPE = ?) "
			+ "when matched then update set NOTIFICATION_DELIVERY_METHODS = ? "
			+ "when not matched then insert (USER_ID, EVENT_TYPE, NOTIFICATION_DELIVERY_METHODS) values (?, ?, ?)";

	private static final String DELETE_SUBSCRIPTION_SQL = "delete from HPC_NOTIFICATION_SUBSCRIPTION "
			+ "where USER_ID = ? and EVENT_TYPE = ?";

	private static final String GET_SUBSCRIPTIONS_SQL = "select * from HPC_NOTIFICATION_SUBSCRIPTION where USER_ID = ?";

	private static final String GET_SUBSCRIPTION_SQL = "select * from HPC_NOTIFICATION_SUBSCRIPTION where USER_ID = ? and EVENT_TYPE = ?";

	private static final String GET_SUBSCRIPTION_ID_SQL = "select ID from HPC_NOTIFICATION_SUBSCRIPTION where USER_ID = ? and EVENT_TYPE = ?";

	private static final String INSERT_TRIGGER_SQL = "insert into HPC_NOTIFICATION_TRIGGER ( "
			+ "NOTIFICATION_SUBSCRIPTION_ID, NOTIFICATION_TRIGGER) values (?, ?)";

	private static final String GET_TRIGGER_SQL = "select * from HPC_NOTIFICATION_TRIGGER where NOTIFICATION_SUBSCRIPTION_ID = ?";

	private static final String DELETE_TRIGGER_SQL = "delete from HPC_NOTIFICATION_TRIGGER where NOTIFICATION_SUBSCRIPTION_ID = ?";

	private static final String GET_SUBSCRIBED_USERS_SQL = "select USER_ID from HPC_NOTIFICATION_SUBSCRIPTION where EVENT_TYPE = ?";

	private static final String GET_SUBSCRIBED_USERS_WITH_TRIGGER_SQL = "select s.USER_ID, t.NOTIFICATION_TRIGGER from HPC_NOTIFICATION_SUBSCRIPTION s "
			+ "join HPC_NOTIFICATION_TRIGGER t on s.ID = t.NOTIFICATION_SUBSCRIPTION_ID where s.EVENT_TYPE = ? ";

	private static final String UPSERT_DELIVERY_RECEIPT_SQL = "merge into HPC_NOTIFICATION_DELIVERY_RECEIPT using dual on (EVENT_ID = ? and USER_ID = ? and NOTIFICATION_DELIVERY_METHOD = ?) "
			+ "when matched then update set DELIVERY_STATUS = ?, DELIVERED = ? "
			+ "when not matched then insert (EVENT_ID, USER_ID, NOTIFICATION_DELIVERY_METHOD, DELIVERY_STATUS, DELIVERED) "
			+ "values (?, ?, ?, ?, ?) ";

	private static final String GET_DELIVERY_RECEIPTS_SQL = "select * from HPC_NOTIFICATION_DELIVERY_RECEIPT where USER_ID = ? "
			+ "order by EVENT_ID desc offset ? rows fetch next ? rows only";

	private static final String GET_DELIVERY_RECEIPT_SQL = "select * from HPC_NOTIFICATION_DELIVERY_RECEIPT where USER_ID = ? and EVENT_ID = ?";

	private static final String GET_DELIVERY_RECEIPTS_COUNT_SQL = "select count(*) from HPC_NOTIFICATION_DELIVERY_RECEIPT where USER_ID = ? ";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	// TODO: Remove after Oracle migration
	@Qualifier("hpcOracleJdbcTemplate")
	// TODO: END
	private JdbcTemplate jdbcTemplate = null;

	// Row mappers.
	private RowMapper<HpcNotificationSubscription> notificationSubscriptionRowMapper = (rs, rowNum) -> {
		HpcNotificationSubscription notificationSubscription = new HpcNotificationSubscription();
		notificationSubscription.setId(rs.getInt("ID"));
		notificationSubscription.setEventType(HpcEventType.fromValue(rs.getString("EVENT_TYPE")));

		String deliveryMethods = rs.getString("NOTIFICATION_DELIVERY_METHODS");
		for (String deliveryMethod : deliveryMethods.split(",")) {
			notificationSubscription.getNotificationDeliveryMethods()
					.add(HpcNotificationDeliveryMethod.fromValue(deliveryMethod));
		}

		return notificationSubscription;
	};
	private RowMapper<HpcNotificationTrigger> notificationTriggerRowMapper = (rs, rowNum) -> {
		return fromNotificationTriggerString(rs.getString("NOTIFICATION_TRIGGER"));
	};
	private RowMapper<HpcNotificationDeliveryReceipt> notificationDeliveryReceiptRowMapper = (rs, rowNum) -> {
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
	private RowMapper<HpcUserTrigger> userTriggerRowMapper = (rs, rowNum) -> {
		HpcUserTrigger userTrigger = new HpcUserTrigger();
		userTrigger.notificationTrigger = fromNotificationTriggerString(rs.getString("NOTIFICATION_TRIGGER"));
		userTrigger.userId = rs.getString("USER_ID");
		return userTrigger;
	};
	private SingleColumnRowMapper<String> userIdRowMapper = new SingleColumnRowMapper<>();
	private SingleColumnRowMapper<BigDecimal> notificationIdRowMapper = new SingleColumnRowMapper<>();

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcNotificationDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcNotificationDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	@Transactional
	public void upsertSubscription(String userId, HpcNotificationSubscription notificationSubscription)
			throws HpcException {
		try {
			String eventType = notificationSubscription.getEventType().value();
			String deliveryMethods = toDeliveryMethodsString(notificationSubscription.getNotificationDeliveryMethods());
			jdbcTemplate.update(UPSERT_SUBSCRIPTION_SQL, userId, eventType, deliveryMethods, userId, eventType,
					deliveryMethods);

			// Update the notification triggers.
			BigDecimal notificationId = jdbcTemplate.queryForObject(GET_SUBSCRIPTION_ID_SQL, notificationIdRowMapper,
					userId, eventType);
			jdbcTemplate.update(DELETE_TRIGGER_SQL, notificationId);
			for (HpcNotificationTrigger trigger : notificationSubscription.getNotificationTriggers()) {
				jdbcTemplate.update(INSERT_TRIGGER_SQL, notificationId,
						toEventPayloadEntriesString(trigger.getPayloadEntries()));
			}

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a notification subscription: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteSubscription(String userId, HpcEventType eventType) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_SUBSCRIPTION_SQL, userId, eventType.value());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a notification subscription: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcNotificationSubscription> getSubscriptions(String userId) throws HpcException {
		try {
			// Get the subscriptions.
			List<HpcNotificationSubscription> subscriptions = jdbcTemplate.query(GET_SUBSCRIPTIONS_SQL,
					notificationSubscriptionRowMapper, userId);

			// Set the triggers.
			for (HpcNotificationSubscription subscription : subscriptions) {
				subscription.getNotificationTriggers().addAll(
						jdbcTemplate.query(GET_TRIGGER_SQL, notificationTriggerRowMapper, subscription.getId()));
			}

			return subscriptions;

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get notification subscriptions: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcNotificationSubscription getSubscription(String userId, HpcEventType eventType) throws HpcException {
		try {
			// Get the subscription.
			HpcNotificationSubscription subscription = jdbcTemplate.queryForObject(GET_SUBSCRIPTION_SQL,
					notificationSubscriptionRowMapper, userId, eventType.value());

			// Set the triggers.
			subscription.getNotificationTriggers()
					.addAll(jdbcTemplate.query(GET_TRIGGER_SQL, notificationTriggerRowMapper, subscription.getId()));

			return subscription;

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get notification subscription: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}

	}

	@Override
	public List<String> getSubscribedUsers(HpcEventType eventType) throws HpcException {
		try {
			return jdbcTemplate.query(GET_SUBSCRIBED_USERS_SQL, userIdRowMapper, eventType.value());

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get notification subscribed users: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<String> getSubscribedUsers(HpcEventType eventType, List<HpcEventPayloadEntry> eventPayloadEntries)
			throws HpcException {
		try {
			Map<String, String> eventPayloadMap = new Hashtable<>();
			eventPayloadEntries.forEach(eventPayloadEntry -> eventPayloadMap.put(eventPayloadEntry.getAttribute(),
					eventPayloadEntry.getValue()));

			HashSet<String> userIds = new HashSet<>();
			jdbcTemplate.query(GET_SUBSCRIBED_USERS_WITH_TRIGGER_SQL, userTriggerRowMapper, eventType.value())
					.forEach(userTrigger -> {
						boolean match = true;

						for (HpcEventPayloadEntry triggerPayloadEntry : userTrigger.notificationTrigger
								.getPayloadEntries()) {
							String value = eventPayloadMap.get(triggerPayloadEntry.getAttribute());
							if (value == null || !value.equals(triggerPayloadEntry.getValue())) {
								match = false;
								break;
							}
						}

						if (match) {
							userIds.add(userTrigger.userId);
						}
					});
			return new ArrayList<String>(userIds);

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get notification subscribed users: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void upsertDeliveryReceipt(HpcNotificationDeliveryReceipt deliveryReceipt) throws HpcException {
		try {
			jdbcTemplate.update(UPSERT_DELIVERY_RECEIPT_SQL, deliveryReceipt.getEventId(), deliveryReceipt.getUserId(),
					deliveryReceipt.getNotificationDeliveryMethod().value(), deliveryReceipt.getDeliveryStatus(),
					deliveryReceipt.getDelivered(), deliveryReceipt.getEventId(), deliveryReceipt.getUserId(),
					deliveryReceipt.getNotificationDeliveryMethod().value(), deliveryReceipt.getDeliveryStatus(),
					deliveryReceipt.getDelivered());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a notification delivery receipt: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcNotificationDeliveryReceipt> getDeliveryReceipts(String userId, int offset, int limit)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_DELIVERY_RECEIPTS_SQL, notificationDeliveryReceiptRowMapper, userId, offset,
					limit);

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get notification subscriptions: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcNotificationDeliveryReceipt getDeliveryReceipt(String userId, int eventId) throws HpcException {
		try {
			List<HpcNotificationDeliveryReceipt> receipts = jdbcTemplate.query(GET_DELIVERY_RECEIPT_SQL,
					notificationDeliveryReceiptRowMapper, userId, eventId);
			if (receipts != null && receipts.size() > 0)
				return receipts.get(0);
			else
				return null;

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get notification subscriptions: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getDeliveryReceiptsCount(String userId) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DELIVERY_RECEIPTS_COUNT_SQL, Integer.class, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count notification delivery receipts: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	// Prepared query.
	private class HpcUserTrigger {
		private String userId = null;
		private HpcNotificationTrigger notificationTrigger = null;
	}

	/**
	 * Map a collection of delivery methods to a comma separated string
	 * 
	 * @param deliveryMethods A list of delivery methods.
	 * @return comma separated string
	 */
	private String toDeliveryMethodsString(List<HpcNotificationDeliveryMethod> deliveryMethods) {
		StringBuilder deliveryMethodsStr = new StringBuilder();
		deliveryMethods.forEach(deliveryMethod -> deliveryMethodsStr.append(deliveryMethod.value() + ","));
		return deliveryMethodsStr.toString();
	}

	/**
	 * Map a collection of event payload entries to a SQL text array (as a String).
	 * 
	 * @param payloadEntries A list of payload entries.
	 * @return SQL text array string.
	 */
	private String toEventPayloadEntriesString(List<HpcEventPayloadEntry> payloadEntries) {
		StringBuilder payloadEntriesStr = new StringBuilder();
		payloadEntries.forEach(payloadEntry -> payloadEntriesStr
				.append(payloadEntry.getAttribute() + "=" + payloadEntry.getValue() + ","));
		return payloadEntriesStr.toString();
	}

	/**
	 * Construct a HpcNotificationTrigger object from string.
	 * 
	 * @param notificationTriggerStr The notification trigger string.
	 * @return HpcNotificationTrigger object
	 */
	private HpcNotificationTrigger fromNotificationTriggerString(String notificationTriggerStr) {
		HpcNotificationTrigger notificationTrigger = new HpcNotificationTrigger();
		for (String trigger : notificationTriggerStr.split(",")) {
			HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
			payloadEntry.setAttribute(trigger.substring(0, trigger.indexOf('=')));
			payloadEntry.setValue(trigger.substring(trigger.indexOf('=') + 1));
			notificationTrigger.getPayloadEntries().add(payloadEntry);
		}

		return notificationTrigger;
	}
}
