package gov.nih.nci.hpc.ws.rs.test;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestServiceImpl;

public class HpcDataManagementRestServiceImplTest {

	@Mock
	private HpcDataManagementBusService dataManagementBusService;

	private HpcDataManagementRestServiceImpl service;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		Constructor<HpcDataManagementRestServiceImpl> constructor = HpcDataManagementRestServiceImpl.class
				.getDeclaredConstructor();
		constructor.setAccessible(true);
		service = constructor.newInstance();
		ReflectionTestUtils.setField(service, "dataManagementBusService", dataManagementBusService);
	}

	@Test
	public void testRegisterCollection_OmittedGenerateMetadataVectorDefaultsToFalse() throws Exception {
		HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
		when(dataManagementBusService.registerCollection("/test/path", collectionRegistration, false)).thenReturn(true);

		service.registerCollection("/test/path", collectionRegistration);

		verify(dataManagementBusService).registerCollection("/test/path", collectionRegistration, false);
	}

	@Test
	public void testRegisterCollection_GenerateMetadataVectorFalseDelegatesFalse() throws Exception {
		HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
		collectionRegistration.setGenerateMetadataVector(false);
		when(dataManagementBusService.registerCollection("/test/path", collectionRegistration, false)).thenReturn(true);

		service.registerCollection("/test/path", collectionRegistration);

		verify(dataManagementBusService).registerCollection("/test/path", collectionRegistration, false);
	}

	@Test
	public void testRegisterCollection_GenerateMetadataVectorTrueDelegatesTrue() throws Exception {
		HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
		collectionRegistration.setGenerateMetadataVector(true);
		when(dataManagementBusService.registerCollection("/test/path", collectionRegistration, true)).thenReturn(true);

		service.registerCollection("/test/path", collectionRegistration);

		verify(dataManagementBusService).registerCollection("/test/path", collectionRegistration, true);
	}
}
