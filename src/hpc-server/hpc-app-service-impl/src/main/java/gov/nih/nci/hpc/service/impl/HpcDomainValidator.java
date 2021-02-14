/**
 * HpcDomainValidator.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.error.HpcDomainValidationResult;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryAttributeMatch;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;

/**
 * Helper class to validate domain objects.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDomainValidator {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	private static final int MAX_COMPOUND_METADATA_QUERY_DEPTH = 10;

	// ---------------------------------------------------------------------//
	// Class members
	// ---------------------------------------------------------------------//

	// The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcDomainValidator.class.getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Default constructor is disabled. */
	private HpcDomainValidator() {
	}

	// ---------------------------------------------------------------------//
	// Security Domain Object Types Validators
	// ---------------------------------------------------------------------//

	/**
	 * Validate User object.
	 *
	 * @param user The object to be validated.
	 * @return true if valid, false otherwise.
	 */
	public static boolean isValidUser(HpcUser user) {
		if (user == null || !isValidNciAccount(user.getNciAccount())) {
			logger.info("Invalid User: " + user);
			return false;
		}
		return true;
	}

	/**
	 * Validate NCI Account object.
	 *
	 * @param nciAccount the object to be validated.
	 * @return true if valid, false otherwise.
	 */
	public static boolean isValidNciAccount(HpcNciAccount nciAccount) {
		if (nciAccount == null || StringUtils.isBlank(nciAccount.getUserId())) {
			return false;
		}

		if (StringUtils.isBlank(nciAccount.getFirstName()) || 
				StringUtils.isBlank(nciAccount.getLastName()) || 
				StringUtils.isBlank(nciAccount.getDoc())) {
			return false;
		}

		return true;
	}

	/**
	 * Validate Integrated System Account object.
	 *
	 * @param integratedSystemAccount the object to be validated.
	 * @return true if valid, false otherwise.
	 */
	public static boolean isValidIntegratedSystemAccount(HpcIntegratedSystemAccount integratedSystemAccount) {
		if (integratedSystemAccount == null || integratedSystemAccount.getUsername() == null
				|| integratedSystemAccount.getPassword() == null
				|| integratedSystemAccount.getIntegratedSystem() == null) {
			logger.info("Invalid Integrated System Account: " + integratedSystemAccount);
			return false;
		}
		return true;
	}

	// ---------------------------------------------------------------------//
	// Data Management Domain Object Types Validators
	// ---------------------------------------------------------------------//

	/**
	 * Validate a file location object.
	 *
	 * @param location the object to be validated.
	 * @return true if valid, false otherwise.
	 */
	public static boolean isValidFileLocation(HpcFileLocation location) {
		if (location == null || StringUtils.isEmpty(location.getFileContainerId())
				|| StringUtils.isEmpty(location.getFileId())) {
			logger.info("Invalid File Location: {}", location);
			return false;
		}
		return true;
	}

	/**
	 * Validate a S3 account.
	 *
	 * @param s3Account The object to be validated.
	 * @return true if valid, false otherwise.
	 */
	public static HpcDomainValidationResult isValidS3Account(HpcS3Account s3Account) {
		HpcDomainValidationResult validationResult = new HpcDomainValidationResult();
		validationResult.setValid(false);
		
		if (s3Account == null || StringUtils.isEmpty(s3Account.getAccessKey())
				|| StringUtils.isEmpty(s3Account.getSecretKey())) {
			validationResult.setMessage("Empty S3 account access/secret key");
			return validationResult;
		}
		
		if(StringUtils.isEmpty(s3Account.getRegion()) && StringUtils.isEmpty(s3Account.getUrl())) {
			validationResult.setMessage("No region (AWS) or URL (3rd Party S3 Provider) provided");
			return validationResult;
		}
		
		if(!StringUtils.isEmpty(s3Account.getRegion()) && !StringUtils.isEmpty(s3Account.getUrl())) {
			validationResult.setMessage("Both region (AWS) or URL (3rd Party S3 Provider) provided");
			return validationResult;
		}
	
		validationResult.setValid(true);
		return validationResult;
	}

	// ---------------------------------------------------------------------//
	// Metadata Domain Object Types Validators
	// ---------------------------------------------------------------------//

	/**
	 * Validate metadata entry collection.
	 *
	 * @param metadataEntries Metadata entry collection.
	 * @param editMetadata true if the metadata is being edited. This is to enable delete.
	 * @return true if valid, false otherwise.
	 */
	public static HpcDomainValidationResult isValidMetadataEntries(List<HpcMetadataEntry> metadataEntries, boolean editMetadata) {
		HpcDomainValidationResult validationResult = new HpcDomainValidationResult();
		validationResult.setValid(true);

		if (metadataEntries == null) {
			validationResult.setValid(false);
			return validationResult;
		}

		for (HpcMetadataEntry metadataEntry : metadataEntries) {
			if(StringUtils.isEmpty(metadataEntry.getAttribute())) {
				validationResult.setValid(false);
				return validationResult;

			} else {
				if(editMetadata == false && StringUtils.isEmpty(metadataEntry.getValue())) {
					if(validationResult.getValid()) {
							validationResult.setMessage("The following entries cannot be empty: " + metadataEntry.getAttribute());
					} else {
							validationResult.setMessage(validationResult.getMessage() + ", " + metadataEntry.getAttribute());
					}
					validationResult.setValid(false);
				}
			}
		}
		return validationResult;
	}

	/**
	 * Validate metadata query level filter.
	 *
	 * @param levelFilter The level filter to validate.
	 * @return true if valid, false otherwise.
	 */
	public static HpcDomainValidationResult isValidMetadataQueryLevelFilter(HpcMetadataQueryLevelFilter levelFilter) {
		HpcDomainValidationResult validationResult = new HpcDomainValidationResult();
		validationResult.setValid(false);

		if (levelFilter == null) {
			validationResult.setMessage("Null level filter");
			return validationResult;
		}
		if (levelFilter.getOperator() == null) {
			validationResult.setMessage("Null level filter operator. Valid values are ["
					+ Arrays.asList(HpcMetadataQueryOperator.values()) + "]");
			return validationResult;
		}
		if (levelFilter.getLevel() == null && (levelFilter.getLabel() == null || levelFilter.getLabel().isEmpty())) {
			validationResult.setMessage("Both level and level-label are null");
			return validationResult;
		}
		if (levelFilter.getLevel() != null && levelFilter.getLabel() != null) {
			validationResult.setMessage("Both level and level-label are not null");
			return validationResult;
		}
		if (levelFilter.getLevel() != null && levelFilter.getLevel() <= 0) {
			validationResult.setMessage("Invalid level filter's level: " + levelFilter.getLevel());
			return validationResult;
		}

		validationResult.setValid(true);
		return validationResult;
	}

	/**
	 * Validate compound metadata query.
	 *
	 * @param compoundMetadataQuery Compound Metadata query.
	 * @return Domain validation result
	 */
	public static HpcDomainValidationResult isValidCompoundMetadataQuery(
			HpcCompoundMetadataQuery compoundMetadataQuery) {
		return isValidCompoundMetadataQuery(compoundMetadataQuery, 1);
	}

	/**
	 * Validate named compound metadata query.
	 *
	 * @param namedCompoundMetadataQuery Named compound metadata query.
	 * @return Domain validation result
	 */
	public static HpcDomainValidationResult isValidNamedCompoundMetadataQuery(
			HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery) {
		HpcDomainValidationResult validationResult = new HpcDomainValidationResult();
		validationResult.setValid(false);

		if (namedCompoundMetadataQuery == null) {
			validationResult.setMessage("Null named compound query");
			return validationResult;
		}
		if (namedCompoundMetadataQuery.getDetailedResponse() == null) {
			validationResult.setMessage("Null detailed response indicator");
			return validationResult;
		}
		if (namedCompoundMetadataQuery.getTotalCount() == null) {
			validationResult.setMessage("Null total count indicator");
			return validationResult;
		}
		if (namedCompoundMetadataQuery.getCompoundQueryType() == null) {
			validationResult.setMessage("Null compound query type. Valid values are ["
					+ Arrays.asList(HpcCompoundMetadataQueryType.values()) + "]");
			return validationResult;
		}

		if (namedCompoundMetadataQuery.getName() == null || namedCompoundMetadataQuery.getName().isEmpty()) {
			validationResult.setMessage("Null or empty query name");
			return validationResult;
		}

		return isValidCompoundMetadataQuery(namedCompoundMetadataQuery.getCompoundQuery());
	}

	// ---------------------------------------------------------------------//
	// Notification Domain Object Types Validators
	// ---------------------------------------------------------------------//

	/**
	 * Validate a notification subscription object.
	 *
	 * @param notificationSubscription the object to be validated.
	 * @return true if valid, false otherwise.
	 */
	public static boolean isValidNotificationSubscription(HpcNotificationSubscription notificationSubscription) {
		if (notificationSubscription == null || notificationSubscription.getEventType() == null
				|| isEmpty(notificationSubscription.getNotificationDeliveryMethods())
				|| notificationSubscription.getNotificationDeliveryMethods().contains(null)) {
			return false;
		}

		return true;
	}

	// ---------------------------------------------------------------------//
	// Data Browse Domain Object Types Validators
	// ---------------------------------------------------------------------//

	/**
	 * Validate a bookmark.
	 *
	 * @param bookmark The bookmark to validate.
	 * @return Domain validation result
	 */
	public static HpcDomainValidationResult isValidBookmark(HpcBookmark bookmark) {
		HpcDomainValidationResult validationResult = new HpcDomainValidationResult();
		validationResult.setValid(false);

		if (bookmark == null) {
			validationResult.setMessage("Null bookmark");
			return validationResult;
		}
		if (StringUtils.isEmpty(bookmark.getName())) {
			validationResult.setMessage("Null or empty bookmark name");
			return validationResult;
		}
		if (StringUtils.isEmpty(bookmark.getPath())) {
			validationResult.setMessage("Null or empty bookmark path");
			return validationResult;
		}

		validationResult.setValid(true);
		return validationResult;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Check if a collection is empty.
	 *
	 * @param collection The collection to check.
	 * @return true if not null and not empty, false otherwise.
	 */
	private static boolean isEmpty(Collection<?> collection) {
		return collection == null ? false : collection.isEmpty();
	}

	/**
	 * Validate compound metadata query.
	 *
	 * @param compoundMetadataQuery Compound Metadata query.
	 * @param queryDepth            The recursion depth of this method on the stack.
	 *                              (used to enforce a limit)
	 * @return Domain validation result.
	 */
	private static HpcDomainValidationResult isValidCompoundMetadataQuery(
			HpcCompoundMetadataQuery compoundMetadataQuery, int queryDepth) {
		HpcDomainValidationResult validationResult = new HpcDomainValidationResult();
		validationResult.setValid(false);

		if (queryDepth > MAX_COMPOUND_METADATA_QUERY_DEPTH) {
			validationResult
					.setMessage("Compound query depth over the allowed limit of " + MAX_COMPOUND_METADATA_QUERY_DEPTH);
			return validationResult;
		}

		if (compoundMetadataQuery == null) {
			validationResult.setMessage("Null compound query");
			return validationResult;
		}

		if (compoundMetadataQuery.getOperator() == null) {
			validationResult.setMessage("Null compound query operator in query [" + compoundMetadataQuery + "]. "
					+ "Valid values are [" + Arrays.asList(HpcCompoundMetadataQueryOperator.values()) + "]");
			return validationResult;
		}

		if (isEmpty(compoundMetadataQuery.getQueries()) && isEmpty(compoundMetadataQuery.getCompoundQueries())) {
			validationResult.setMessage("Compound query [" + compoundMetadataQuery + "]. "
					+ "contains no sub queries (simple or compound)");
			return validationResult;
		}

		// Validate the sub simple queries.
		if (compoundMetadataQuery.getQueries() != null) {
			for (HpcMetadataQuery metadataQuery : compoundMetadataQuery.getQueries()) {
				HpcDomainValidationResult subQueryValidationResult = isValidMetadataQuery(metadataQuery);
				if (!subQueryValidationResult.getValid()) {
					return subQueryValidationResult;
				}
			}
		}

		// Validate the sub compound queries.
		if (compoundMetadataQuery.getCompoundQueries() != null) {
			for (HpcCompoundMetadataQuery subQuery : compoundMetadataQuery.getCompoundQueries()) {
				HpcDomainValidationResult subCompoundQueryValidationResult = isValidCompoundMetadataQuery(subQuery,
						queryDepth + 1);
				if (!subCompoundQueryValidationResult.getValid()) {
					return subCompoundQueryValidationResult;
				}
			}
		}

		validationResult.setValid(true);
		return validationResult;
	}

	/**
	 * Validate a metadata (simple) query.
	 *
	 * @param metadataQuery The metadata query to validate.
	 * @return Domain validation result.
	 */
	private static HpcDomainValidationResult isValidMetadataQuery(HpcMetadataQuery metadataQuery) {
		HpcDomainValidationResult validationResult = new HpcDomainValidationResult();
		validationResult.setValid(false);

		HpcMetadataQueryAttributeMatch attribueMatch = metadataQuery.getAttributeMatch();
		if ((attribueMatch == null || attribueMatch.equals(HpcMetadataQueryAttributeMatch.EXACT))
				&& StringUtils.isEmpty(metadataQuery.getAttribute())) {
			validationResult.setMessage("Null metadata attribute in query [" + metadataQuery + "]");
			return validationResult;
		}
		if ((attribueMatch != null && attribueMatch.equals(HpcMetadataQueryAttributeMatch.ANY))
				&& !StringUtils.isEmpty(metadataQuery.getAttribute())) {
			validationResult.setMessage(
					"Metadata attribute in not empty w/ match any attribute in query [" + metadataQuery + "]");
			return validationResult;
		}
		if (StringUtils.isEmpty(metadataQuery.getValue())) {
			validationResult.setMessage("Null metadata value in query [" + metadataQuery + "]");
			return validationResult;
		}
		if (metadataQuery.getOperator() == null) {
			validationResult.setMessage("Null operator in query [" + metadataQuery + "]. " + "Valid values are ["
					+ Arrays.asList(HpcMetadataQueryOperator.values()) + "]");
			return validationResult;
		}
		if (metadataQuery.getLevelFilter() != null) {
			HpcDomainValidationResult levelFilterValidationResult = isValidMetadataQueryLevelFilter(
					metadataQuery.getLevelFilter());
			if (!levelFilterValidationResult.getValid()) {
				return levelFilterValidationResult;
			}
		}
		if (metadataQuery.getOperator().equals(HpcMetadataQueryOperator.TIMESTAMP_GREATER_THAN)
				|| metadataQuery.getOperator().equals(HpcMetadataQueryOperator.TIMESTAMP_LESS_THAN)
				|| metadataQuery.getOperator().equals(HpcMetadataQueryOperator.TIMESTAMP_GREATER_OR_EQUAL)
				|| metadataQuery.getOperator().equals(HpcMetadataQueryOperator.TIMESTAMP_LESS_OR_EQUAL)) {
			if (StringUtils.isEmpty(metadataQuery.getFormat())) {
				validationResult.setMessage("Null format in query with timestamp operator [" + metadataQuery + "]");
				return validationResult;
			}
		} else if (!StringUtils.isEmpty(metadataQuery.getFormat())) {
			validationResult.setMessage("Format provided in query with no timestamp operator [" + metadataQuery + "]");
			return validationResult;
		}

		validationResult.setValid(true);
		return validationResult;
	}
}
