package gov.nih.nci.hpc.archivescleaner;

import gov.nih.nci.hpc.archivescleaner.ArchivesCleanerApplication.ResidualItem;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReportGeneratorTests {

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

}
