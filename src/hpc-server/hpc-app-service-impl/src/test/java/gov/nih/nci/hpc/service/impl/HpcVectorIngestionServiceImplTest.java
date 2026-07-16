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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcTextEmbeddingProxy;
import gov.nih.nci.hpc.integration.HpcVectorStoreProxy;
import gov.nih.nci.hpc.service.HpcMetadataService;

/**
 * HPC Vector Ingestion Service Implementation Test.
 */
@RunWith(MockitoJUnitRunner.class)
public class HpcVectorIngestionServiceImplTest {

    private static final String COLLECTION_PATH = "/path/to/collection";
    private static final String TEMPLATE = "Embed this metadata: {{metadata}}";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private HpcVectorIngestionServiceImpl service;

    @Mock
    private HpcTextEmbeddingProxy hpcTextEmbeddingProxy;
    @Mock
    private HpcVectorStoreProxy hpcVectorStoreProxy;
    @Mock
    private HpcMetadataService hpcMetadataService;
    @Mock
    private HpcMetadataNormalizationLocator hpcMetadataNormalizationLocator;

    @Test
    public void testIndexCollectionBlankCollectionPath() throws HpcException {
        expectedException.expect(HpcException.class);
        expectedException.expectMessage("Collection path cannot be blank");

        service.indexCollection(" ");
    }

    @Test
    public void testIndexCollectionMetadataRetrievalFailure() throws HpcException {
        ReflectionTestUtils.setField(service, "embeddingTemplate", TEMPLATE);
        when(hpcMetadataService.getCollectionMetadataEntries(COLLECTION_PATH))
                .thenThrow(new HpcException("metadata failure", HpcErrorType.UNEXPECTED_ERROR));

        expectedException.expect(HpcException.class);
        expectedException.expectMessage("metadata failure");

        service.indexCollection(COLLECTION_PATH);
    }

    @Test
    public void testIndexCollectionNormalizationHitAndTemplateRendering() throws HpcException {
        ReflectionTestUtils.setField(service, "embeddingTemplate", TEMPLATE);
        List<Float> embeddingVector = Arrays.asList(0.1f, 0.2f, 0.3f);
        HpcMetadataEntries metadataEntries = new HpcMetadataEntries();
        metadataEntries.getSelfMetadataEntries().add(toMetadataEntry("tumor_type", "Lung"));
        metadataEntries.getSelfMetadataEntries().add(toMetadataEntry("assay", "RNASeq"));
        metadataEntries.getSelfMetadataEntries().add(toMetadataEntry("ignored_system", "x"));

        Map<String, String> normalizationMap = new LinkedHashMap<>();
        normalizationMap.put("tumor_type", "disease_type");

        when(hpcMetadataService.getCollectionMetadataEntries(COLLECTION_PATH)).thenReturn(metadataEntries);
        when(hpcMetadataService.toUserProvidedMetadataEntries(metadataEntries.getSelfMetadataEntries()))
                .thenReturn(metadataEntries.getSelfMetadataEntries().subList(0, 2));
        when(hpcMetadataNormalizationLocator.getNormalizationMapping()).thenReturn(normalizationMap);
        when(hpcTextEmbeddingProxy.getEmbeddingVector("Embed this metadata: assay: RNASeq; disease_type: Lung"))
                .thenReturn(embeddingVector);

        service.indexCollection(COLLECTION_PATH);

        verify(hpcTextEmbeddingProxy).getEmbeddingVector("Embed this metadata: assay: RNASeq; disease_type: Lung");
        verify(hpcVectorStoreProxy).storeVector(embeddingVector, COLLECTION_PATH);
        verify(hpcMetadataService).setMetadataVectorAdded(COLLECTION_PATH);
    }

    @Test
    public void testIndexCollectionNormalizationMiss() throws HpcException {
        ReflectionTestUtils.setField(service, "embeddingTemplate", TEMPLATE);
        List<Float> embeddingVector = Arrays.asList(0.1f, 0.2f, 0.3f);
        HpcMetadataEntries metadataEntries = new HpcMetadataEntries();
        metadataEntries.getSelfMetadataEntries().add(toMetadataEntry("sample_type", "Tumor"));

        when(hpcMetadataService.getCollectionMetadataEntries(COLLECTION_PATH)).thenReturn(metadataEntries);
        when(hpcMetadataService.toUserProvidedMetadataEntries(metadataEntries.getSelfMetadataEntries()))
                .thenReturn(metadataEntries.getSelfMetadataEntries());
        when(hpcMetadataNormalizationLocator.getNormalizationMapping()).thenReturn(new LinkedHashMap<>());
        when(hpcTextEmbeddingProxy.getEmbeddingVector("Embed this metadata: sample_type: Tumor"))
                .thenReturn(embeddingVector);

        service.indexCollection(COLLECTION_PATH);

        verify(hpcTextEmbeddingProxy).getEmbeddingVector("Embed this metadata: sample_type: Tumor");
        verify(hpcVectorStoreProxy).storeVector(embeddingVector, COLLECTION_PATH);
        verify(hpcMetadataService).setMetadataVectorAdded(COLLECTION_PATH);
    }

    @Test
    public void testIndexCollectionStoreVectorFailure_DoesNotSetMetadataVectorAdded() throws HpcException {
        ReflectionTestUtils.setField(service, "embeddingTemplate", TEMPLATE);
        List<Float> embeddingVector = Arrays.asList(0.1f, 0.2f, 0.3f);
        HpcMetadataEntries metadataEntries = new HpcMetadataEntries();
        metadataEntries.getSelfMetadataEntries().add(toMetadataEntry("sample_type", "Tumor"));

        when(hpcMetadataService.getCollectionMetadataEntries(COLLECTION_PATH)).thenReturn(metadataEntries);
        when(hpcMetadataService.toUserProvidedMetadataEntries(metadataEntries.getSelfMetadataEntries()))
                .thenReturn(metadataEntries.getSelfMetadataEntries());
        when(hpcMetadataNormalizationLocator.getNormalizationMapping()).thenReturn(new LinkedHashMap<>());
        when(hpcTextEmbeddingProxy.getEmbeddingVector("Embed this metadata: sample_type: Tumor"))
                .thenReturn(embeddingVector);
        doThrow(new HpcException("vector store failure", HpcErrorType.UNEXPECTED_ERROR))
                .when(hpcVectorStoreProxy).storeVector(embeddingVector, COLLECTION_PATH);

        try {
            service.indexCollection(COLLECTION_PATH);
            fail("Expected HpcException");
        } catch (HpcException e) {
            verify(hpcMetadataService, never()).setMetadataVectorAdded(COLLECTION_PATH);
        }
    }

    private HpcMetadataEntry toMetadataEntry(String attribute, String value) {
        HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
        metadataEntry.setAttribute(attribute);
        metadataEntry.setValue(value);
        return metadataEntry;
    }
}
