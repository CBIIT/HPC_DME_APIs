package gov.nih.nci.hpc.archivescleaner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

//EmbeddedServletContainerAutoConfiguration.class
@SpringBootApplication(exclude = {WebMvcAutoConfiguration.class})
//public class ArchivesCleanerApplication implements ApplicationRunner {
public class ArchivesCleanerApplication implements CommandLineRunner {

  public static class LongRunningOpTimingDisplayer implements Runnable {

    private static final TemporalUnit[] UNITS_OF_INTEREST = new TemporalUnit[]
      { ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS };

    private static final String[] UNIT_LABELS = new String[]
      { "hr", "min", "sec" };


    private static String timeForDisplay(Duration pDuration) {
      StringBuilder sb = new StringBuilder();
      int index = 0;
      for (TemporalUnit unitOfInterest : UNITS_OF_INTEREST) {
        long x = pDuration.get(unitOfInterest);
        if (1 <= x) {
          sb.append(sb.length() == 0 ? "" : ", ")
            .append(x).append(" ").append(UNIT_LABELS[index]);
        }
        index += 1;
      }
      return sb.toString();
    }

    private LocalDateTime started;

    private final long sleepInterval = 10000L;

    public LongRunningOpTimingDisplayer() {
      this.started = LocalDateTime.now();
    }

    @Override
    public void run() {
      try {
        Thread.sleep(this.sleepInterval);

        System.out.println("Elapsed time of task is %s." + timeForDisplay(Duration
            .between(this.started, LocalDateTime.now())));
      } catch (InterruptedException ie) {
        return;
      }
    }

  }

  private static final Properties DEFAULT_MSG_BUNDLE;
  static {
    Properties props = new Properties();
    props.setProperty("feedback.auth.failed", "Authentication has failed.");
    props.setProperty("feedback.auth.failed.with.retry",
      "Authentication has failed, but you may retry.");
    props.setProperty("feedback.auth.success",
      "Authentication has succeeded.");
    props.setProperty("feedback.one.retry", "1 remaining attempt.");
    props.setProperty("feedback.more.retries", "Remaining # attempts = $0.");
    props.setProperty("feedback.termination.by.auth.failures",
      "Program is terminating due to excessive authentication failures.");
    props.setProperty("prompt.password", "Type password: ");
    props.setProperty("prompt.username", "Type username: ");

    DEFAULT_MSG_BUNDLE = props;
  }

  private static final String SINGLE_SPACE = " ";
  private static final String TWO_SPACES = "  ";

  protected static final class ResidualItem {
    protected ResidualItem() {}

    protected ResidualItem(String pItemPath, String pServiceResponse) {
      this.itemPath = pItemPath;
      this.serviceResponse = pServiceResponse;
    }

    protected String itemPath;
    protected String serviceResponse;
  }


  public static void main(String[] args) {
    SpringApplication.run(ArchivesCleanerApplication.class, args);
  }

  @Value("${dme.auth.max.tries}")
  private int authMaxTries;

  @Value("${dme.rest.server-base-url}")
  private String restServerBaseUrl;

  @Value("${dme.rest.srvc.authenticate}")
  private String restServiceAuthenticate;

  @Value("${dme.rest.srvc.del.coll}")
  private String restServiceDelCollUrl;

  @Value("${dme.rest.srvc.del.dataobj}")
  private String restServiceDelDataObjUrl;

  @Autowired
  private ArchivesCleanerDao cleanerDao;

  @Autowired
  private RestTemplateFactory restTemplateFactory;

  private List<String> collections2Delete;
  private List<String> deletedCollections;
  private List<ResidualItem> residualCollections;

  private List<String> dataObjects2Delete;
  private List<String> deletedDataObjects;
  private List<ResidualItem> residualDataObjects;

  private Properties msgBundle;

  private RestTemplate theRestTemplate;
  private String dmeApiAuthToken;


  @Override
  public void run(String ... args) throws Exception {
    loadMessageBundle();
    if (args.length > 0) {
      StringBuilder sb = new StringBuilder();
      for (String someArg : args) {
        sb.append(" ").append(someArg);
      }
      System.out.println("The args were:" + sb.toString());
	    this.cleanerDao.setTargetedDocs(Optional.of(Arrays.asList(args)));
    }

    this.theRestTemplate = this.restTemplateFactory.getRestTemplate();
    if (!processAuthentication()) {
      return;
    }
    System.out.println("... Echo auth token: " + this.dmeApiAuthToken);

    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final Date startExec = new Date();
    System.out.println("... Clean processing started at " + df.format(
      startExec));

    this.collections2Delete = this.cleanerDao.runQueryForCollections();
    echoCollections2Delete();
    System.out.println("... Finished querying for Collections to delete.");

    this.dataObjects2Delete = this.cleanerDao.runQueryForDataObjects();
    echoDataObjects2Delete();
    System.out.println("... Finished querying for Data Objects to delete.");

    processDeletions();
    System.out.println("... Finished performing delete operations.");

    final Date finishExec = new Date();
    System.out.println("... Clean processing finished at " + df.format(
      finishExec));

    final String reportFileLoc = createResultsReport(startExec, finishExec);
    System.out.println("... Generated report file at " + reportFileLoc);

    System.out.println("FINISHED SUCCESSFULLY.");
	}




  private String createResultsReport(Date pStart, Date pFinish) {
    ReportGenerator reportGenerator = new
        ReportGenerator(
        this.cleanerDao.getTargetedDocs().get(),
        this.deletedCollections,
        this.deletedDataObjects,
        this.residualCollections,
        this.residualDataObjects,
        pStart,
        pFinish);
    return reportGenerator.generate();
  }


  private void echoCollections2Delete() {
    System.out.println("The following " + this.collections2Delete.size() +
      " collections have been identified for deletion: ");
    int index = 1;
    for (String someCollection : this.collections2Delete) {
      System.out.println("     " + index + ". " + someCollection);
      index += 1;
    }
    System.out.println("<Collections list end>");
  }


  private void echoDataObjects2Delete() {
    System.out.println("The following " + this.dataObjects2Delete.size() +
      " data objects have been identified for deletion: ");
    int index = 1;
    for (String someDataObject : this.dataObjects2Delete) {
      System.out.println("     " + index + ". " + someDataObject);
      index += 1;
    }
    System.out.println("<Data objects list end>");
  }


  private String extractResponseDetails(ResponseEntity<String> pSrvcResponse) {
    final StringBuilder sb1 = new StringBuilder();
    sb1.append("[ Response Status = ")
        .append(pSrvcResponse.getStatusCodeValue())
        .append("/")
        .append(pSrvcResponse.getStatusCode().getReasonPhrase())
        .append(" ; Response Content ");

    String bodySample = pSrvcResponse.getBody();
    if (100 < bodySample.length()) {
      sb1.append("(truncated to first 100 chars) ");
      bodySample = bodySample.substring(0, 100);
    }
    sb1.append("= '").append(bodySample).append("' ]");
    String responseDetails = sb1.toString();

    return responseDetails;
  }


  private String formDelCollRestSrvcUrl(String dmePath2Coll) throws
    MalformedURLException
  {
    String effCollPath = dmePath2Coll.trim();
    if (effCollPath.startsWith("/")) {
      effCollPath = effCollPath.substring(1);
    }
    String concreteUrl = UriComponentsBuilder.fromHttpUrl(
      this.restServiceDelCollUrl).buildAndExpand(effCollPath).encode()
      .toUri().toURL().toExternalForm();

    return concreteUrl;
  }


  private String formDelDataObjRestSrvcUrl(String dmePath2DataObj) throws
    MalformedURLException
  {
    String effDataObjPath = dmePath2DataObj.trim();
    if (effDataObjPath.startsWith("/")) {
      effDataObjPath = effDataObjPath.substring(1);
    }
    String concreteUrl = UriComponentsBuilder.fromHttpUrl(
      this.restServiceDelDataObjUrl).buildAndExpand(effDataObjPath).encode()
      .toUri().toURL().toExternalForm();

    return concreteUrl;
  }


  private void loadMessageBundle() {
    this.msgBundle = new Properties();
    this.msgBundle.putAll(DEFAULT_MSG_BUNDLE);
    try {
      this.msgBundle.load(this.getClass().getResourceAsStream(
        "/message-bundle.properties"));
    } catch (IOException e) {
      System.out.println("Failed to load message bundle, falling back on" +
        " internal defaults.");
    }
  }


  private void performCollectionsDeletes() {
    System.out.println("... Performing delete operations on Collections");
    HttpHeaders myHeaders = new HttpHeaders();
    myHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.dmeApiAuthToken));
    this.deletedCollections = new ArrayList<>();
    this.residualCollections = new ArrayList<>();
    int count = 0;
    ResponseEntity<String> serviceResponse = null;
    for (String someCollection : this.collections2Delete) {
      try {
        String concreteUrl = formDelCollRestSrvcUrl(someCollection);
        System.out.println("About to invoke HTTP DELETE on " + concreteUrl);
        Thread displayRunningTimeThread = new Thread(new LongRunningOpTimingDisplayer());
        displayRunningTimeThread.start();
        // Make request to delete collection
        serviceResponse = this.theRestTemplate.exchange(
          concreteUrl, HttpMethod.DELETE, new HttpEntity<Object>(myHeaders),
          String.class);
        displayRunningTimeThread.interrupt();
        if (HttpStatus.OK.equals(serviceResponse.getStatusCode()) ||
            HttpStatus.NO_CONTENT.equals(serviceResponse.getStatusCode())) {
          this.deletedCollections.add(someCollection);
          System.out.println("Delete operation was successful");
        } else {
          this.residualCollections.add(new ResidualItem(someCollection,
            extractResponseDetails(serviceResponse)));
          System.out.println("Delete operation failed, recording collection as outstanding");
        }
      } catch (MalformedURLException mue) {
        this.residualCollections.add(new ResidualItem(someCollection,
          "N/A: Unable to form proper URL for REST service request."));
      }
      count += 1;
      if (count > 0 && count % 10 == 0) {
        int numLeft = this.collections2Delete.size() - count;
        System.out.println("... # remaining Collections to process = " + numLeft);
      }
    }
    System.out.println("... Done performing delete operations on Collections");
  }


  private void performDataObjectsDeletes() {
    System.out.println("... Performing delete operations on Data Objects");
    final HttpHeaders myHeaders = new HttpHeaders();
    myHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.dmeApiAuthToken));
    this.deletedDataObjects = new ArrayList<>();
    this.residualDataObjects = new ArrayList<>();
    int count = 0;
    for (String someDataObj : this.dataObjects2Delete) {
      try {
        String concreteUrl = formDelDataObjRestSrvcUrl(someDataObj);
        System.out.println("About to invoke HTTP DELETE on " + concreteUrl);
        Thread displayRunningTimeThread = new Thread(new LongRunningOpTimingDisplayer());
        displayRunningTimeThread.start();
        // Make request to delete data object
        ResponseEntity<String> serviceResponse = this.theRestTemplate.exchange(
          concreteUrl, HttpMethod.DELETE, new HttpEntity<String>(myHeaders),
          String.class);
        displayRunningTimeThread.interrupt();
        if (HttpStatus.OK.equals(serviceResponse.getStatusCode()) ||
            HttpStatus.NO_CONTENT.equals(serviceResponse.getStatusCode())) {
          this.deletedDataObjects.add(someDataObj);
          System.out.println("Delete operation was successful");
        } else {
          this.residualDataObjects.add(new ResidualItem(someDataObj,
            extractResponseDetails(serviceResponse)));
          System.out.println("Delete operation failed, recording data object as outstanding");
        }
      } catch (MalformedURLException mue) {
        this.residualDataObjects.add(new ResidualItem(someDataObj,
          "N/A: Unable to form proper URL for REST service request."));
      }
      count += 1;
      if (count > 0 && count % 10 == 0) {
        int numLeft = this.dataObjects2Delete.size() - count;
        System.out.println("... # remaining Data Objects to process = " + numLeft);
      }
    }

    System.out.println("... Done performing delete operations on Data Objects");
  }


  private boolean processAuthentication() throws IOException {
    boolean retAuthSuccess = false;
    System.out.print(this.msgBundle.getProperty("prompt.username") +
      SINGLE_SPACE);
    String username = new Scanner(System.in).next();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    passwdLoop:
    for (int attemptNum = 1; attemptNum <= this.authMaxTries; attemptNum += 1) {
      String userPasswd = new String(System.console().readPassword(
        this.msgBundle.getProperty("prompt.password") + SINGLE_SPACE));
      this.theRestTemplate.getInterceptors().add(new
        BasicAuthorizationInterceptor(username, userPasswd));
      try {
        ResponseEntity<String> authResp = this.theRestTemplate.exchange(
          this.restServiceAuthenticate, HttpMethod.GET,
          new HttpEntity<String>(headers), String.class);
        if (HttpStatus.OK.equals(authResp.getStatusCode())) {
          this.dmeApiAuthToken = new ObjectMapper().readTree(
            authResp.getBody()).get("token").textValue();
          retAuthSuccess = true;
          break passwdLoop;
        }
      } catch (HttpClientErrorException hcee) {
        // Do nothing is intentional
      }
      produceAuthFailConsoleOutput(this.authMaxTries - attemptNum);
      this.theRestTemplate.getInterceptors().removeIf(someInterceptor -> (
        someInterceptor instanceof BasicAuthorizationInterceptor));
    }
    produceAuthProcLastConsoleOutput(retAuthSuccess);
    return retAuthSuccess;
  }


  private void produceAuthFailConsoleOutput(int remainingCount) {
    if (0 == remainingCount) {
      System.out.println(this.msgBundle.getProperty("feedback.auth.failed"));
    } else {
      System.out.print(this.msgBundle.getProperty(
        "feedback.auth.failed.with.retry"));
      String infoMsg = (1 == remainingCount) ?
        this.msgBundle.getProperty("feedback.one.retry") :
        this.msgBundle.getProperty("feedback.more.retries").replace("$0",
          String.valueOf(remainingCount));
      System.out.println(TWO_SPACES + infoMsg);
    }
  }


  private void produceAuthProcLastConsoleOutput(boolean retAuthSuccess) {
    System.out.print("\n\n");
    System.out.println(this.msgBundle.getProperty(retAuthSuccess ?
      "feedback.auth.success" : "feedback.termination.by.auth.failures"));
  }


  private void processDeletions() {
    this.theRestTemplate = this.restTemplateFactory.getRestTemplate(new
      CustomRestErrorHandler());
    performCollectionsDeletes();
    performDataObjectsDeletes();
  }

}
