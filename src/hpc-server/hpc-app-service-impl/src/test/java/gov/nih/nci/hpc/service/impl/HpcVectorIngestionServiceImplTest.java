/**
 * HpcVectorIngestionServiceImplTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcTextEmbeddingProxy;
import gov.nih.nci.hpc.integration.HpcVectorStoreProxy;

/**
 * HPC Vector Ingestion Service Implementation Test.
 */
@RunWith(MockitoJUnitRunner.class)
public class HpcVectorIngestionServiceImplTest {

    private static final String COLLECTION_ID = "COLL_123";
    private static final String TEXT = "sample collection text";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private HpcTextEmbeddingProxy hpcTextEmbeddingProxy;
    @Mock
    private HpcVectorStoreProxy hpcVectorStoreProxy;

    @Test
    public void testIndexCollectionBlankCollectionId() throws HpcException {
        HpcVectorIngestionServiceImpl service = createService();

        expectedException.expect(HpcException.class);
        expectedException.expectMessage("Collection ID cannot be blank");

        service.indexCollection(" ", TEXT);
    }

    @Test
    public void testIndexCollectionBlankText() throws HpcException {
        HpcVectorIngestionServiceImpl service = createService();

        expectedException.expect(HpcException.class);
        expectedException.expectMessage("Input text cannot be blank");

        service.indexCollection(COLLECTION_ID, " ");
    }

    @Test
    public void testIndexCollectionSuccess() throws HpcException {
        HpcVectorIngestionServiceImpl service = createService();
        List<Float> embeddingVector = Arrays.asList(0.1f, 0.2f, 0.3f);
        when(hpcTextEmbeddingProxy.getEmbeddingVector(TEXT)).thenReturn(embeddingVector);

        service.indexCollection(COLLECTION_ID, TEXT);

        verify(hpcTextEmbeddingProxy).getEmbeddingVector(TEXT);
        verify(hpcVectorStoreProxy).storeVector(embeddingVector, COLLECTION_ID);
    }

    private HpcVectorIngestionServiceImpl createService() {
        HpcVectorIngestionServiceImpl service = new HpcVectorIngestionServiceImpl();
        ReflectionTestUtils.setField(service, "hpcTextEmbeddingProxy", hpcTextEmbeddingProxy);
        ReflectionTestUtils.setField(service, "hpcVectorStoreProxy", hpcVectorStoreProxy);
        return service;
    }
}
