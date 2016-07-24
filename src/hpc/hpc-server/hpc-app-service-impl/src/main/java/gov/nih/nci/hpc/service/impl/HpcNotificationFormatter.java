/**
 * HpcNotificationFormatter.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationFormat;
import gov.nih.nci.hpc.domain.notification.HpcNotificationFormatArgument;
import gov.nih.nci.hpc.exception.HpcException;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * <p>
 * Format text for event notifications.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNotificationFormatter
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    // Default Payload entry value.
	private static final String DEFAULT_PAYLOAD_ENTRY_VALUE = "<N/A>";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// A map of notification formats.
	private Map<HpcEventType, HpcNotificationFormat> notificationFormats = new HashMap<>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    @SuppressWarnings("unused")
	private HpcNotificationFormatter() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param notificationFormatPath The path to the notification formats JSON file.
     * 
     * @throws HpcException
     */
    public HpcNotificationFormatter(String notificationFormatPath) throws HpcException
    {
		initNotificationFormats(notificationFormatPath);
    }		
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Generate a notification text message for an event.
     *
     * @param event The event to generate the text for.
     * @return A notification text message.
     * @throws HpcException
     */
    public String formatText(HpcEvent event) throws HpcException
    {
    	// Find the format for the event type
    	HpcNotificationFormat format = notificationFormats.get(event.getType());
    	if(format == null) {
    	   throw new HpcException("Notification format not found for: " + event.getType(),
    			                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
		return format(format.getTextFormat(), format.getTextArguments(),
				      event.getPayloadEntries());
    }
    
    /**
     * Generate a notification subject for an event.
     *
     * @param event The event to generate the text for.
     * @return A notification text message.
     * @throws HpcException
     */
    public String formatSubject(HpcEvent event) throws HpcException
    {
    	// Find the format for the event type
    	HpcNotificationFormat format = notificationFormats.get(event.getType());
    	if(format == null) {
    	   throw new HpcException("Notification format not found for: " + event.getType(),
    			                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
		return format(format.getSubjectFormat(), format.getSubjectArguments(),
				      event.getPayloadEntries());
    }
    		                               
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Generate a formatted string from a format and argument.
     *
     * @param format The format.
     * @param formatArguments The arguments.
     * @param eventPayloadEntries The event payload entries.
     * @return A formatted string.
     * @throws HpcException
     */
    private String format(String format, List<HpcNotificationFormatArgument> formatArguments,
    		              List<HpcEventPayloadEntry> eventPayloadEntries) 
    		             throws HpcException
    {
    	// Prepare the array of format arguments with values from the event 
    	ArrayList<String> arguments = new ArrayList<>(formatArguments.size());
		for(HpcNotificationFormatArgument argument : formatArguments) {
			arguments.add(argument.getIndex(), 
					      getPayloadEntryValue(eventPayloadEntries, 
					    		               argument.getPayloadEntryAttribute()));
		}
		
		// Return a formatted message.
		return String.format(format, arguments.toArray());
    }
    
    /**
     * Instantiate a notification format argument object from JSON.
     *
     * @param jsonNotificationFormatArgument The notification format argument JSON
     * @return HpcNotificationFormatArgument A notification format argument object.
     * 
     * @throws HpcException If failed to parse the JSON
     */
	private HpcNotificationFormatArgument notificationFormatArgumentFromJSON(
			                                          JSONObject jsonNotificationFormatArgument) 
    		                                          throws HpcException
    {
    	if(!jsonNotificationFormatArgument.containsKey("index") ||
	       !jsonNotificationFormatArgument.containsKey("payloadEntryAttribute")) {
	       throw new HpcException("Invalid notification format argument JSON object: " + 
	                              jsonNotificationFormatArgument,
	    	                      HpcErrorType.SPRING_CONFIGURATION_ERROR);	
    	}
	    			
    	// JSON -> POJO.
    	HpcNotificationFormatArgument notificationFormatArgument = new HpcNotificationFormatArgument();
    	notificationFormatArgument.setIndex(Integer.valueOf((String) jsonNotificationFormatArgument.get("index")));
    	notificationFormatArgument.setPayloadEntryAttribute((String) jsonNotificationFormatArgument.get("payloadEntryAttribute"));
    	
    	return notificationFormatArgument;
    }
	
    /**
     * Instantiate a list of notification format argument objects from JSON.
     *
     * @param jsonNotificationFormatArguments The notification format arguments JSON array.
     * @return List<HpcNotificationFormatArgument>  A list of notification format argument objects.
     * 
     * @throws HpcException If failed to parse the JSON
     */
	private List<HpcNotificationFormatArgument> 
	        notificationFormatArgumentsFromJSON(JSONArray jsonNotificationFormaArguments) 
	        		                           throws HpcException
	{	
		List<HpcNotificationFormatArgument> arguments = new ArrayList<>();
	    if(jsonNotificationFormaArguments != null && 
	       jsonNotificationFormaArguments.size() > 0) {
		   @SuppressWarnings("unchecked")
		   Iterator<JSONObject> jsonArgumentIterator = jsonNotificationFormaArguments.iterator();
		   while(jsonArgumentIterator.hasNext()) {
			     arguments.add(notificationFormatArgumentFromJSON(jsonArgumentIterator.next()));
		   }
		   
		   // Validate arguments index was set correctly.
		   int maxIndex = 0;
		   for(HpcNotificationFormatArgument argument : arguments) {
			   if(maxIndex < argument.getIndex()) {
				  maxIndex = argument.getIndex();
			   }
		   }
		   if(maxIndex + 1 != arguments.size()) {
			  throw new HpcException("Invalid arguments index: " + jsonNotificationFormaArguments,
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
     * 
     * @throws HpcException If failed to parse the JSON
     */
	private HpcNotificationFormat notificationFormatFromJSON(JSONObject jsonNotificationFormat) 
    		                                                throws HpcException
    {
    	if(!jsonNotificationFormat.containsKey("subjectFormat") ||
	       !jsonNotificationFormat.containsKey("subjectArguments") ||
	       !jsonNotificationFormat.containsKey("textFormat") ||
	       !jsonNotificationFormat.containsKey("textArguments")) {
	       throw new HpcException("Invalid notification format JSON object: " + 
	                              jsonNotificationFormat,
	    	                      HpcErrorType.SPRING_CONFIGURATION_ERROR);	
    	}
	    			
    	// JSON -> POJO.
    	HpcNotificationFormat notificationFormat = new HpcNotificationFormat();
    	notificationFormat.setSubjectFormat((String) jsonNotificationFormat.get("subjectFormat"));
    	notificationFormat.getSubjectArguments().addAll(
    			    notificationFormatArgumentsFromJSON(
    			    		    (JSONArray) jsonNotificationFormat.get("subjectArguments")));
    	notificationFormat.setTextFormat((String) jsonNotificationFormat.get("textFormat"));
    	notificationFormat.getTextArguments().addAll(
    			    notificationFormatArgumentsFromJSON(
    			    		    (JSONArray) jsonNotificationFormat.get("textArguments")));

	    return notificationFormat;
    }
    
    /**
     * Open and parse the notification formats JSON file.
     * 
     * @param notificationFormatPath The path to the notification formats JSON.
     * @throws HpcException
     */
	private void initNotificationFormats(String notificationFormatPath) throws HpcException
    {
		// Open and Parse the notification formats JSON file.
		JSONArray jsonNotificationFormats = null;
		try {
	         FileReader reader = new FileReader(notificationFormatPath);
	         jsonNotificationFormats = (JSONArray) ((JSONObject) new JSONParser().parse(reader)).get("notificationFormats");

		} catch(Exception e) {
		        throw new HpcException("Could not open or parse: " + notificationFormatPath,
                                       HpcErrorType.SPRING_CONFIGURATION_ERROR, e);
		}
		
		// Validate formats are defined.
        if(jsonNotificationFormats == null || jsonNotificationFormats.size() == 0) {
       	   throw new HpcException("No notification formats found in JSON file",
                                  HpcErrorType.SPRING_CONFIGURATION_ERROR); 
        }
        
        // Iterate through the list of formats and populate the map.
		@SuppressWarnings("unchecked")
		Iterator<JSONObject> jsonNotificationFormatIterator = jsonNotificationFormats.iterator();
		while(jsonNotificationFormatIterator.hasNext()) {
		      JSONObject jsonNotificationFormat = jsonNotificationFormatIterator.next();
			   
			  // Extract the event type.
			  HpcEventType eventType = HpcEventType.valueOf(
			                              (String) jsonNotificationFormat.get("eventType"));
			  if(eventType == null) {
				 throw new HpcException("Invalid event type: " + jsonNotificationFormat,
				                        HpcErrorType.SPRING_CONFIGURATION_ERROR); 
			  }
			   
			  // Populate the map <eventType -> notificationFormat>
			  notificationFormats.put(eventType, notificationFormatFromJSON(jsonNotificationFormat));
		}
    }
	
    /**
     * Get notification payload entry value.
     *
     * @param eventPayloadEntries The event payload entries.
     * @param attribute The payload entry attribute to find.
     * @return The payload entry value.
     */
	private String getPayloadEntryValue(List<HpcEventPayloadEntry> eventPayloadEntries, 
			                            String attribute)
	{
		if(eventPayloadEntries == null) {
		   return DEFAULT_PAYLOAD_ENTRY_VALUE;
		}
		
		for(HpcEventPayloadEntry payloadEntry : eventPayloadEntries) {
			if(payloadEntry.getAttribute().equals(attribute)) {
			   return payloadEntry.getValue();
			}
		}
		
		return DEFAULT_PAYLOAD_ENTRY_VALUE;
	}
}

 