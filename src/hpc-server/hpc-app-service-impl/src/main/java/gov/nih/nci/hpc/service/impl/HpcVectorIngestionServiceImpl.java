/**
 * HpcVectorIngestionServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcTextEmbeddingProxy;
import gov.nih.nci.hpc.integration.HpcVectorStoreProxy;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcVectorIngestionService;

/**
 * <p>
 * HPC vector ingestion application service implementation.
 * </p>
 */
public class HpcVectorIngestionServiceImpl implements HpcVectorIngestionService {

    @Autowired
    private HpcTextEmbeddingProxy hpcTextEmbeddingProxy = null;

    @Autowired
    private HpcVectorStoreProxy hpcVectorStoreProxy = null;

    @Autowired
    private HpcMetadataDAO hpcMetadataDAO = null;

    @Autowired
    private HpcMetadataService hpcMetadataService = null;

    @Autowired
    private HpcMetadataNormalizationLocator hpcMetadataNormalizationLocator = null;

    @Value("${hpc.ai.embedding.template}")
    private String embeddingTemplate = null;

    private static final String METADATA_PLACEHOLDER = "{{metadata}}";

    @Override
    public void indexCollection(String collectionId) throws HpcException {
        if (collectionId == null || collectionId.isBlank()) {
            throw new HpcException("Collection ID cannot be blank", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        String path = hpcMetadataDAO.getCollectionPathByCollectionId(collectionId);
        if (StringUtils.isBlank(path)) {
            throw new HpcException("Collection path not found for collection ID: " + collectionId,
                    HpcErrorType.INVALID_REQUEST_INPUT);
        }

        HpcMetadataEntries metadataEntries = hpcMetadataService.getCollectionMetadataEntries(path);
        List<HpcMetadataEntry> userMetadataEntries = metadataEntries != null
                ? hpcMetadataService.toUserProvidedMetadataEntries(metadataEntries.getSelfMetadataEntries())
                : new ArrayList<>();
        String embeddingText = getEmbeddingText(userMetadataEntries);

        List<Float> vector = hpcTextEmbeddingProxy.getEmbeddingVector(embeddingText);
        hpcVectorStoreProxy.storeVector(vector, collectionId);
    }

    private String getEmbeddingText(List<HpcMetadataEntry> metadataEntries) {
        Map<String, String> normalizationMapping = hpcMetadataNormalizationLocator.getNormalizationMapping();
        Map<String, String> normalizedMetadata = new LinkedHashMap<>();
        if (metadataEntries != null) {
            metadataEntries.stream()
                    .filter(metadataEntry -> metadataEntry != null && !StringUtils.isBlank(metadataEntry.getAttribute())
                            && !StringUtils.isBlank(metadataEntry.getValue()))
                    .sorted(Comparator.comparing(HpcMetadataEntry::getAttribute).thenComparing(HpcMetadataEntry::getValue))
                    .forEach(metadataEntry -> {
                        String attribute = metadataEntry.getAttribute();
                        String canonicalAttribute = normalizationMapping.get(attribute);
                        if (StringUtils.isBlank(canonicalAttribute)) {
                            canonicalAttribute = normalizationMapping.get(attribute.toLowerCase());
                        }
                        String normalizedAttribute = !StringUtils.isBlank(canonicalAttribute) ? canonicalAttribute
                                : attribute;
                        normalizedMetadata.put(normalizedAttribute, metadataEntry.getValue());
                    });
        }

        StringBuilder metadataTextBuilder = new StringBuilder();
        normalizedMetadata.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (metadataTextBuilder.length() > 0) {
                        metadataTextBuilder.append("; ");
                    }
                    metadataTextBuilder.append(entry.getKey()).append(": ").append(entry.getValue());
                });

        String template = embeddingTemplate != null ? embeddingTemplate : METADATA_PLACEHOLDER;
        if (template.contains(METADATA_PLACEHOLDER)) {
            return template.replace(METADATA_PLACEHOLDER, metadataTextBuilder.toString());
        }
        return template + " " + metadataTextBuilder;
    }
}
