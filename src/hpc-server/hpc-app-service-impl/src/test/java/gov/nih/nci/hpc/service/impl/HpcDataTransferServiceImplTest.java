/**
 * HpcDataTransferServiceImplTest.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

/**
 * HPC Data Transfer Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class HpcDataTransferServiceImplTest {

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The app service under test.
  //@InjectMocks
  private HpcDataTransferService dataTransferService = null;

  // Expected exception rule.
  @Rule public ExpectedException expectedException = ExpectedException.none();

  // Mocks.
  @Mock private HpcDataTransferProxy dataTransferProxyMock = null;
  @Mock private HpcDataManagementConfigurationLocator dataManagementConfigurationLocatorMock = null;
  @Mock private HpcSystemAccountLocator systemAccountLocatorMock = null;

  //---------------------------------------------------------------------//
  // Unit Tests
  //---------------------------------------------------------------------//

  /**
   * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation,
   * gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
   */
  /* Test scenario: null dataTransferType
   * Expected: HpcException - "Invalid generate download URL request"
   */
  @Test
  public void testGenerateDownloadRequestURLNullDataTransferType() throws HpcException {
    expectedException.expect(HpcException.class);
    expectedException.expectMessage("Invalid generate download URL request");

    dataTransferService.generateDownloadRequestURL("", new HpcFileLocation(), null, "");
  }

  /**
   * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation,
   * gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
   */
  /* Test scenario: Invalid archiveLocation
   * Expected: HpcException - "Invalid generate download URL request"
   */
  @Test
  public void testGenerateDownloadRequestURLInvalidArchiveLocation() throws HpcException {
    expectedException.expect(HpcException.class);
    expectedException.expectMessage("Invalid generate download URL request");

    dataTransferService.generateDownloadRequestURL(
        "", new HpcFileLocation(), HpcDataTransferType.S_3, "");
  }

  /**
   * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation,
   * gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
   */
  /* Test scenario: Successful
   * Expected: A download URL - "https://downloadURL"
   */
  @Test
  public void testGenerateDownloadRequestURL() throws HpcException {
    // Mock setup.
    when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(
            anyObject(), anyObject()))
        .thenReturn(new HpcDataTransferConfiguration());
    when(systemAccountLocatorMock.getSystemAccount(anyObject(), anyObject()))
        .thenReturn(new HpcIntegratedSystemAccount());
    when(dataTransferProxyMock.authenticate(anyObject(), anyObject())).thenReturn("token");
    when(dataTransferProxyMock.generateDownloadRequestURL(anyObject(), anyObject(), anyObject()))
        .thenReturn("https://downloadURL");

    HpcFileLocation archiveLocation = new HpcFileLocation();
    archiveLocation.setFileContainerId("test");
    archiveLocation.setFileId("test");

    String downloadURL =
        dataTransferService.generateDownloadRequestURL(
            "", archiveLocation, HpcDataTransferType.S_3, "");

    // Assert expected result.
    assertEquals(downloadURL, "https://downloadURL");
  }

  /**
   * {@link HpcDataTransferService#downloadDataObject(String, HpcFileLocation,
   * gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination,
   * gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination, HpcDataTransferType, String,
   * String, boolean, long)}
   */
  /* Test scenario: null dataTransferType
   * Expected: HpcException - "Invalid data transfer request"
   */
  @Test
  public void testDownloadDataObjectNullDataTransferType() throws HpcException {
    expectedException.expect(HpcException.class);
    expectedException.expectMessage("Invalid data transfer request");

    dataTransferService.downloadDataObject("", null, null, null, null, "", "", false, 0L);
  }

  /**
   * {@link HpcDataTransferService#downloadDataObject(String, HpcFileLocation,
   * gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination,
   * gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination, HpcDataTransferType, String,
   * String, boolean, long)}
   */
  /* Test scenario: Invalid archiveLocation
   * Expected: HpcException - "Invalid data transfer request"
   */
  @Test
  public void testDownloadDataObjectInvalidArchiveLocation() throws HpcException {
    expectedException.expect(HpcException.class);
    expectedException.expectMessage("Invalid data transfer request");

    dataTransferService.downloadDataObject(
        "", new HpcFileLocation(), null, null, HpcDataTransferType.S_3, "", "", false, 0L);
  }
  
  /**
   * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation,
   * gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
   */
  /* Test scenario: Successful
   * Expected: HpcDataObjectDownloadResponse object w/ download task ID
   */
  @Test
  public void testDownloadDataObject() throws HpcException {
    // Mock setup.
    /*
    when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(
            anyObject(), anyObject()))
        .thenReturn(new HpcDataTransferConfiguration());
    when(systemAccountLocatorMock.getSystemAccount(anyObject(), anyObject()))
        .thenReturn(new HpcIntegratedSystemAccount());
    when(dataTransferProxyMock.authenticate(anyObject(), anyObject())).thenReturn("token");
    when(dataTransferProxyMock.generateDownloadRequestURL(anyObject(), anyObject(), anyObject()))
        .thenReturn("https://downloadURL");

    HpcFileLocation archiveLocation = new HpcFileLocation();
    archiveLocation.setFileContainerId("test");
    archiveLocation.setFileId("test");

    HpcDataObjectDownloadResponse downloadResponse =
        dataTransferService.downloadDataObject(
            "", new HpcFileLocation(), null, null, HpcDataTransferType.S_3, "", "", false, 0L);

    // Assert expected result.
    assertEquals(downloadResponse.getDownloadTaskId(), "test-task-id");*/
  }

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//

  @Before
  public void init() throws HpcException {
    Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = new HashMap<>();
    dataTransferProxies.put(HpcDataTransferType.GLOBUS, dataTransferProxyMock);
    dataTransferProxies.put(HpcDataTransferType.S_3, dataTransferProxyMock);

    HpcDataTransferServiceImpl dataTransferServiceImpl =
        new HpcDataTransferServiceImpl(dataTransferProxies, "/test/download/directory");
    dataTransferServiceImpl.setDataManagementConfigurationLocator(
        dataManagementConfigurationLocatorMock);
    dataTransferServiceImpl.setSystemAccountLocator(systemAccountLocatorMock);

    dataTransferService = dataTransferServiceImpl;
  }
}
