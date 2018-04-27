package gov.nih.nci.hpc.archivescleaner;

import gov.nih.nci.hpc.archivescleaner.ArchivesCleanerApplication.ResidualItem;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReportGeneratorTests {

//  @Rule
//  public TemporaryFolder tempFolderRule = new TemporaryFolder();

  @Autowired
  ReportGenerator rgBean;

  @Test
  public void testNoArgConstructor() {
    final String outputMsgPreamble = this.getClass().getCanonicalName() + " testNoArgConstructor: ";
    System.out.println(outputMsgPreamble + "started");

    ReportGenerator rg = new ReportGenerator();

    Assert.assertNotNull(rg);
    Assert.assertNotNull(rg.getDocsList());
    Assert.assertTrue(rg.getDocsList().isEmpty());
    Assert.assertNotNull(rg.getCollectionsDeleted());
    Assert.assertTrue(rg.getCollectionsDeleted().isEmpty());
    Assert.assertNotNull(rg.getDataObjectsDeleted());
    Assert.assertTrue(rg.getDataObjectsDeleted().isEmpty());
    Assert.assertFalse(rg.getCollectionsOutstanding().isPresent());
    Assert.assertFalse(rg.getDataObjectsOutstanding().isPresent());
    Assert.assertFalse(rg.getStartTime().isPresent());
    Assert.assertFalse(rg.getFinishTime().isPresent());

    System.out.println(outputMsgPreamble + "finished!");
  }


  @Test
  public void test7ArgConstructor_clean() {
    final String outputMsgPreamble = this.getClass().getCanonicalName() + " test7ArgConstructor_clean: ";
    System.out.println(outputMsgPreamble + "started");

    final List<String> madeUpDocs = new ArrayList<>();
    madeUpDocs.add("larry");
    madeUpDocs.add("moe");
    madeUpDocs.add("curly");

    final List<String> madeUpDelColls = new ArrayList<>();
    madeUpDelColls.add("/this/is-only/a-test/coll01");
    madeUpDelColls.add("/this/is-only/a-test/coll02");
    madeUpDelColls.add("/this/is-only/a-test/coll03");

    final List<String> madeUpDelDataObjs = new ArrayList<>();
    madeUpDelDataObjs.add("/this/is-only/a-test/coll01/df-foo.xyz");
    madeUpDelDataObjs.add("/this/is-only/a-test/coll02/df-bar.xyz");
    madeUpDelDataObjs.add("/this/is-only/a-test/coll03/df-foobar.xyz");

    final List<ResidualItem> madeUpOutColls = new ArrayList<>();
    madeUpOutColls.add(new ResidualItem(
      "/this/is-only/a-test/coll04",
      "Unable to delete reason"));

    final List<ResidualItem> madeUpOutDataObjs = new ArrayList<>();
    madeUpOutDataObjs.add(new ResidualItem(
      "/this/is-only/a-test/coll01/out-df-alpha.abc",
      "Info about why data file was not deleted"
    ));

    final Date madeUpStart = Date.from(LocalDateTime.now().minusMinutes(5L)
      .atZone(ZoneId.systemDefault()).toInstant());

    final Date madeUpFinish = Date.from(LocalDateTime.now().minusSeconds(10L)
      .atZone(ZoneId.systemDefault()).toInstant());

    ReportGenerator rg = new ReportGenerator(
      madeUpDocs,
      madeUpDelColls,
      madeUpDelDataObjs,
      madeUpOutColls,
      madeUpOutDataObjs,
      madeUpStart,
      madeUpFinish);

    Assert.assertNotNull(rg);
    Assert.assertEquals(rg.getDocsList(), madeUpDocs);
    Assert.assertEquals(rg.getCollectionsDeleted(), madeUpDelColls);
    Assert.assertEquals(rg.getDataObjectsDeleted(), madeUpDelDataObjs);
    Assert.assertTrue(rg.getCollectionsOutstanding().isPresent());
    Assert.assertEquals(rg.getCollectionsOutstanding().get(), madeUpOutColls);
    Assert.assertTrue(rg.getDataObjectsOutstanding().isPresent());
    Assert.assertEquals(rg.getDataObjectsOutstanding().get(), madeUpOutDataObjs);
    Assert.assertTrue(rg.getStartTime().isPresent());
    Assert.assertEquals(rg.getStartTime().get(), madeUpStart);
    Assert.assertTrue(rg.getFinishTime().isPresent());
    Assert.assertEquals(rg.getFinishTime().get(), madeUpFinish);

    System.out.println(outputMsgPreamble + "finished!");
  }


  @Test
  public void test7ArgConstructor_allArgsNull() {
    final String outputMsgPreamble = this.getClass().getCanonicalName() +
      " test7ArgConstructor_allArgsNull: ";
    System.out.println(outputMsgPreamble + "started");

    ReportGenerator rg = new ReportGenerator(
      null,null,null,null,null,null,null);

    Assert.assertNotNull(rg);
    Assert.assertNotNull(rg.getDocsList());
    Assert.assertTrue(rg.getDocsList().isEmpty());
    Assert.assertNotNull(rg.getCollectionsDeleted());
    Assert.assertTrue(rg.getCollectionsDeleted().isEmpty());
    Assert.assertNotNull(rg.getDataObjectsDeleted());
    Assert.assertTrue(rg.getDataObjectsDeleted().isEmpty());
    Assert.assertNotNull(rg.getCollectionsOutstanding());
    Assert.assertFalse(rg.getCollectionsOutstanding().isPresent());
    Assert.assertNotNull(rg.getDataObjectsOutstanding());
    Assert.assertFalse(rg.getDataObjectsOutstanding().isPresent());
    Assert.assertNotNull(rg.getStartTime());
    Assert.assertFalse(rg.getStartTime().isPresent());
    Assert.assertNotNull(rg.getFinishTime());
    Assert.assertFalse(rg.getFinishTime().isPresent());

    System.out.println(outputMsgPreamble + "finished");
  }


  @Test
  public void test5ArgConstructor_clean() {
    final String outputMsgPreamble =
      this.getClass().getCanonicalName() + " test5ArgConstructor_clean: ";

    System.out.println(outputMsgPreamble + "started");

    final List<String> bogusDocs = Arrays.asList(
      "fred", "wilma", "betty", "barney");

    final List<String> bogusDelColls = Arrays.asList(
      "/home/fred/coll1",
      "/home/wilma/coll-a",
      "/home/betty/coll-uno",
      "/home/barney/coll-one");

    final List<String> bogusDelDataObjs = Arrays.asList(
      "/home/fred/coll1/df1.xyz",
      "/home/wilma/coll-a/df-no-01.abc",
      "/home/betty/coll-uno/df-uno.pqr",
      "/home/barney/coll-one/df-one.ghi");

    final List<ResidualItem> bogusResColls = Arrays.asList(
      new ResidualItem("/home/fred/coll2",
        "Unknown reason"),
      new ResidualItem("/home/wilma/coll-b",
        "Insufficient privileges"),
      new ResidualItem("/home/betty/coll-dos",
        "Request timed out"),
      new ResidualItem("/home/barney/coll-two",
        "Does not exist")
    );

    final List<ResidualItem> bogusResDataObjs = Arrays.asList(
      new ResidualItem("/home/fred/coll1/df2.xyz",
        "Unknown reason"),
      new ResidualItem("/home/wilma/coll-a/df-no-02.abc",
        "Insufficient privileges"),
      new ResidualItem("/home/betty/coll-uno/fs-dos.pqr",
        "Request timed out"),
      new ResidualItem("/home/barney/coll-one/df-two.ghi",
        "Does not exist")
    );

    ReportGenerator rg = new ReportGenerator(bogusDocs, bogusDelColls,
      bogusDelDataObjs, bogusResColls, bogusResDataObjs);

    Assert.assertNotNull(rg);
    Assert.assertEquals(rg.getDocsList(), bogusDocs);
    Assert.assertEquals(rg.getCollectionsDeleted(), bogusDelColls);
    Assert.assertEquals(rg.getDataObjectsDeleted(), bogusDelDataObjs);

    Assert.assertNotNull(rg.getCollectionsOutstanding());
    Assert.assertTrue(rg.getCollectionsOutstanding().isPresent());
    Assert.assertEquals(rg.getCollectionsOutstanding().get(), bogusResColls);

    Assert.assertNotNull(rg.getDataObjectsOutstanding());
    Assert.assertTrue(rg.getDataObjectsOutstanding().isPresent());
    Assert.assertEquals(rg.getDataObjectsOutstanding().get(), bogusResDataObjs);

    Assert.assertNotNull(rg.getStartTime());
    Assert.assertFalse(rg.getStartTime().isPresent());

    Assert.assertNotNull(rg.getFinishTime());
    Assert.assertFalse(rg.getFinishTime().isPresent());

    System.out.println(outputMsgPreamble + "finished");
  }


  @Test
  public void test5ArgConstructor_allArgsNull() {
    final String outputMsgPreamble =
      this.getClass().getCanonicalName() + " test5ArgConstructor_allArgsNull: ";

    System.out.println(outputMsgPreamble + "started");

    ReportGenerator rg = new ReportGenerator(null, null, null, null, null);

    Assert.assertNotNull(rg);
    Assert.assertNotNull(rg.getDocsList());
    Assert.assertTrue(rg.getDocsList().isEmpty());

    Assert.assertNotNull(rg.getCollectionsDeleted());
    Assert.assertTrue(rg.getCollectionsDeleted().isEmpty());

    Assert.assertNotNull(rg.getDataObjectsDeleted());
    Assert.assertTrue(rg.getDataObjectsDeleted().isEmpty());

    Assert.assertNotNull(rg.getCollectionsOutstanding());
    Assert.assertFalse(rg.getCollectionsOutstanding().isPresent());

    Assert.assertNotNull(rg.getDataObjectsOutstanding());
    Assert.assertFalse(rg.getDataObjectsOutstanding().isPresent());

    Assert.assertNotNull(rg.getStartTime());
    Assert.assertFalse(rg.getStartTime().isPresent());

    Assert.assertNotNull(rg.getFinishTime());
    Assert.assertFalse(rg.getFinishTime().isPresent());

    System.out.println(outputMsgPreamble + "finished");
  }


  @Test
  public void testGenerate_specifyFilename() {
    final String outputMsgPreamble =
      this.getClass().getCanonicalName() + " testGenerate_specifyFilename: ";

    System.out.println(outputMsgPreamble + "started");

    final Map<String, Object> inputsMap = makeFictitiousReportInputs();
    this.rgBean.setDocsList((List<String>) inputsMap.get("docsList"));
    this.rgBean.setCollectionsDeleted((List<String>) inputsMap.get(
      "deletedCollections"));
    this.rgBean.setDataObjectsDeleted((List<String>) inputsMap.get(
      "deletedDataObjects"));
    this.rgBean.setCollectionsOutstanding(Optional.of(
      (List<ResidualItem>) inputsMap.get("outstandingCollections")));
    this.rgBean.setDataObjectsOutstanding(Optional.of(
        (List<ResidualItem>) inputsMap.get("outstandingDataObjects")));
    this.rgBean.setStartTime(Optional.of((Date) inputsMap.get("startTime")));
    this.rgBean.setFinishTime(Optional.of((Date) inputsMap.get("finishTime")));

    String reportFilePath = "C:\\tmp\\718-test-output\\my-report.txt";
    this.rgBean.generate(reportFilePath);

    File reportFile = new File(reportFilePath);
    Assert.assertTrue(reportFile.exists() && reportFile.isFile());

    System.out.println(outputMsgPreamble + "finished");
  }


  @Test
  public void testGenerate_omitFilename() {
    final String outputMsgPreamble =
        this.getClass().getCanonicalName() + " testGenerate_omitFilename: ";

    System.out.println(outputMsgPreamble + "started");

    final Map<String, Object> inputsMap = makeFictitiousReportInputs();
    this.rgBean.setDocsList((List<String>) inputsMap.get("docsList"));
    this.rgBean.setCollectionsDeleted((List<String>) inputsMap.get(
        "deletedCollections"));
    this.rgBean.setDataObjectsDeleted((List<String>) inputsMap.get(
        "deletedDataObjects"));
    this.rgBean.setCollectionsOutstanding(Optional.of(
        (List<ResidualItem>) inputsMap.get("outstandingCollections")));
    this.rgBean.setDataObjectsOutstanding(Optional.of(
        (List<ResidualItem>) inputsMap.get("outstandingDataObjects")));
    this.rgBean.setStartTime(Optional.of((Date) inputsMap.get("startTime")));
    this.rgBean.setFinishTime(Optional.of((Date) inputsMap.get("finishTime")));

    String reportFileAbsPath = this.rgBean.generate();
    System.out.print(outputMsgPreamble + " report file location is " +
      reportFileAbsPath);

    File reportFile = new File(reportFileAbsPath);
    Assert.assertTrue(reportFile.exists() && reportFile.isFile());

    System.out.println(outputMsgPreamble + "finished");
  }

  //@Test
  public void testFileSystemWriteToFile() {
    final String tempFileLoc = "C:\\tmp\\718-test-output\\dummy.txt";
    try (
      BufferedWriter bufWriter = new BufferedWriter(new FileWriter(tempFileLoc, true));
    ) {
      bufWriter.write("hello world");
      bufWriter.newLine();
      bufWriter.write("the end");
      bufWriter.newLine();
      bufWriter.flush();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      Assert.fail("IOException happened!");
    }

    try (
      BufferedReader bufReader = new BufferedReader(new FileReader(tempFileLoc));
    ) {
      String firstLine = bufReader.readLine();
      String secondLine = bufReader.readLine();
      Assert.assertEquals(firstLine, "hello world");
      Assert.assertEquals(secondLine, "the end");
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
      Assert.fail("File was not found at " + tempFileLoc);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      Assert.fail("Encountered problem with file I/O!");
    }

  }


  private Map<String, Object> makeFictitiousReportInputs() {
    final Map<String, Object> retMap = new HashMap<>();

    retMap.put("docsList", Arrays.asList(
        "DOC-alpha", "DOC-beta", "DOC-gamma", "DOC-zeta"));

    retMap.put("deletedCollections", Arrays.asList(
        "/alpha/bpath1/foo",
        "/alpha/bpath2/bar",
        "/beta/bpathX/uno",
        "/beta/bpathX/dos",
        "/beta/bpathX/tres",
        "/gamma/bpApple/red",
        "/gamma/bpApple/green",
        "/gamma/bpApple/gold",
        "/gamma/bpBanana/yellow",
        "/gamma/bpBanana/green",
        "/gamma/bpBanana/brown"
    ));

    retMap.put("deletedDataObjects", Arrays.asList(
        "/alpha/bpath1/myfile1.txt",
        "/alpha/bpath1/myfile2.txt",
        "/alpha/bpath1/myfile3.txt",
        "/beta/bpathX/sampleDoc1.pdf",
        "/beta/bpathX/sampleDoc2.pdf",
        "/gamma/bpApple/petPhotoA.jpg",
        "/gamma/bpApple/petPhotoB.jpg",
        "/gamma/bpApple/petPhotoC.jpg",
        "/zeta/books-vol1.xlsx",
        "/zeta/books-vol2.xlsx",
        "/zeta/books-vol3.xlsx"
    ));

    retMap.put("outstandingCollections", Arrays.asList(
        new ResidualItem("/alpha/bpath2/leftover", "Reason 1"),
        new ResidualItem("/beta/bpathX/cuatro", "Reason 2"),
        new ResidualItem("/gamma/bpBanana/black", "Reason 3")
    ));

    retMap.put("outstandingDataObjects", Arrays.asList(
        new ResidualItem("/alpha/bpath1/my-leftover.docx", "Reason X"),
        new ResidualItem("/beta/bpathX/remaining.pdf", "Reason Y"),
        new ResidualItem("/gamma/bpBanana/crap.xml", "Reason Z")
    ));

    Calendar cal = Calendar.getInstance();
    cal.set(2018, Calendar.JANUARY, 1, 9, 30, 42);
    retMap.put("startTime", cal.getTime());

    cal.add(Calendar.MINUTE, 10);
    cal.add(Calendar.SECOND, 23);
    retMap.put("finishTime", cal.getTime());

    return retMap;
  }

}
