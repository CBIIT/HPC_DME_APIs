package gov.nih.nci.hpc.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
//import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.dao.HpcDataManagementConfigurationDAO;
import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;

@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations={"/hpc-app-service-beans-configuration.xml"})
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
	    DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class })
public class HpcDataTransferServiceImplTest {
	
	@Configuration
	static class HpcDataTransferServiceImplTestContextConfiguration {
		
		
	    @Bean
	    public HpcDataTransferProxy hpcGlobusDataTransferProxy() {
	        return Mockito.mock(HpcDataTransferProxy.class);
	    }
	    @Autowired
	    private HpcDataTransferProxy hpcGlobusDataTransferProxy;
	  
	
	    @Bean
	    public HpcDataTransferProxy hpcS3DataTransferProxy() {
	        return Mockito.mock(HpcDataTransferProxy.class);
	    }
	    @Autowired
	    private HpcDataTransferProxy hpcS3DataTransferProxy;
	    
	    
		@Bean
		public HpcDataTransferService dataTransferService() throws HpcException {
			
			Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = new HashMap<HpcDataTransferType, HpcDataTransferProxy>();
			dataTransferProxies.put(HpcDataTransferType.GLOBUS, hpcGlobusDataTransferProxy);
			dataTransferProxies.put(HpcDataTransferType.S_3, hpcS3DataTransferProxy);
			
			HpcDataTransferUploadReport uploadReport = new HpcDataTransferUploadReport();
			uploadReport.setBytesTransferred(2000);
			
			HpcDataTransferAuthenticatedToken token = new HpcDataTransferAuthenticatedToken();
			token.setDataTransferType(HpcDataTransferType.GLOBUS);
			token.setConfigurationId("configId");
			HpcRequestInvoker invoker = new HpcRequestInvoker();
			List<HpcDataTransferAuthenticatedToken> tokens = invoker.getDataTransferAuthenticatedTokens();
			tokens.add(token);
			HpcRequestContext.setRequestInvoker(invoker);
			
			//Mockito.when(hpcGlobusDataTransferProxy.getDataTransferUploadStatus(Mockito.anyObject(), Mockito.anyString(), Mockito.anyObject())).thenReturn(uploadReport);
			Mockito.doReturn(uploadReport).when(hpcGlobusDataTransferProxy).getDataTransferUploadStatus(Mockito.anyObject(), Mockito.anyString(), Mockito.anyObject());
				
			HpcDataTransferService dataTransferService = new HpcDataTransferServiceImpl(dataTransferProxies, "./");
			return dataTransferService;
			
			
		}
		
		@Bean
		public HpcSystemAccountLocator hpcSystemAccountLocator() throws HpcException {
			return Mockito.mock(HpcSystemAccountLocator.class);
		}
		
		@Bean
		public HpcSystemAccountDAO systemAccountDAO() {
			return Mockito.mock(HpcSystemAccountDAO.class);
		}
		
		@Bean
		public HpcDataManagementConfigurationLocator dataManagementConfigurationLocator() throws HpcException {
			
			HpcDataManagementConfigurationLocator dataManagementConfigurationLocator =  Mockito.mock(HpcDataManagementConfigurationLocator.class);
			
			HpcDataTransferConfiguration dataTransferConfiguration = new HpcDataTransferConfiguration();
		    Mockito.doReturn(dataTransferConfiguration).when(dataManagementConfigurationLocator).getDataTransferConfiguration(Mockito.anyString(), Mockito.anyObject());
		    return dataManagementConfigurationLocator;
		}
		
		@Bean 
		public HpcDataManagementConfigurationDAO dataManagementConfigurationDAO() {
			return Mockito.mock(HpcDataManagementConfigurationDAO.class);
		}
		
		@Bean 
		public HpcDataManagementProxy dataManagementProxy() {
			return Mockito.mock(HpcDataManagementProxy.class);
		}
		
		@Bean
		public HpcDataDownloadDAO dataDowbloadDAO() {
			return Mockito.mock(HpcDataDownloadDAO.class);
		}
		
		@Bean
		public HpcEventService hpcEventService() {
			return Mockito.mock(HpcEventService.class);
		}
		
		@Bean
		public HpcPagination hpcDownloadResultsPagination() {
			return Mockito.mock(HpcPagination.class);
		}
	}
	
	@Autowired 
	private HpcDataTransferService dataTransferService;
	
	
	@Test
	public void calculateDataObjectUploadPercentCompleteTest() {
		
		//Test input
		HpcSystemGeneratedMetadata metadata = new HpcSystemGeneratedMetadata();
		metadata.setDataTransferRequestId("requestId");
	    metadata.setConfigurationId("configId");
	    metadata.setSourceSize(5000L);
	    
		//Null if either of data transfer type or status is null
		
		metadata.setDataTransferStatus(HpcDataTransferUploadStatus.RECEIVED);
		metadata.setDataTransferType(null);
		Integer result = dataTransferService.calculateDataObjectUploadPercentComplete(metadata);
		Assert.assertEquals(null, result);
	
		metadata.setDataTransferStatus(null);
		metadata.setDataTransferType(HpcDataTransferType.S_3);
		result = dataTransferService.calculateDataObjectUploadPercentComplete(metadata);
		Assert.assertEquals(null, result);
		
		//50% if in temporary archive
		metadata.setDataTransferType(HpcDataTransferType.S_3);
		metadata.setDataTransferStatus(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE);
		result = dataTransferService.calculateDataObjectUploadPercentComplete(metadata);
		Assert.assertEquals(new Integer(50), result);
		
		//50% if in transfer in progress and S3 type
		metadata.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE);
		result = dataTransferService.calculateDataObjectUploadPercentComplete(metadata);
		Assert.assertEquals(new Integer(50), result);
		
		metadata.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE);
		result = dataTransferService.calculateDataObjectUploadPercentComplete(metadata);
		Assert.assertEquals(new Integer(50), result);
		
		//Computed value
		metadata.setDataTransferType(HpcDataTransferType.GLOBUS);
		result = dataTransferService.calculateDataObjectUploadPercentComplete(metadata);
		Assert.assertEquals(new Integer(40), result);
		
		
	}
}