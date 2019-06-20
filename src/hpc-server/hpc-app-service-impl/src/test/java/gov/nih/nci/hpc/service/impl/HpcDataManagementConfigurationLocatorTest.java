/**
 * HpcDataManagementConfigurationLocatorTest.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
// import org.springframework.test.util.ReflectionTestUtils;
import gov.nih.nci.hpc.dao.HpcDataManagementConfigurationDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.model.HpcArchiveDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;


/**
 * HPC Data Management Configuration Locator Test.
 *
 * @author <a href="mailto:sunita.menon@nih.gov">Sunita Menon</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class HpcDataManagementConfigurationLocatorTest {

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//


  @InjectMocks
  HpcDataManagementConfigurationLocator dataManagementConfigurationLocator;


  // The Data Management Configuration DAO instance.
  @Mock
  HpcDataManagementConfigurationDAO dataManagementConfigurationDAO;

  @Mock
  HpcDataManagementProxy dataManagementProxy;

  // Expected exception rule.
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  List<HpcDataManagementConfiguration> configurations = null;

  @Before
  public void init() throws HpcException {

    configurations = new ArrayList<HpcDataManagementConfiguration>();
    HpcDataManagementConfiguration configuration = new HpcDataManagementConfiguration();
    configuration.setId("someId1");
    String basepath = "/CCR_CMM_Archive";
    configuration.setDoc("CMM");
    configuration.setBasePath(basepath);
    configuration.setArchiveType(HpcArchiveType.CLEVERSAFE);

    HpcArchiveDataTransferConfiguration s3Configuration = new HpcArchiveDataTransferConfiguration();
    s3Configuration.setUploadRequestURLExpiration(100);
    configuration.setArchiveS3Configuration(s3Configuration);
    HpcArchiveDataTransferConfiguration globusConfiguration =
        new HpcArchiveDataTransferConfiguration();
    configuration.setArchiveGlobusConfiguration(globusConfiguration);

    configurations.add(configuration);

    when(dataManagementConfigurationDAO.getDataManagementConfigurations())
        .thenReturn(configurations);
    // ReflectionTestUtils.setField(dataManagementConfigurationLocator,
    // "dataManagementConfigurationDAO", dataManagementConfigurationDAO);

    when(dataManagementProxy.getRelativePath(basepath)).thenReturn(basepath);
    // ReflectionTestUtils.setField(dataManagementConfigurationLocator, "dataManagementProxy",
    // dataManagementProxy);

    // This causes the DAO to update the configuratorLocator cache
    dataManagementConfigurationLocator.reload();
  }

  // ---------------------------------------------------------------------//
  // Unit Tests
  // ---------------------------------------------------------------------//

  /**
   * Success test: get docs
   * 
   * @throws HpcException
   */
  @Test
  public void testGetDocs() throws HpcException {

    // Read the value from the configuratorLocator - it should be same as the setup data
    Set<String> docs = dataManagementConfigurationLocator.getDocs();
    assertNotNull(docs);
    assertEquals(docs.size(), 1);
    assertTrue(docs.contains("CMM"));

  }


  /**
   * Success test: get archiveDataTransferType
   * 
   * @throws HpcException
   */
  @Test
  public void testGetArchiveType() throws HpcException {

    // This is from the setup data loaded in init method
    HpcArchiveType type = dataManagementConfigurationLocator.getArchiveType("someId1");
    assertEquals(type, HpcArchiveType.CLEVERSAFE);

  }


  /**
   * Success test: getConfigurationId
   */
  @Test
  public void testGetConfigurationId() {
    String configurationId =
        dataManagementConfigurationLocator.getConfigurationId("/CCR_CMM_Archive");
    assertEquals(configurationId, "someId1");

  }

  /**
   * Success test: get dataTransferconfiguration
   * 
   * @throws HpcException
   */
  @Test
  public void testGetDataTransferConfigurationC() throws HpcException {

    // This is from the setup data loaded in init method
    HpcArchiveDataTransferConfiguration transferConfig = dataManagementConfigurationLocator
        .getArchiveDataTransferConfiguration("someId1", HpcDataTransferType.S_3);
    assertEquals(transferConfig.getUploadRequestURLExpiration(), new Integer(100));
  }


  /**
   * Failure test: Throw invalid path exception if basepath format not correct
   * 
   * @throws HpcException
   */
  @Test
  public void testInvalidPathNoSlash() throws HpcException {

    // Setup test data

    HpcDataManagementConfiguration configuration = new HpcDataManagementConfiguration();
    configuration.setId("someId2");
    String basepath = "FNL_SF_Archive";
    configuration.setDoc("FNLCR");
    configuration.setBasePath(basepath);

    configurations.add(configuration);
    when(dataManagementConfigurationDAO.getDataManagementConfigurations())
        .thenReturn(configurations);
    when(dataManagementProxy.getRelativePath(basepath)).thenReturn(basepath);

    // Exception is thrown because there is no "/" in the basepath
    expectedException.expect(HpcException.class);
    expectedException
        .expectMessage("Invalid base path [" + basepath + "]. Only one level path supported.");

    dataManagementConfigurationLocator.reload();

  }


  /**
   * Failure test: Throw invalid path exception if basepath format not correct
   * 
   * @throws HpcException
   */
  @Test
  public void testInvalidPathMultipleSlashes() throws HpcException {

    // Setup test data

    HpcDataManagementConfiguration configuration = new HpcDataManagementConfiguration();
    configuration.setId("someId2");
    String basepath = "/FNL_SF_Archive/Test";
    configuration.setDoc("FNLCR");
    configuration.setBasePath(basepath);

    configurations.add(configuration);
    when(dataManagementConfigurationDAO.getDataManagementConfigurations())
        .thenReturn(configurations);
    when(dataManagementProxy.getRelativePath(basepath)).thenReturn(basepath);

    // Exception is thrown because there is no "/" in the basepath
    expectedException.expect(HpcException.class);
    expectedException
        .expectMessage("Invalid base path [" + basepath + "]. Only one level path supported.");

    dataManagementConfigurationLocator.reload();

  }


  /**
   * Failure test: Throw invalid path exception if basepath format not correct
   * 
   * @throws HpcException
   */
  @Test
  public void testInvalidPathDuplicate() throws HpcException {

    // Setup test data

    HpcDataManagementConfiguration configuration = new HpcDataManagementConfiguration();
    configuration.setId("someId2");
    String basepath = "/CCR_CMM_Archive";
    configuration.setDoc("FNLCR");
    configuration.setBasePath(basepath);

    configurations.add(configuration);
    when(dataManagementConfigurationDAO.getDataManagementConfigurations())
        .thenReturn(configurations);
    when(dataManagementProxy.getRelativePath(basepath)).thenReturn(basepath);

    // Exception is thrown because there is no "/" in the basepath
    expectedException.expect(HpcException.class);
    expectedException
        .expectMessage("Duplicate base-path in data management configurations:/CCR_CMM_Archive");

    dataManagementConfigurationLocator.reload();

  }

  /**
   * Failure test: get archiveDataTransferType
   * 
   * @throws HpcException
   */
  @Test
  public void testNoArchiveType() throws HpcException {

    // Exception is thrown because someIdx configuration id does not exist
    expectedException.expect(HpcException.class);
    expectedException.expectMessage("Could not locate configuration: someIdx");

    // This is from the setup data loaded in init method
    HpcArchiveType type = dataManagementConfigurationLocator.getArchiveType("someIdx");
    assertEquals(type, HpcArchiveType.CLEVERSAFE);

  }


  /**
   * Failure test: get dataTransferconfiguration
   * 
   * @throws HpcException
   */
  @Test
  public void testNoDataTransferConfiguration() throws HpcException {


    // Exception is thrown because someIdx configuration id does not exist
    expectedException.expect(HpcException.class);
    expectedException.expectMessage("Could not locate data transfer configuration: " + "someIdx"
        + " " + HpcDataTransferType.S_3);

    dataManagementConfigurationLocator.getArchiveDataTransferConfiguration("someIdx",
        HpcDataTransferType.S_3);


  }



}
