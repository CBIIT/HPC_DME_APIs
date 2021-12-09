/**
 * HpcNotificationFormatter.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationFormat;
import gov.nih.nci.hpc.domain.notification.HpcNotificationFormatArgument;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Format text for event notifications.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcNotificationFormatter {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Default Payload entry value.
	private static final String DEFAULT_PAYLOAD_ENTRY_VALUE = "<N/A>";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// A map of notification formats.
	private Map<String, HpcNotificationFormat> notificationFormats = new HashMap<>();

	private Map<HpcSystemAdminNotificationType, HpcNotificationFormat> systemAdminNotificationFormats = new HashMap<>();

	private String defaultBaseUiURL = null;
	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Default Constructor.
	 *
	 * @throws HpcException Constructor is disabled.
	 */
	@SuppressWarnings("unused")
	private HpcNotificationFormatter() throws HpcException {
		throw new HpcException("Constructor Disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
	}

	/**
	 * Constructor for Spring Dependency Injection.
	 *
	 * @param notificationFormatPath The path to the notification formats JSON file.
	 * @param defaultBaseUiURL       The DME UI base URL.
	 * @throws HpcException on spring configuration error.
	 */
	public HpcNotificationFormatter(String notificationFormatPath, String defaultBaseUiURL) throws HpcException {
		initNotificationFormats(notificationFormatPath);
		this.defaultBaseUiURL = defaultBaseUiURL;
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Generate a notification text message for an event.
	 *
	 * @param eventType      The event type to generate the text for.
	 * @param payloadEntries The payload entries to use for the format arguments.
	 * @param doc			 The doc specific template to be used for this user. (Optional)
	 * @return A notification text message.
	 * @throws HpcException on service failure.
	 */
	public String formatText(HpcEventType eventType, String doc, List<HpcEventPayloadEntry> payloadEntries) throws HpcException {
		// Find the format for the event type
		HpcNotificationFormat format = getFormat(eventType, doc, payloadEntries);

		// Add environment specific URL
		HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute("BASE_UI_URL");
		payloadEntry.setValue(defaultBaseUiURL);
		payloadEntries.add(payloadEntry);

		return format(format.getTextFormat(), format.getTextArguments(), payloadEntries);
	}

	/**
	 * Generate a notification text message for system admin notification type.
	 *
	 * @param notificationType The system admin notification type to generate the
	 *                         text for.
	 * @param payloadEntries   The payload entries to use for the format arguments.
	 * @return A notification text message.
	 * @throws HpcException on service failure.
	 */
	public String formatText(HpcSystemAdminNotificationType notificationType, List<HpcEventPayloadEntry> payloadEntries)
			throws HpcException {
		// Find the format for the event type
		HpcNotificationFormat format = systemAdminNotificationFormats.get(notificationType);
		if (format == null) {
			throw new HpcException("Notification format not found for: " + notificationType,
					HpcErrorType.UNEXPECTED_ERROR);
		}

		return format(format.getTextFormat(), format.getTextArguments(), payloadEntries);
	}

	/**
	 * Generate a notification subject for an event.
	 *
	 * @param eventType      The event type to generate the subject for.
	 * @param doc			 The doc specific template to be used for this user. (Optional)
	 * @param payloadEntries The payload entries to use for the format arguments.
	 * @return A notification text message.
	 * @throws HpcException on service failure.
	 */
	public String formatSubject(HpcEventType eventType, String doc, List<HpcEventPayloadEntry> payloadEntries) throws HpcException {
		// Find the format for the event type
		HpcNotificationFormat format = getFormat(eventType, doc, payloadEntries);
		return format(format.getSubjectFormat(), format.getSubjectArguments(), payloadEntries);
	}

	/**
	 * Generate a notification subject for an event.
	 *
	 * @param notificationType The system admin notification type to generate the
	 *                         subject for.
	 * @param payloadEntries   The payload entries to use for the format arguments.
	 * @return A notification text message.
	 * @throws HpcException on service failure.
	 */
	public String formatSubject(HpcSystemAdminNotificationType notificationType,
			List<HpcEventPayloadEntry> payloadEntries) throws HpcException {
		// Find the format for the event type
		HpcNotificationFormat format = systemAdminNotificationFormats.get(notificationType);
		if (format == null) {
			throw new HpcException("Notification format not found for: " + notificationType,
					HpcErrorType.UNEXPECTED_ERROR);
		}

		return format(format.getSubjectFormat(), format.getSubjectArguments(), payloadEntries);
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Generate a formatted string from a format and argument.
	 *
	 * @param format              The format.
	 * @param formatArguments     The arguments.
	 * @param eventPayloadEntries The event payload entries.
	 * @return A formatted string.
	 */
	private String format(String format, List<HpcNotificationFormatArgument> formatArguments,
			List<HpcEventPayloadEntry> eventPayloadEntries) {
		// Prepare the array of format arguments with values from the event
		ArrayList<String> arguments = new ArrayList<>(formatArguments.size());
		for (HpcNotificationFormatArgument argument : formatArguments) {
			arguments.add(argument.getIndex(),
					getPayloadEntryValue(eventPayloadEntries, argument.getPayloadEntryAttribute()));
		}

		// Return a formatted message.
		return String.format(format, arguments.toArray());
	}

	/**
	 * Instantiate a notification format argument object from JSON.
	 *
	 * @param jsonNotificationFormatArgument The notification format argument JSON
	 * @return HpcNotificationFormatArgument A notification format argument object.
	 * @throws HpcException If failed to parse the JSON
	 */
	private HpcNotificationFormatArgument notificationFormatArgumentFromJSON(JSONObject jsonNotificationFormatArgument)
			throws HpcException {
		if (!jsonNotificationFormatArgument.containsKey("index")
				|| !jsonNotificationFormatArgument.containsKey("payloadEntryAttribute")) {
			throw new HpcException(
					"Invalid notification format argument JSON object: " + jsonNotificationFormatArgument,
					HpcErrorType.SPRING_CONFIGURATION_ERROR);
		}

		// JSON -> POJO.
		HpcNotificationFormatArgument notificationFormatArgument = new HpcNotificationFormatArgument();
		notificationFormatArgument.setIndex(Integer.valueOf((String) jsonNotificationFormatArgument.get("index")));
		notificationFormatArgument
				.setPayloadEntryAttribute((String) jsonNotificationFormatArgument.get("payloadEntryAttribute"));

		return notificationFormatArgument;
	}

	/**
	 * Instantiate a list of notification format argument objects from JSON.
	 *
	 * @param jsonNotificationFormatArguments The notification format arguments JSON
	 *                                        array.
	 * @return A list of notification format argument objects.
	 * @throws HpcException If failed to parse the JSON.
	 */
	private List<HpcNotificationFormatArgument> notificationFormatArgumentsFromJSON(
			JSONArray jsonNotificationFormatArguments) throws HpcException {
		List<HpcNotificationFormatArgument> arguments = new ArrayList<>();
		if (jsonNotificationFormatArguments != null && jsonNotificationFormatArguments.size() > 0) {
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> jsonArgumentIterator = jsonNotificationFormatArguments.iterator();
			while (jsonArgumentIterator.hasNext()) {
				arguments.add(notificationFormatArgumentFromJSON(jsonArgumentIterator.next()));
			}

			// Validate arguments index was set correctly.
			int maxIndex = 0;
			for (HpcNotificationFormatArgument argument : arguments) {
				if (maxIndex < argument.getIndex()) {
					maxIndex = argument.getIndex();
				}
			}
			if (maxIndex + 1 != arguments.size()) {
				throw new HpcException("Invalid arguments index: " + jsonNotificationFormatArguments,
						HpcErrorType.SPRING_CONFIGURATION_ERROR);
			}
		}

		return arguments;
	}

	/**
	 * Instantiate a notification format object from JSON.
	 *
	 * @param jsonNotificationFormat The notification format JSON
	 * @return HpcNotificationFormat A notification format object.
	 * @throws HpcException If failed to parse the JSON.
	 */
	private HpcNotificationFormat notificationFormatFromJSON(JSONObject jsonNotificationFormat) throws HpcException {
		if (!jsonNotificationFormat.containsKey("subjectFormat")
				|| !jsonNotificationFormat.containsKey("subjectArguments")
				|| !jsonNotificationFormat.containsKey("textFormat")
				|| !jsonNotificationFormat.containsKey("textArguments")) {
			throw new HpcException("Invalid notification format JSON object: " + jsonNotificationFormat,
					HpcErrorType.SPRING_CONFIGURATION_ERROR);
		}

		// JSON -> POJO.
		HpcNotificationFormat notificationFormat = new HpcNotificationFormat();
		notificationFormat.setSubjectFormat((String) jsonNotificationFormat.get("subjectFormat"));
		notificationFormat.getSubjectArguments().addAll(
				notificationFormatArgumentsFromJSON((JSONArray) jsonNotificationFormat.get("subjectArguments")));
		notificationFormat.setTextFormat((String) jsonNotificationFormat.get("textFormat"));
		notificationFormat.getTextArguments()
				.addAll(notificationFormatArgumentsFromJSON((JSONArray) jsonNotificationFormat.get("textArguments")));

		return notificationFormat;
	}

	/**
	 * Open and parse the notification formats JSON file.
	 *
	 * @param notificationFormatPath The path to the notification formats JSON.
	 * @throws HpcException on service failure.
	 */
	private void initNotificationFormats(String notificationFormatPath) throws HpcException {
		// Open and Parse the notification formats JSON file.
		JSONArray jsonNotificationFormats = null;
		try (FileReader reader = new FileReader(notificationFormatPath)) {
			jsonNotificationFormats = (JSONArray) ((JSONObject) new JSONParser().parse(reader))
					.get("notificationFormats");

		} catch (Exception e) {
			throw new HpcException("Could not open or parse: " + notificationFormatPath,
					HpcErrorType.SPRING_CONFIGURATION_ERROR, e);
		}

		// Validate formats are defined.
		if (jsonNotificationFormats == null || jsonNotificationFormats.isEmpty()) {
			throw new HpcException("No notification formats found in JSON file",
					HpcErrorType.SPRING_CONFIGURATION_ERROR);
		}

		// Iterate through the list of formats and populate the map.
		@SuppressWarnings("unchecked")
		Iterator<JSONObject> jsonNotificationFormatIterator = jsonNotificationFormats.iterator();
		while (jsonNotificationFormatIterator.hasNext()) {
			JSONObject jsonNotificationFormat = jsonNotificationFormatIterator.next();

			// Extract the event type.
			String eventTypeStr = (String) jsonNotificationFormat.get("eventType");
			String doc = (String) jsonNotificationFormat.get("doc");
			String systemAdminNotificationTypeStr = (String) jsonNotificationFormat.get("systemAdminNotificationType");
			if (StringUtils.isEmpty(eventTypeStr) && StringUtils.isEmpty(systemAdminNotificationTypeStr)) {
				throw new HpcException("Invalid event type / system admin notification type: " + jsonNotificationFormat,
						HpcErrorType.SPRING_CONFIGURATION_ERROR);
			}

			HpcNotificationFormat notificationFormat = notificationFormatFromJSON(jsonNotificationFormat);
			if (eventTypeStr != null) {
				// Populate the map <eventType -> notificationFormat>
				if(StringUtils.isEmpty(doc))
					notificationFormats.put(eventTypeStr, notificationFormat);
				else
					notificationFormats.put(eventTypeStr + "_" + doc, notificationFormat);
			} else {
				// Populate the map <systemAdminNotificationType -> notificationFormat>
				systemAdminNotificationFormats.put(
						HpcSystemAdminNotificationType.valueOf(systemAdminNotificationTypeStr), notificationFormat);
			}
		}
	}

	/**
	 * Get notification payload entry value.
	 *
	 * @param eventPayloadEntries The event payload entries.
	 * @param attribute           The payload entry attribute to find.
	 * @return The payload entry value.
	 */
	private String getPayloadEntryValue(List<HpcEventPayloadEntry> eventPayloadEntries, String attribute) {
		if (eventPayloadEntries == null) {
			return DEFAULT_PAYLOAD_ENTRY_VALUE;
		}

		for (HpcEventPayloadEntry payloadEntry : eventPayloadEntries) {
			if (payloadEntry.getAttribute().equals(attribute)) {
				return payloadEntry.getValue();
			}
		}

		return DEFAULT_PAYLOAD_ENTRY_VALUE;
	}
	
	/**
	 * Get format template
	 *
	 * @param eventType      The event type to obtain the format for.
	 * @param doc			 The doc specific template to be used for this user. (Optional)
	 * @param payloadEntries The payload entries to use for looking up specific template.
	 * @return The format template
	 * @throws HpcException If failed to find the format.
	 */
	private HpcNotificationFormat getFormat(HpcEventType eventType, String doc,
			List<HpcEventPayloadEntry> payloadEntries) throws HpcException {
		HpcNotificationFormat format = null;
		// For collection updates, try to find update specific templates
		if (eventType.equals(HpcEventType.COLLECTION_UPDATED)) {
			HpcEventPayloadEntry updateEntry = payloadEntries.stream()
					.filter(entry -> "UPDATE".equals(entry.getAttribute())).findAny().orElse(null);
			// Try to find doc specific template first
			format = notificationFormats.get(eventType.toString() + "_" + updateEntry.getValue() + "_" + doc);
			if (format != null)
				return format;
			// Try to find template without doc
			format = notificationFormats.get(eventType.toString() + "_" + updateEntry.getValue());
			if (format != null)
				return format;
		}
		// Find the doc specific format for the event type
		format = notificationFormats.get(eventType.toString() + "_" + doc);
		if (format != null)
			return format;
		// Find the format for the event type
		format = notificationFormats.get(eventType.toString());
		if (format == null)
			throw new HpcException("Notification format not found for: " + eventType, HpcErrorType.UNEXPECTED_ERROR);
		return format;
	}
}
