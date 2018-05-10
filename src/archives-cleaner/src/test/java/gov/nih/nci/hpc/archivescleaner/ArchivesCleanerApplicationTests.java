package gov.nih.nci.hpc.archivescleaner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.html.Option;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ArchivesCleanerApplicationTests {

  public static final List<String> SPEC_DOC_LIST = Arrays.asList("HiTIF", "BURGER_KING");


  public static final Pattern REGEX_PATTERN_$DME_BASEPATH_CHILD = Pattern.compile("/[^/]+/[^/]+");

  //  private static final String REGEX_BP_IMMED_CHILD = "/[^/]+/[^/]+";
  private static final String SPACER_INDENT = "     ";


  @Autowired
  private ArchivesCleanerDao archivesCleanerDao;


  @Before
  public void initCleanerDao() {
    this.archivesCleanerDao.setTargetedDocs(Optional.empty());
  }


  @Test
	public void contextLoads() {
    System.out.println(this.getClass().getCanonicalName() + ": contextLoads started.");
    System.out.println(this.getClass().getCanonicalName() + ": contextLoads finished.");
  }


	@Test
  public void testRunQueryForCollections_defaultDocs() {
    System.out.println(this.getClass().getCanonicalName() +
      ": testRunQueryForCollections_defaultDocs started.");

    final List<String> dmeColls = this.archivesCleanerDao.runQueryForCollections();
    Assert.notNull(dmeColls, "dmeColls should NOT be null!");
    System.out.println(SPACER_INDENT + "Not null validation passed. # elements = " + dmeColls.size());

    for (String someDmeColl : dmeColls) {
      validateCollIsBasePathChild(someDmeColl);
    }

    System.out.println(this.getClass().getCanonicalName() +
      ": testRunQueryForCollections_defaultDocs finished.");
  }


  @Test
  public void testRunQueryForCollections_nonDefaultDocs() {
    System.out.println(this.getClass().getCanonicalName() +
        ": testRunQueryForCollections_nonDefaultDocs started.");

    this.archivesCleanerDao.setTargetedDocs(Optional.of(SPEC_DOC_LIST));
    final List<String> dmeColls = this.archivesCleanerDao.runQueryForCollections();
    Assert.notNull(dmeColls, "dmeColls should NOT be null!");
    System.out.println(SPACER_INDENT + "Not null validation passed. # elements = " + dmeColls.size());

    for (String someDmeColl : dmeColls) {
      validateCollIsBasePathChild(someDmeColl);
    }

    System.out.println(this.getClass().getCanonicalName() +
        ": testRunQueryForCollections_nonDefaultDocs finished.");
  }


  @Test
  public void testRunQueryForDataObjects_defaultDocs() {
	  System.out.println(this.getClass().getCanonicalName() +
      ": testRunQueryForDataObjects_defaultDocs started.");

	  List<String> dmeDataObjs = this.archivesCleanerDao.runQueryForDataObjects();
	  Assert.notNull(dmeDataObjs, "dmeDataObjs should NOT be null!");
    System.out.println(SPACER_INDENT + "Not null validation passed. # elements = " + dmeDataObjs.size());

    for (String someDmeDataObj : dmeDataObjs) {
      validateDataObjIsBasePathChild(someDmeDataObj);
    }

    System.out.println(this.getClass().getCanonicalName() +
      ": testRunQueryForDataObjects_defaultDocs finished.");
	}


  @Test
  public void testRunQueryForDataObjects_nonDefaultDocs() {
    System.out.println(this.getClass().getCanonicalName() +
        ": testRunQueryForDataObjects_nonDefaultDocs started.");

    this.archivesCleanerDao.setTargetedDocs(Optional.of(SPEC_DOC_LIST));
    List<String> dmeDataObjs = this.archivesCleanerDao.runQueryForDataObjects();
    Assert.notNull(dmeDataObjs, "dmeDataObjs should NOT be null!");
    System.out.println(SPACER_INDENT + "Not null validation passed. # elements = " + dmeDataObjs.size());

    for (String someDmeDataObj : dmeDataObjs) {
      validateDataObjIsBasePathChild(someDmeDataObj);
    }

    System.out.println(this.getClass().getCanonicalName() +
      ": testRunQueryForDataObjects_nonDefaultDocs finished.");
  }

	@Test
	public void testTargetedDocsGetterSetterPair() {
    System.out.println(this.getClass().getCanonicalName() +
      ": testTargetedDocsGetterSetterPair started.");

    this.archivesCleanerDao.setTargetedDocs(Optional.of(SPEC_DOC_LIST));
    final Optional<List<String>> queriedList = this.archivesCleanerDao.getTargetedDocs();
    Assert.isTrue(queriedList.isPresent(), "queriedList is empty Optional!");
    Assert.isTrue(queriedList.get().equals(SPEC_DOC_LIST), "queriedList should match sampleList!" +
      "  queriedList = [" + queriedList.get() + "]; sampleList = [" + SPEC_DOC_LIST + "].");

    System.out.println(this.getClass().getCanonicalName() +
      ": testTargetedDocsGetterSetterPair finished.");
  }


  private void validateCollIsBasePathChild(String item) {
    System.out.print(SPACER_INDENT + "Validating form of collection " +
      item + " ...");
    if (REGEX_PATTERN_$DME_BASEPATH_CHILD.matcher(item).matches()) {
      System.out.println("passed.");
    } else {
      System.out.println("failed!");
      fail("Collection " + item + " does NOT have expected form" +
        "(/<base-path>/<collection-name>).");
    }
  }


  private void validateDataObjIsBasePathChild(String item) {
    System.out.print(SPACER_INDENT + "Validating form of data object " +
      item + " ...");
    if (REGEX_PATTERN_$DME_BASEPATH_CHILD.matcher(item).matches()) {
      System.out.println("passed.");
    } else {
      System.out.println("failed!");
      fail("Data object " + item + " does NOT have expected form" +
          "(/<base-path>/<data-obj-name>).");
    }
  }

}
