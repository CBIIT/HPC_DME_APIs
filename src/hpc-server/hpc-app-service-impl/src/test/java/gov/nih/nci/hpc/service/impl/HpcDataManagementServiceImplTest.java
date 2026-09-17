package gov.nih.nci.hpc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.exception.HpcException;

@ExtendWith(MockitoExtension.class)
class HpcDataManagementServiceImplTest {

	@Mock
	private HpcDataRegistrationDAO dataRegistrationDAO;

	private HpcDataManagementServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new HpcDataManagementServiceImpl();
		setPrivateField("dataRegistrationDAO", dataRegistrationDAO);
	}

	@Test
	void testUpdateBulkDataObjectRegistrationTaskSetsTotalBytesTransferred() throws HpcException {
		HpcBulkDataObjectRegistrationTask task = new HpcBulkDataObjectRegistrationTask();
		task.getItems().add(createItem(100L, 100, true, null));
		task.getItems().add(createItem(200L, 25, null, null));
		task.getItems().add(createItem(400L, 100, true, "/source/path"));
		task.getItems().add(createItem(300L, null, false, null));

		service.updateBulkDataObjectRegistrationTask(task);

		ArgumentCaptor<HpcBulkDataObjectRegistrationTask> captor = ArgumentCaptor
				.forClass(HpcBulkDataObjectRegistrationTask.class);
		verify(dataRegistrationDAO).upsertBulkDataObjectRegistrationTask(captor.capture());
		assertEquals(150L, captor.getValue().getTotalBytesTransferred());
	}

	@Test
	void testCompleteBulkDataObjectRegistrationTaskCopiesTotalBytesTransferredToResult() throws HpcException {
		HpcBulkDataObjectRegistrationTask task = new HpcBulkDataObjectRegistrationTask();
		task.setId("task-id");
		task.setUserId("user-id");
		task.setCreated(Calendar.getInstance());
		task.setUploadMethod(HpcDataTransferUploadMethod.S_3);
		task.setRegistrationSize(600L);
		task.getItems().add(createItem(100L, 100, true, null));
		task.getItems().add(createItem(200L, 50, true, "/source/path"));
		task.getItems().add(createItem(300L, null, false, null));

		Calendar completed = Calendar.getInstance();
		service.completeBulkDataObjectRegistrationTask(task, false, "failed", completed);

		verify(dataRegistrationDAO).deleteBulkDataObjectRegistrationTask("task-id");
		ArgumentCaptor<HpcBulkDataObjectRegistrationResult> captor = ArgumentCaptor
				.forClass(HpcBulkDataObjectRegistrationResult.class);
		verify(dataRegistrationDAO).upsertBulkDataObjectRegistrationResult(captor.capture());
		assertEquals(100L, captor.getValue().getTotalBytesTransferred());
		assertEquals(600L, captor.getValue().getRegistrationSize());
	}

	private HpcBulkDataObjectRegistrationItem createItem(Long size, Integer percentComplete, Boolean result,
			String linkSourcePath) {
		HpcBulkDataObjectRegistrationItem item = new HpcBulkDataObjectRegistrationItem();
		HpcDataObjectRegistrationTaskItem taskItem = new HpcDataObjectRegistrationTaskItem();
		taskItem.setPath("/path");
		taskItem.setSize(size);
		taskItem.setPercentComplete(percentComplete);
		taskItem.setResult(result);
		item.setTask(taskItem);

		HpcDataObjectRegistrationRequest request = new HpcDataObjectRegistrationRequest();
		request.setLinkSourcePath(linkSourcePath);
		item.setRequest(request);
		return item;
	}

	private void setPrivateField(String fieldName, Object value) {
		try {
			Field field = HpcDataManagementServiceImpl.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(service, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to set field: " + fieldName, e);
		}
	}
}
