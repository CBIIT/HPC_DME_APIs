/**
 * HpcVectorSearchServiceImplTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.junit.Assert.assertEquals;
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
 * HPC Vector Search Service Implementation Test.
 */
@RunWith(MockitoJUnitRunner.class)
public class HpcVectorSearchServiceImplTest {

    private static final String QUERY_TEXT = "find similar collections";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private HpcTextEmbeddingProxy hpcTextEmbeddingProxy;
    @Mock
    private HpcVectorStoreProxy hpcVectorStoreProxy;

    @Test
    public void testQueryCollectionsBlankQueryText() throws HpcException {
        HpcVectorSearchServiceImpl service = createService();

        expectedException.expect(HpcException.class);
        expectedException.expectMessage("Query text cannot be blank");

        service.queryCollections(" ", 5);
    }

    @Test
    public void testQueryCollectionsInvalidMaxResults() throws HpcException {
        HpcVectorSearchServiceImpl service = createService();

        expectedException.expect(HpcException.class);
        expectedException.expectMessage("maxResults must be greater than zero");

        service.queryCollections(QUERY_TEXT, 0);
    }

    @Test
    public void testQueryCollectionsSuccess() throws HpcException {
        HpcVectorSearchServiceImpl service = createService();
        List<Float> queryVector = Arrays.asList(0.4f, 0.5f, 0.6f);
        List<String> collectionIds = Arrays.asList("COLL_1", "COLL_2");
        when(hpcTextEmbeddingProxy.getEmbeddingVector(QUERY_TEXT)).thenReturn(queryVector);
        when(hpcVectorStoreProxy.findCollectionIds(queryVector, 2)).thenReturn(collectionIds);

        List<String> results = service.queryCollections(QUERY_TEXT, 2);

        assertEquals(collectionIds, results);
        verify(hpcTextEmbeddingProxy).getEmbeddingVector(QUERY_TEXT);
        verify(hpcVectorStoreProxy).findCollectionIds(queryVector, 2);
    }

    private HpcVectorSearchServiceImpl createService() {
        HpcVectorSearchServiceImpl service = new HpcVectorSearchServiceImpl();
        ReflectionTestUtils.setField(service, "hpcTextEmbeddingProxy", hpcTextEmbeddingProxy);
        ReflectionTestUtils.setField(service, "hpcVectorStoreProxy", hpcVectorStoreProxy);
        return service;
    }
}
