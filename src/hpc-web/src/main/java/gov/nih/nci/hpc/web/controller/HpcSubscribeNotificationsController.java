/**
 * HpcSubscribeNotificationsController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationTrigger;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.notification.HpcAddOrUpdateNotificationSubscriptionProblem;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsResponseDTO;
import gov.nih.nci.hpc.dto.notification.HpcRemoveNotificationSubscriptionProblem;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcNotification;
import gov.nih.nci.hpc.web.model.HpcNotificationRequest;
import gov.nih.nci.hpc.web.model.HpcNotificationTriggerModel;
import gov.nih.nci.hpc.web.model.HpcNotificationTriggerModelEntry;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcIdentityUtil;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * <p>
 * Controller to subscribe or unsubscribe notifications
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/subscribe")
public class HpcSubscribeNotificationsController extends
    gov.nih.nci.hpc.web.controller.AbstractHpcController {


  private static class SubscriptionProblemsBundle {
    private Optional<List<String>> problems;

    private SubscriptionProblemsBundle() {
      this.problems = Optional.empty();
    }

    private SubscriptionProblemsBundle(List<String> problemsVal) {
      this.problems = (null == problemsVal || problemsVal.isEmpty()) ?
        Optional.empty() : Optional.of(problemsVal);
    }

    private Optional<List<String>> getProblems() {
      return this.problems;
    }

    private void setProblems(Optional<List<String>> arg) {
      this.problems = arg;
    }
  }


  private static class SubscriptionProblemsBundleDeserializer implements
    JsonDeserializer<SubscriptionProblemsBundle> {

    @Override
    public SubscriptionProblemsBundle deserialize(
      JsonElement jsonElement,
      Type type,
      JsonDeserializationContext jsonDeserializationContext)
    throws JsonParseException {

      SubscriptionProblemsBundle resultObj = null;
      if (jsonElement.isJsonObject()) {
        List<String> problems = new ArrayList<>();
        JsonObject jsonObj = jsonElement.getAsJsonObject();
        if (jsonObj.has("subscriptionsCouldNotBeAddedOrUpdated")) {
          // Single instance or List of HpcAddOrUpdateNotificationSubscriptionProblem
          Consumer<JsonElement> consumer1 = elem -> {
            String problemMsg = MSG_TEMPLATE__ADD_UPDT_SBSCRPTN_PRBLM
              .replace("PLACEHOLDER_EVENT",
                elem.getAsJsonObject()
                    .getAsJsonObject("subscription")
                    .getAsJsonPrimitive("eventType")
                    .getAsString())
              .replace("PLACEHOLDER_PROBLEM",
                elem.getAsJsonObject()
                    .getAsJsonPrimitive("problem")
                    .getAsString());
            problems.add(problemMsg);
          };
          JsonElement tmpElem = jsonObj.get("subscriptionsCouldNotBeAddedOrUpdated");
          if (tmpElem.isJsonArray()) {
            tmpElem.getAsJsonArray().forEach(consumer1);
          }
          else if (tmpElem.isJsonObject()) {
            consumer1.accept(tmpElem);
          }
        }
      /*
        if (jsonObj.has("addedOrUpdatedSubscriptions")) {
          // Single instance or List of HpcNotificationSubscription
          // if desired, add logic to transform the content for this member
        }
        if (jsonObj.has("removedSubscriptions")) {
          // Single instance or List of HpcEventType
          // if desired, add logic to transform the content for this member
        }
      */
        if (jsonObj.has("subscriptionsCouldNotBeRemoved")) {
          // Single instance or List of HpcRemoveNotificationSubscriptionProblem
          Consumer<JsonElement> consumer2 = elem -> {
            String problemMsg = MSG_TEMPLATE__RMV_SBSCRPTN_PRBLM
                .replace("PLACEHOLDER_EVENT",
                    elem.getAsJsonObject()
                        .getAsJsonPrimitive("removeSubscriptionEvent")
                        .getAsString())
                .replace("PLACEHOLDER_PROBLEM",
                    elem.getAsJsonObject()
                        .getAsJsonPrimitive("problem")
                        .getAsString());
            problems.add(problemMsg);
          };
          JsonElement tmpElem = jsonObj.get("subscriptionsCouldNotBeRemoved");
          if (tmpElem.isJsonArray()) {
            tmpElem.getAsJsonArray().forEach(consumer2);
          }
          else if (tmpElem.isJsonObject()) {
            consumer2.accept(tmpElem);
          }
        }
        resultObj = new SubscriptionProblemsBundle(problems);
      }

      return resultObj;
    }

  }


  public static final String PROPERTY_KEY__NOTIFICATION_EXCLUDE_LIST =
      "gov.nih.nci.notification.exclude.list";

//  private static final Feature[] JSON_DESERIAL_FEATURES_4_ACTIVATION = new
//    Feature[] { Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY };

  private static final String MSG_TEMPLATE__ADD_UPDT_SBSCRPTN_PRBLM =
      "Unable to add/update Notifications for PLACEHOLDER_EVENT.  Reason: PLACEHOLDER_PROBLEM.";

  private static final String MSG_TEMPLATE__RMV_SBSCRPTN_PRBLM =
    "Unable to remove Notifications for PLACEHOLDER_EVENT.  Reason: PLACEHOLDER_PROBLEM.";


  private static String buildProblemMsgs(
    HpcNotificationSubscriptionsResponseDTO respDto) {
    List<String> problemMsgs = new ArrayList<>();
    respDto.getSubscriptionsCouldNotBeAddedOrUpdated()
      .stream()
      .forEach(problem -> {
        problemMsgs.add(generateProblemMsg(problem));
      });
    respDto.getSubscriptionsCouldNotBeRemoved()
      .stream()
      .forEach(problem -> {
        problemMsgs.add(generateProblemMsg(problem));
      });
    String result = problemMsgs.stream()
      .reduce((str1, str2) -> (str1 + "\n" + str2))
      .orElse("");

    return result;
  }


  private static String generateProblemMsg(
    HpcAddOrUpdateNotificationSubscriptionProblem problem) {
    String msg = MSG_TEMPLATE__ADD_UPDT_SBSCRPTN_PRBLM
      .replace("PLACEHOLDER_EVENT",
        problem.getSubscription().getEventType().name())
      .replace("PLACEHOLDER_PROBLEM",
        problem.getProblem());

    return msg;
  }


  private static String generateProblemMsg(
    HpcRemoveNotificationSubscriptionProblem problem) {
    String msg = MSG_TEMPLATE__RMV_SBSCRPTN_PRBLM
      .replace("PLACEHOLDER_EVENT",
        problem.getRemoveSubscriptionEvent().name())
      .replace("PLACEHOLDER_PROBLEM",
        problem.getProblem());

    return msg;
  }


  @Value("${gov.nih.nci.hpc.server.notification}")
  private String notificationURL;

  @Autowired
  private Environment env;

  private List<HpcEventType> selectedEventTypes = null;
  private List<HpcEventType> unselectedEventTypes = null;

  /**
   * GET action to populate notifications with user subscriptions
   */
  @RequestMapping(method = RequestMethod.GET)
  public String home(@RequestBody(required = false) String q, Model model,
      BindingResult bindingResult,
      HttpSession session, HttpServletRequest request) {
    String authToken = (String) session.getAttribute("hpcUserToken");
    if (authToken == null) {
      return "redirect:/";
    }

    HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
    if (user == null) {
      ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
      bindingResult.addError(error);
      HpcLogin hpcLogin = new HpcLogin();
      model.addAttribute("hpcLogin", hpcLogin);
      return "redirect:/login?returnPath=subscribe";
    }

    populateNotifications(user, authToken, session, model);
    HpcNotificationRequest notificationRequest = new HpcNotificationRequest();
    model.addAttribute("notificationRequest", notificationRequest);

    return "subscribenotifications";
  }

  /**
   * POST action to subscribe or unsubscribe notifications
   *
   * @param notificationRequest   model object for receiving request state of
   *                               type HpcNotificationRequest
   * @param model  model state to support view template rendering
   * @param bindingResult  binding result
   * @param session  The HttpSession object
   * @param request  The HttpServletRequest object
   * @param redirectAttrs  redirection attributes state
   * @return String navigation outcome or null
   */
  @RequestMapping(method = RequestMethod.POST)
  public String search(
      @Valid @ModelAttribute("notificationRequest")
        HpcNotificationRequest notificationRequest,
      Model model,
      BindingResult bindingResult,
      HttpSession session,
      HttpServletRequest request,
      RedirectAttributes redirectAttrs) {

    try {
      String authToken = (String) session.getAttribute("hpcUserToken");
      String serviceURL = notificationURL;
//      HpcNotificationSubscriptionsRequestDTO subscriptionsRequestDTO = buildNotifSubsrptnsReqDto(
//          request);
      HpcNotificationSubscriptionsRequestDTO subscriptionsRequestDTO =
        buildDto4ServiceRequest(request, session);

      WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath,
        sslCertPassword);
      client.header("Authorization", "Bearer " + authToken);
      Response restResponse = client.invoke("POST", subscriptionsRequestDTO);
      if (restResponse.getStatus() == 200) {
        if (restResponse.getEntity() instanceof InputStream) {
          String responseBody = IOUtils.toString((InputStream)
            restResponse.getEntity());
          Gson gsonObj = new GsonBuilder()
            .registerTypeAdapter(SubscriptionProblemsBundle.class, new
              SubscriptionProblemsBundleDeserializer())
            .create();
          SubscriptionProblemsBundle spBundle = gsonObj.fromJson(responseBody,
            SubscriptionProblemsBundle.class);
          String statusMsg = "Updated successfully";
          if (null != spBundle && spBundle.getProblems().isPresent()) {
              StringBuilder sb = new StringBuilder();
              List<String> problems = spBundle.getProblems().get();
              for (String problem : problems) {
                if (sb.length() > 0) {
                  sb.append("\n");
                }
                sb.append(problem);
              }
              statusMsg = sb.toString();
          }
          model.addAttribute("updateStatus", statusMsg);
        }
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        model.addAttribute("updateStatus",
            "Failed to save criteria! Reason: " + exception.getMessage());
      }
    } catch (HttpStatusCodeException e) {
      model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
      e.printStackTrace();
    } catch (RestClientException e) {
      model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
      e.printStackTrace();
    } finally {
      String authToken = (String) session.getAttribute("hpcUserToken");
      if (authToken == null) {
        return "redirect:/";
      }
      HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
      if (user == null) {
        ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
        bindingResult.addError(error);
        HpcLogin hpcLogin = new HpcLogin();
        model.addAttribute("hpcLogin", hpcLogin);
        return "redirect:/";
      }

      populateNotifications(user, authToken, session, model);
      model.addAttribute("notificationRequest", notificationRequest);
    }

    return "subscribenotifications";
  }


  /*
   * Builds data transfer object for use in invoking service to perform actions
   * on notification subscriptions (add, update, remove).
   *
   * @param request - HttpServletRequest instance
   * @param session - HttpSession instance
   * @return HpcNotificationSubscriptionsRequestDTO instance for making
   *          service call
   */
  private HpcNotificationSubscriptionsRequestDTO buildDto4ServiceRequest(
      HttpServletRequest request, HttpSession session) {

    classifyEventTypes(request);

    final HpcNotificationSubscriptionsRequestDTO dto = new
        HpcNotificationSubscriptionsRequestDTO();

    List<HpcNotificationSubscription> addUpdtModel = generateAddUpdtModel(
      request, session);
    List<HpcEventType> removeModel = generateRemoveModel(session, addUpdtModel);

    dto.getAddUpdateSubscriptions().addAll(addUpdtModel);
    dto.getDeleteSubscriptions().addAll(removeModel);

    return dto;
  }


  /*
   * Given list of triggers, adds new trigger based on COLLECTION_PATH
   * attribute being equal to given path value.
   *
   * @param triggers - list of triggers as List<HpcNotificationTrigger>
   * @param pathVal - desired triggering value for COLLECTION_PATH attribute
   */
  private void captureAddedCollPathTrigger(
      List<HpcNotificationTrigger> triggers, String pathVal) {
    HpcEventPayloadEntry pathEntry = new HpcEventPayloadEntry();
    pathEntry.setAttribute("COLLECTION_PATH");
    pathEntry.setValue(pathVal);

    List<HpcEventPayloadEntry> entries = new ArrayList<HpcEventPayloadEntry>();
    entries.add(pathEntry);

    HpcNotificationTrigger trigger = new HpcNotificationTrigger();
    trigger.getPayloadEntries().addAll(entries);
    triggers.add(trigger);
  }


  /*
   * Given list of triggers, HTTP request, and specified request parameter,
   * determines whether parameter corresponds to existing Collection Path based
   * Notification Subscription.  If so, adds trigger to list of triggers based
   * on COLLECTION_PATH attribute being equal to value of existing Collection
   * Path as received in request.
   *
   * @param triggers - list of triggers as List<HpcNotificationTrigger>
   * @param paramName - name of request parameter
   * @param request - HttpServletRequest instance
   */
  private void captureExistingCollPathTrigger(
      List<HpcNotificationTrigger> triggers, String paramName,
      HttpServletRequest request) {
    String counter = paramName.substring("existingCollectionCheck".length());
    String[] existingCollectionPath = request.getParameterValues(
        "existingCollectionPath" + counter);

    HpcEventPayloadEntry pathEntry = new HpcEventPayloadEntry();
    pathEntry.setAttribute("COLLECTION_PATH");
    pathEntry.setValue(existingCollectionPath[0]);

    List<HpcEventPayloadEntry> entries = new ArrayList<HpcEventPayloadEntry>();
    entries.add(pathEntry);
    String[] existingMetadataCheck = request.getParameterValues(
        "existingMetadataCheck" + counter);
    if (null != existingMetadataCheck && 0 < existingMetadataCheck.length &&
        checkIfTrueOrOn(existingMetadataCheck[0])) {
      HpcEventPayloadEntry metadataEntry = new HpcEventPayloadEntry();
      metadataEntry.setAttribute("UPDATE");
      metadataEntry.setValue("METADATA");
      entries.add(metadataEntry);
    }

    HpcNotificationTrigger trigger = new HpcNotificationTrigger();
    trigger.getPayloadEntries().addAll(entries);
    triggers.add(trigger);
  }


  /*
   * Checks whether given string is equal to "true" or "on".
   *
   * @param val - given string
   * @return boolean true if string equals "true" or "on", false otherwise
   */
  private boolean checkIfTrueOrOn(String val) {
    return "true".equals(val) || "on".equals(val);
  }


  /*
   * Determines which Event Types are selected and which are not selected in
   * request.  Puts that information in fields named selectedEventTypes
   * and unselectedEventTypes, both of type List<HpcEventType>.
   *
   * @param request - HttpServletRequest instance
   */
  private void classifyEventTypes(HttpServletRequest request) {
    List<HpcEventType> availableEventTypes = getEventTypes();
    String[] eventTypesInReq = request.getParameterValues("eventType");
    if (null == eventTypesInReq || 0 == eventTypesInReq.length) {
      // no Event Types from request object
      this.selectedEventTypes = new ArrayList<>();
      this.unselectedEventTypes = new ArrayList<>(availableEventTypes);
    } else {
      // at least 1 Event Type from request object
      this.selectedEventTypes = new ArrayList<>(eventTypesInReq.length);
      for (String anEventTypeInReq : eventTypesInReq) {
        this.selectedEventTypes.add(HpcEventType.valueOf(anEventTypeInReq));
      }
      this.unselectedEventTypes = new ArrayList<>(availableEventTypes);
      this.unselectedEventTypes.removeAll(this.selectedEventTypes);
    }
  }


  /*
   * Constructs an Optional of type HpcNotificationSubscription based on whether
   * request conveys Notification Subscription(s) of the Collection Updated kind.
   * If the request does so, the Optional is populated.  Otherwise, it is empty.
   *
   * @param request - HttpServletRequest instance
   * @return Optional<HpcNotificationSubscription> that is not empty if
   *          request conveys Notification Subscription(s) of the Collection
   *          Updated kind
   */
  private Optional<HpcNotificationSubscription> constructCollUpdtSubscrptn(
      HttpServletRequest request,
      List<HpcNotificationSubscription> currSubscriptions) {

    HpcNotificationSubscription collUpdtSubscription = null;

    Optional<HpcNotificationSubscription> currCollUpdtSubscription =
      Optional.empty();
    if (null != currSubscriptions && !currSubscriptions.isEmpty()) {
      currSubscriptions.stream()
        .filter(subscription -> HpcEventType.COLLECTION_UPDATED.equals(
          subscription.getEventType()) )
        .findAny();
    }

    Optional<List<HpcNotificationTrigger>> collUpdtTriggersOptional =
      constructCollUpdtTriggers(request);

    if (currCollUpdtSubscription.isPresent()) {
      if (collUpdtTriggersOptional.isPresent()) {
        currCollUpdtSubscription.get().getNotificationTriggers().clear();
        currCollUpdtSubscription.get().getNotificationTriggers().addAll(
          collUpdtTriggersOptional.get());
        collUpdtSubscription = currCollUpdtSubscription.get();
      }
    } else {
      if (collUpdtTriggersOptional.isPresent()) {
        collUpdtSubscription = new HpcNotificationSubscription();
        collUpdtSubscription.setEventType(HpcEventType.COLLECTION_UPDATED);
        collUpdtSubscription.getNotificationDeliveryMethods().add(
          HpcNotificationDeliveryMethod.EMAIL);
        collUpdtSubscription.getNotificationTriggers().addAll(
          collUpdtTriggersOptional.get());
      }
    }

    return (null == collUpdtSubscription) ? Optional.empty() : Optional.of(
        collUpdtSubscription);
  }


  /*
   * Constructs an Optional of type List<HpcNotificationTrigger> based on
   * whether request conveys Notification Subscription(s) of the Collection
   * Updated kind.  If request does so, returned Optional is populated.
   * Otherwise, it is empty.
   *
   * @param request - HttpServletRequest instance
   * @return Optional<List<HpcNotificationTrigger>> that is not empty if
   *          request conveys Notification Subscription(s) of the Collection
   *          Updated kind
   */
  private Optional<List<HpcNotificationTrigger>> constructCollUpdtTriggers(
      HttpServletRequest request) {
    List<HpcNotificationTrigger> accumTriggers = new ArrayList<>();
    Enumeration<String> params = request.getParameterNames();
    while (params.hasMoreElements()) {
      String paramName = params.nextElement();
      if (paramName.startsWith("collectionPathAdded")) {
        String[] value = request.getParameterValues(paramName);
        if (value != null && !value[0].isEmpty()) {
          captureAddedCollPathTrigger(accumTriggers, value[0]);
        }
      } else if (paramName.startsWith("existingCollectionCheck")) {
        String[] value = request.getParameterValues(paramName);
        if (value != null && value[0].equals("on")) {
          captureExistingCollPathTrigger(accumTriggers, paramName, request);
        }
      }
    }
    Optional<List<HpcNotificationTrigger>> retTriggerList =
        accumTriggers.isEmpty() ? Optional.empty() : Optional.of(accumTriggers);

    return retTriggerList;
  }


  /*
   * For request to save Notification Subscriptions, generates model object
   * state representing actions to add or update Subscriptions.
   *
   * @param request - HttpServletRequest instance
   * @param session - HttpSession instance
   * @return List<HpcNotificationSubscription> representing actions to add or
   *          update Subscriptions
   */
  private List<HpcNotificationSubscription> generateAddUpdtModel(
      HttpServletRequest request, HttpSession session) {
    List<HpcNotificationSubscription> tmpSubscriptions =
      getUserNotificationSubscriptions((String) session.getAttribute(
      "hpcUserToken"));

    final List<HpcNotificationSubscription> addUpdtModel = new ArrayList<>();

    constructCollUpdtSubscrptn(request, tmpSubscriptions).ifPresent(
      notifSub -> {
        addUpdtModel.add(notifSub);
      }
    );

    if (null == tmpSubscriptions) {
      // Since there are currently no Subscriptions, set up adding new
      //  Subscription per Event Type selected in request.
      this.selectedEventTypes.stream().forEach(slctdEventType -> {
        HpcNotificationSubscription hpcNotifSub = new
            HpcNotificationSubscription();
        hpcNotifSub.setEventType(slctdEventType);
        hpcNotifSub.getNotificationDeliveryMethods().add(
            HpcNotificationDeliveryMethod.EMAIL);
        addUpdtModel.add(hpcNotifSub);
      });
    } else {
      // Set up adding Subscriptions to Event Types which are currently
      //  not subscribed to but were selected in request.
      this.selectedEventTypes.stream().forEach(slctdEventType -> {
        List<HpcNotificationSubscription> filteredSubs = tmpSubscriptions
            .stream()
            .filter(someSub -> someSub.getEventType().equals(slctdEventType))
            .collect(Collectors.toList());
        if (filteredSubs.isEmpty()) {
          HpcNotificationSubscription hpcNotifSub = new
              HpcNotificationSubscription();
          hpcNotifSub.setEventType(slctdEventType);
          hpcNotifSub.getNotificationDeliveryMethods().add(
              HpcNotificationDeliveryMethod.EMAIL);
          addUpdtModel.add(hpcNotifSub);
        }
      });
    }

    return addUpdtModel;
  }


  /*
   * For request to save Notification Subscriptions, generates model object
   * state representing actions to remove Subscriptions.
   *
   * @param session - HttpSession instance
   * @return List<HpcEventType> representing actions to remove Subscriptions
   */
  private List<HpcEventType> generateRemoveModel(HttpSession session,
    List<HpcNotificationSubscription> addUpdateModel) {

    List<HpcEventType> removeModel = new ArrayList<>();

    // Set up removing Subscriptions to Event Types which are currently
    //  subscribed to but were not selected in request.
    List<HpcNotificationSubscription> tmpSubscriptions =
        getUserNotificationSubscriptions((String) session.getAttribute(
            "hpcUserToken"));
    if (null == tmpSubscriptions) {
      // do nothing, as there are no current subscriptions that can be removed
    } else {
      this.unselectedEventTypes.stream().forEach(unslctdEventType -> {
        tmpSubscriptions.stream()
            .filter(someSub -> someSub.getEventType().equals(unslctdEventType))
            .findFirst()
            .ifPresent(firstNotifSub -> removeModel.add(unslctdEventType));
      });
    }

    // If "Collection Updated" Event Type is already represented in Add/Update
    //  Subscriptions model state, then ensure that Remove Subscriptions model
    //  state excludes "Collection Updated" Event Type.
    if (null != addUpdateModel &&
        addUpdateModel.stream()
          .filter(subscription ->
            HpcEventType.COLLECTION_UPDATED.equals(subscription.getEventType())
          )
          .findAny()
          .isPresent()) {
      removeModel.remove(HpcEventType.COLLECTION_UPDATED);
    }

    return removeModel;
  }


  /*
   * Looks up Display Name for a given Event Name.
   *
   * @param eventName - Name of an Event
   * @return Display Name for Event
   */
  private String getDisplayName(String eventName) {
    final String displayName = this.env.getProperty(eventName);
    return (null == displayName) ? eventName : displayName;
  }


  /*
   * Access all Event Types that can be used in Notification Subscriptions.
   *
   * @return List<HpcEventType> representing the Event Types
   */
  private List<HpcEventType> getEventTypes() {
    List<HpcEventType> allEventTypes = Arrays.asList(HpcEventType.values());
    List<HpcEventType> effectiveEventTypes = null;
    if (this.env.containsProperty(PROPERTY_KEY__NOTIFICATION_EXCLUDE_LIST)) {
      String[] exclusionsByName = this.env.getProperty(
          PROPERTY_KEY__NOTIFICATION_EXCLUDE_LIST).split(",");
      if (2 <= exclusionsByName.length) {
        effectiveEventTypes = allEventTypes
          .stream()
          .filter(event ->
            0 == Arrays.stream(exclusionsByName)
                        .filter(exclusion -> exclusion.equals(event.name()) )
                        .count() )
          .collect(Collectors.toList());
      }
    }
    if (null == effectiveEventTypes) {
      effectiveEventTypes = new ArrayList<>(allEventTypes);
    }

    return effectiveEventTypes;
  }


  /*
   * Given User Web Auth Token and an Event Type, accesses User's Subscription
   * to that Event Type, if applicable.
   *
   * @param authToken - HPC Web user authentication token
   * @param type - Event Type
   * @return HpcNotificationSubscription instance representing User's
   *          Subscription to given Event Type, or null if not applicable
   */
  private HpcNotificationSubscription getNotificationSubscription(
      String authToken, HpcEventType type) {
    final List<HpcNotificationSubscription> allSubscriptions =
        getUserNotificationSubscriptions(authToken);

    HpcNotificationSubscription theSubscription = null;
    if (null != allSubscriptions && !allSubscriptions.isEmpty()) {
      for (HpcNotificationSubscription tmp : allSubscriptions) {
        if (tmp.getEventType().equals(type)) {
          theSubscription = tmp;
          break;
        }
      }
    }

    return theSubscription;
  }


  /*
   * Given User Web Auth Token, accesses User's Notification Subscriptions, if
   * any.
   *
   * @param authToken - HPC Web user authentication token
   * @return List<HpcNotificationSubscription> instance representing User's
   *          Subscriptions if there are any; null if there are none
   */
  private List<HpcNotificationSubscription> getUserNotificationSubscriptions(
      String authToken) {
    HpcNotificationSubscriptionListDTO subscriptionListDTO = HpcClientUtil
        .getUserNotifications(authToken, this.notificationURL, this.sslCertPath,
            this.sslCertPassword);
    List<HpcNotificationSubscription> userNotifs = null;
    if (null != subscriptionListDTO &&
        null != subscriptionListDTO.getSubscriptions() &&
        !subscriptionListDTO.getSubscriptions().isEmpty()) {
      userNotifs = subscriptionListDTO.getSubscriptions();
    }

    return userNotifs;
  }


  /*
   * Puts in Model instance (backing view template) and web session data about
   * Notification Subscriptions of current User.
   *
   * @param user - HpcUserDTO representing current User
   * @param authToken - HPC Web user authentication token
   * @param session - HttpSession instance
   * @param model - Model instance having view template state
   */
  private void populateNotifications(HpcUserDTO user, String authToken,
      HttpSession session, Model model) {
    List<HpcNotification> notifications = new ArrayList<HpcNotification>();
    List<HpcEventType> types = getEventTypes();
    for (HpcEventType type : types) {
      if ((type.equals(HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT)
              || ((type.equals(HpcEventType.USAGE_SUMMARY_REPORT)
            		|| type.equals(HpcEventType.DATA_TRANSFER_UPLOAD_IN_TEMPORARY_ARCHIVE))
            		 && !HpcIdentityUtil.isUserSystemAdmin(session)))
    	  || type.equals(HpcEventType.USER_REGISTERED))
        continue;
      HpcNotificationSubscription subscription = getNotificationSubscription(
          authToken, type);
      HpcNotification notification = new HpcNotification();
      notification.setEventType(type.name());
      notification.setDisplayName(getDisplayName(type.name()));
      notification.setSubscribed(null != subscription);
      populateTriggerData(type, subscription, notification);
      notifications.add(notification);
    }

    model.addAttribute("notifications", notifications);
    session.setAttribute("subscribedNotifications", notifications);
  }


  /*
   * Given Event Type, Subscription, and Notification, if Event Type is
   * Collection Updated and Subscription is not null and has at least
   * 1 Trigger, then attempts to update Triggers of the Subscription.
   *
   * @param type - Event Type
   * @param subscription - Subscription
   * @param notification - Notification
   */
  private void populateTriggerData(HpcEventType type,
      HpcNotificationSubscription subscription, HpcNotification notification) {
    if (HpcEventType.COLLECTION_UPDATED.equals(type) &&
        null != subscription &&
        null != subscription.getNotificationTriggers() &&
        !subscription.getNotificationTriggers().isEmpty()) {
      populateTriggerModelEntries(subscription, notification);
    }
  }


  /*
   * Given Subscription and Notification, iterate the Subscription's Triggers
   * and if one is for Metadata Attribute COLLECTION_PATH, copy the Attribute
   * value over to Notification as item in Notification's Triggers.
   *
   * @param subscription - Subscription
   * @param notification - Notification
   */
  private void populateTriggerModelEntries(
      HpcNotificationSubscription subscription, HpcNotification notification) {
    List<HpcNotificationTrigger> triggers = subscription
        .getNotificationTriggers();
    for (HpcNotificationTrigger trigger : triggers) {
      HpcNotificationTriggerModel triggerModel = new
          HpcNotificationTriggerModel();
      if (null != trigger.getPayloadEntries() &&
          !trigger.getPayloadEntries().isEmpty()) {
        HpcNotificationTriggerModelEntry modelEntry = new
            HpcNotificationTriggerModelEntry();
        for (HpcEventPayloadEntry entry : trigger.getPayloadEntries()) {
          if ("COLLECTION_PATH".equals(entry.getAttribute())) {
            modelEntry.setPath(entry.getValue());
          }
          // else
          // if(entry.getAttribute().equals("UPDATE"))
          // modelEntry.setMetadata(entry.getValue());
        }
        triggerModel.getEntries().add(modelEntry);
      }
      notification.getTriggers().add(triggerModel);
    }
  }


/*

  private HpcNotificationSubscriptionsRequestDTO buildNotifSubsrptnsReqDto(
      HttpServletRequest request) {
    String[] selectedEventTypes = request.getParameterValues("eventType");
    HpcNotificationSubscriptionsRequestDTO dto = new
        HpcNotificationSubscriptionsRequestDTO();

    Optional<HpcNotificationSubscription> optCollUpdtSub =
        constructCollUpdtSubscrptn(request);
    if (optCollUpdtSub.isPresent()) {
      dto.getAddUpdateSubscriptions().add(optCollUpdtSub.get());
    }
    if (null != selectedEventTypes) {
      List<HpcEventType> availableEventTypes = getEventTypes();
      for (HpcEventType anAvailableEventType : availableEventTypes) {
        if (subscribed(selectedEventTypes, anAvailableEventType.name())) {
          HpcNotificationSubscription theSub = new
              HpcNotificationSubscription();
          theSub.setEventType(anAvailableEventType);
          theSub.getNotificationDeliveryMethods().add(
              HpcNotificationDeliveryMethod.EMAIL);
          dto.getAddUpdateSubscriptions().add(theSub);
        } else if (!HpcEventType.COLLECTION_UPDATED.equals(anAvailableEventType) ||
            !optCollUpdtSub.isPresent()) {
          dto.getDeleteSubscriptions().add(anAvailableEventType);
        }
      }
    }

    return dto;
  }

  private boolean subscribed(String[] selectedEventTypes,
      String targetEventType) {
    boolean subscribedFlag = false;

    if (null != selectedEventTypes && 0 < selectedEventTypes.length) {
      for (String someSlctdEventType : selectedEventTypes) {
        if (someSlctdEventType.equals(targetEventType)) {
          subscribedFlag = true;
          break;
        }
      }
    }

    return subscribedFlag;
  }

*/

}