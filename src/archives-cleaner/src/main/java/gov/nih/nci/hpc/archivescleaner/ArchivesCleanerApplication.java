package gov.nih.nci.hpc.archivescleaner;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

//EmbeddedServletContainerAutoConfiguration.class
@SpringBootApplication(exclude = {WebMvcAutoConfiguration.class})
//public class ArchivesCleanerApplication implements ApplicationRunner {
public class ArchivesCleanerApplication implements CommandLineRunner {

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


  @Value("${dme.rest.server-base-url}")
  private String restServerBaseUrl;

  @Value("${dme.rest.srvc.del-coll.ctx-rel-url}")
  private String restServiceDelCollRelUrl;

  @Value("${dme.rest.srvc.del-data.ctx-rel-url}")
  private String restServiceDelDataObjRelUrl;

  @Autowired
  private ArchivesCleanerDao cleanerDao;

  private List<String> collections2Delete;
  private List<String> deletedCollections;
  private List<ResidualItem> residualCollections;

  private List<String> dataObjects2Delete;
  private List<String> deletedDataObjects;
  private List<ResidualItem> residualDataObjects;


	@Override
	public void run(String ... args) throws Exception {
	  if (args.length > 0) {
      StringBuilder sb = new StringBuilder();
      for (String someArg : args) {
        sb.append(" ").append(someArg);
      }
      System.out.println("The args were:" + sb.toString());
	    this.cleanerDao.setTargetedDocs(Optional.of(Arrays.asList(args)));
    }

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

//    processDeletions(RestTemplateFactory.getRestTemplate());
//    System.out.println("... Finished performing delete operations.");

    final Date finishExec = new Date();
    System.out.println("... Clean processing finished at " + df.format(
      finishExec));

//    final String reportFileLoc = createResultsReport(startExec, finishExec);
//    System.out.println("... Generated report file at " + reportFileLoc);

    System.out.println("FINISHED SUCCESSFULLY.");
	}


  private void processDeletions(RestTemplate myRestTemplate) {
    final HttpHeaders myHeaders = new HttpHeaders();
    myHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer ".concat("TODO"));

    final StringBuilder sb = new StringBuilder();
    sb.append(this.restServerBaseUrl).append(this.restServiceDelCollRelUrl);
    final String restServiceDelCollWholeUrl = sb.toString();

    System.out.println("... Performing delete operations on Collections");

    int count = 0;
    for (String someCollection : this.collections2Delete) {
      // Make request to delete collection
      ResponseEntity<String> serviceResponse = myRestTemplate.exchange(
        restServiceDelCollWholeUrl, HttpMethod.DELETE, new HttpEntity<String>
        (myHeaders), String.class, someCollection);
      if (HttpStatus.OK.equals(serviceResponse.getStatusCode()) ||
          HttpStatus.NO_CONTENT.equals(serviceResponse.getStatusCode())) {
        deletedCollections.add(someCollection);
      } else {
        residualCollections.add(new ResidualItem(someCollection,
          serviceResponse.getBody()));
      }
      count += 1;
      if (count > 0 && count % 10 == 0) {
        int numLeft = this.collections2Delete.size() - count;
        System.out.println("... # remaining Collections to process = " + numLeft);
      }
    }

    System.out.println("... Done performing delete operations on Collections");

    sb.setLength(0);
    sb.append(this.restServerBaseUrl).append(this.restServiceDelDataObjRelUrl);
    final String restServiceDelDataObjWholeUrl = sb.toString();

    System.out.println("... Performing delete operations on Data Objects");

    count = 0;
    for (String someDataObj : this.dataObjects2Delete) {
      // Make request to delete data object
      ResponseEntity<String> serviceResponse = myRestTemplate.exchange(
        restServiceDelDataObjWholeUrl, HttpMethod.DELETE, new HttpEntity<String>
        (myHeaders), String.class, someDataObj);
      if (HttpStatus.OK.equals(serviceResponse.getStatusCode()) ||
          HttpStatus.NO_CONTENT.equals(serviceResponse.getStatusCode())) {
        deletedDataObjects.add(someDataObj);
      } else {
        residualDataObjects.add(new ResidualItem(someDataObj,
          serviceResponse.getBody()));
      }
      count += 1;
      if (count > 0 && count % 10 == 0) {
        int numLeft = this.dataObjects2Delete.size() - count;
        System.out.println("... # remaining Data Objects to process = " + numLeft);
      }
    }

    System.out.println("... Done performing delete operations on Data Objects");
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

}
