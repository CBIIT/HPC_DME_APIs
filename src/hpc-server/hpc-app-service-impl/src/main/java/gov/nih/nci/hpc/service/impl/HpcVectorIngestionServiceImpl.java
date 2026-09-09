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
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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
    private HpcMetadataService hpcMetadataService = null;

    @Autowired
    private HpcMetadataNormalizationLocator hpcMetadataNormalizationLocator = null;

    @Value("${hpc.ai.embedding.template}")
    private String embeddingTemplate = null;

    private static final String METADATA_PLACEHOLDER = "{{metadata}}";

    //The logger instance.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public void indexCollection(String collectionPath) throws HpcException {
        if (collectionPath == null || collectionPath.isBlank()) {
            throw new HpcException("Collection path cannot be blank", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        HpcMetadataEntries metadataEntries = hpcMetadataService.getCollectionMetadataEntries(collectionPath);
        List<HpcMetadataEntry> userMetadataEntries = metadataEntries != null
                ? hpcMetadataService.toUserProvidedMetadataEntries(metadataEntries.getSelfMetadataEntries())
                : new ArrayList<>();
        String embeddingText = buildEmbeddingText(userMetadataEntries);

        List<Float> vector = hpcTextEmbeddingProxy.getEmbeddingVector(embeddingText);
        hpcVectorStoreProxy.storeVector(vector, collectionPath);
        hpcMetadataService.setMetadataVectorAdded(collectionPath);
    }


    private String buildEmbeddingText(List<HpcMetadataEntry> metadataEntries) {

        Map<String, String> normalizationMapping = hpcMetadataNormalizationLocator.getNormalizationMapping();

        /*
         * Normalize metadata attribute names.
         *
         * LinkedHashMap preserves insertion order. The order is not used for an explicitly configured
         * template, but provides a stable iteration order before the {{metadata}} fallback is sorted.
         */
        Map<String, String> normalizedMetadata = new LinkedHashMap<>();
        if (metadataEntries != null) {
            for (HpcMetadataEntry metadataEntry : metadataEntries) {
               if (metadataEntry == null
                    || StringUtils.isBlank(metadataEntry.getAttribute())
                    || StringUtils.isBlank(metadataEntry.getValue())) {
                    continue;
                }
                String attribute = metadataEntry.getAttribute().trim();
                String value = metadataEntry.getValue().trim();

                /*
                 * The normalization mapping is expected to use lowercase source attribute names.
                 * Example: * "platform" -> "sequencer"
                 * "disease" -> "disease_type"
                 */
                String canonicalAttribute = normalizationMapping.get( attribute.toLowerCase(Locale.ROOT));
                String normalizedAttribute = StringUtils.isBlank(canonicalAttribute) ? attribute : canonicalAttribute.trim();
                normalizedMetadata.put(normalizedAttribute, value);
            }
        }

        //Use the configured template if one is supplied, otherwise use {{metadata}} as the fallback.
        String template = StringUtils.isBlank(embeddingTemplate) ? METADATA_PLACEHOLDER : embeddingTemplate;

        /*
         * {{metadata}} means that all normalized metadata should be * included.
         * Sort by canonical metadata key so that the same metadata always
         * produces the same text, regardless of the order in which the metadata entries were supplied.
         */
        if (template.contains(METADATA_PLACEHOLDER)) {
            String metadataText = normalizedMetadata.entrySet().stream()
                  .sorted(Map.Entry.comparingByKey())
                  .map(entry -> entry.getKey() + ": " + entry.getValue())
                  .collect(Collectors.joining("; "));

            return template.replace(METADATA_PLACEHOLDER, metadataText);
        }

        /*
         * Explicit template:
         * Example:
         * Dataset generated from sequencing with analyte type {{analyte_type}} and library strategy {{library_strategy}}
         * using platform {{sequencer}} for disease {{disease_type}}
         *
         * The template determines:
         * 1. Which metadata fields are included
         * 2. The order in which they appear
         */
        Pattern pattern = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");
        Matcher matcher = pattern.matcher(template);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String templateKey = matcher.group(1).trim();

            //Look up the metadata using the template key.
            String value = normalizedMetadata.get(templateKey);

            //Also allow case-insensitive template keys.
            if (value == null) {
                value = normalizedMetadata.get( templateKey.toLowerCase(Locale.ROOT));
            }

            //A metadata field explicitly referenced by the template must be present. Do not generate an incomplete embedding.
            if (value == null) {
                throw new IllegalArgumentException( "Required metadata '" + templateKey + "' is missing; cannot generate embedding");
            }

            matcher.appendReplacement( result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        logger.info("Embedding text generated from template {}: {}", template, result.toString());
        return result.toString();

    }

}