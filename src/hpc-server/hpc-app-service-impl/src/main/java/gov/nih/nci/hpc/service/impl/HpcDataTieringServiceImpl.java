/**
 * HpcDataTieringServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidTierItems;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.dao.HpcDataTieringDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcTieringRequestType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcBulkTierItem;
import gov.nih.nci.hpc.domain.model.HpcBulkTierRequest;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTieringService;

/**
 * HPC Data Tiering Application Service Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcDataTieringServiceImpl implements HpcDataTieringService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Map data transfer type to its proxy impl.
	private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = new EnumMap<>(
			HpcDataTransferType.class);

	// Data Tiering DAO.
	@Autowired
	private HpcDataTieringDAO dataTieringDAO = null;

	// S3 data transfer proxy.
	@Autowired
	@Qualifier("hpcS3DataTransferProxy")
	private HpcDataTransferProxy s3DataTransferProxy = null;

	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;

	// Data management configuration locator.
	@Autowired
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;

	// The max number of days to keep deep archive in progress
	@Value("${hpc.service.dataTransfer.maxDeepArchiveInProgressDays}")
	private Integer maxDeepArchiveInProgressDays = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 *
	 * @param dataTransferProxies The data transfer proxies.
	 * @throws HpcException on spring configuration error.
	 */
	public HpcDataTieringServiceImpl(Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies)
			throws HpcException {
		if (dataTransferProxies == null || dataTransferProxies.isEmpty()) {
			throw new HpcException("Null or empty map of data transfer proxies",
					HpcErrorType.SPRING_CONFIGURATION_ERROR);
		}

		this.dataTransferProxies.putAll(dataTransferProxies);

	}

	/**
	 * Default Constructor.
	 *
	 * @throws HpcException Constructor is disabled.
	 */
	@SuppressWarnings("unused")
	private HpcDataTieringServiceImpl() throws HpcException {
		throw new HpcException("Default Constructor disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTieringService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void tierDataObject(String userId, String path, HpcFileLocation hpcFileLocation,
			HpcDataTransferType dataTransferType, String configurationId) throws HpcException {
		// Input Validation.
		if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(path) || StringUtils.isEmpty(configurationId)
				|| !isValidFileLocation(hpcFileLocation)) {
			throw new HpcException("Invalid tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the S3 archive configuration ID.
		String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(configurationId)
				.getS3UploadConfigurationId();

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		String prefix = hpcFileLocation.getFileId();
		// Create life cycle policy with this data object
		dataTransferProxies.get(dataTransferType).setTieringPolicy(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), hpcFileLocation,
				prefix, dataTransferConfiguration.getTieringBucket(), dataTransferConfiguration.getTieringProtocol());
		// Add a record of the lifecycle rule
		dataTieringDAO.insert(userId, HpcTieringRequestType.TIER_DATA_OBJECT, s3ArchiveConfigurationId,
				Calendar.getInstance(), prefix);
	}

	@Override
	public void tierCollection(String userId, String path, HpcDataTransferType dataTransferType, String configurationId)
			throws HpcException {

		// Input Validation.
		if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(path) || StringUtils.isEmpty(configurationId)) {
			throw new HpcException("Invalid tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the S3 archive configuration ID.
		String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(configurationId)
				.getS3UploadConfigurationId();

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		HpcFileLocation hpcFileLocation = dataTransferConfiguration.getBaseArchiveDestination().getFileLocation();

		String prefix = hpcFileLocation.getFileId() + path + "/";
		// Create life cycle policy with this collection
		dataTransferProxies.get(dataTransferType).setTieringPolicy(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), hpcFileLocation,
				prefix, dataTransferConfiguration.getTieringBucket(), dataTransferConfiguration.getTieringProtocol());
		// Add a record of the lifecycle rule
		dataTieringDAO.insert(userId, HpcTieringRequestType.TIER_COLLECTION, s3ArchiveConfigurationId,
				Calendar.getInstance(), prefix);
	}

	@Override
	public void tierDataObjects(String userId, HpcBulkTierRequest bulkTierRequest, HpcDataTransferType dataTransferType)
			throws HpcException {
		// Input Validation.
		if (StringUtils.isEmpty(userId) || !isValidTierItems(bulkTierRequest)) {
			throw new HpcException("Invalid tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		// Create life cycle policy with these data objects
		for (HpcBulkTierItem item : bulkTierRequest.getItems()) {
			// Get the S3 archive configuration ID.
			String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(item.getConfigurationId())
					.getS3UploadConfigurationId();

			// Get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(item.getConfigurationId(), s3ArchiveConfigurationId,
							dataTransferType);

			HpcFileLocation hpcFileLocation = dataTransferConfiguration.getBaseArchiveDestination().getFileLocation();

			String prefix = item.getPath();
			// Create life cycle policy with this collection
			dataTransferProxies.get(dataTransferType).setTieringPolicy(
					getAuthenticatedToken(dataTransferType, item.getConfigurationId(), s3ArchiveConfigurationId),
					hpcFileLocation, prefix, dataTransferConfiguration.getTieringBucket(),
					dataTransferConfiguration.getTieringProtocol());

			// Add a record of the lifecycle rule
			dataTieringDAO.insert(userId, HpcTieringRequestType.TIER_DATA_OBJECT, s3ArchiveConfigurationId,
					Calendar.getInstance(), prefix);
		}
	}

	@Override
	public void tierCollections(String userId, HpcBulkTierRequest bulkTierRequest, HpcDataTransferType dataTransferType)
			throws HpcException {
		// Input Validation.
		if (StringUtils.isEmpty(userId) || !isValidTierItems(bulkTierRequest)) {
			throw new HpcException("Invalid archive request", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		// Create life cycle policy with these data objects
		for (HpcBulkTierItem item : bulkTierRequest.getItems()) {
			// Get the S3 archive configuration ID.
			String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(item.getConfigurationId())
					.getS3UploadConfigurationId();

			// Get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(item.getConfigurationId(), s3ArchiveConfigurationId,
							dataTransferType);

			HpcFileLocation hpcFileLocation = dataTransferConfiguration.getBaseArchiveDestination().getFileLocation();

			String prefix = hpcFileLocation.getFileId() + item.getPath() + "/";
			// Create life cycle policy with this collection
			dataTransferProxies.get(dataTransferType).setTieringPolicy(
					getAuthenticatedToken(dataTransferType, item.getConfigurationId(), s3ArchiveConfigurationId),
					hpcFileLocation, prefix, dataTransferConfiguration.getTieringBucket(),
					dataTransferConfiguration.getTieringProtocol());
			// Add a record of the lifecycle rule
			dataTieringDAO.insert(userId, HpcTieringRequestType.TIER_COLLECTION, s3ArchiveConfigurationId,
					Calendar.getInstance(), prefix);
		}
	}

	@Override
	public boolean isTieringSupported(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType) throws HpcException {
		// Get the data transfer configuration (Globus or S3).
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		return HpcIntegratedSystem.CLOUDIAN.equals(dataTransferConfiguration.getArchiveProvider())
				|| HpcIntegratedSystem.AWS.equals(dataTransferConfiguration.getArchiveProvider());
	}

	@Override
	public boolean deepArchiveDelayed(Calendar deepArchiveDate) {
		if (deepArchiveDate == null) {
			return true;
		}

		// Check if there is a delay in toggling the status
		Date expiration = new Date();
		expiration.setTime(deepArchiveDate.getTimeInMillis() + 1000 * 60 * 60 * maxDeepArchiveInProgressDays * 24);
		// If delayed, return true
		return expiration.before(new Date());
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Get the data transfer authenticated token from the request context. If it's
	 * not in the context, get a token by authenticating.
	 *
	 * @param dataTransferType         The data transfer type.
	 * @param configurationId          The data management configuration ID.
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX
	 * @return A data transfer authenticated token.
	 * @throws HpcException If it failed to obtain an authentication token.
	 */
	private Object getAuthenticatedToken(HpcDataTransferType dataTransferType, String configurationId,
			String s3ArchiveConfigurationId) throws HpcException {
		HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Unknown user", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Get the data transfer configuration (Globus or S3).
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		// Search for an existing token.
		for (HpcDataTransferAuthenticatedToken authenticatedToken : invoker.getDataTransferAuthenticatedTokens()) {
			if (authenticatedToken.getDataTransferType().equals(dataTransferType)
					&& authenticatedToken.getConfigurationId().equals(configurationId)
					&& (authenticatedToken.getS3ArchiveConfigurationId() == null || authenticatedToken
							.getS3ArchiveConfigurationId().equals(dataTransferConfiguration.getId()))) {
				return authenticatedToken.getDataTransferAuthenticatedToken();
			}
		}

		// No authenticated token found for this request. Create one.
		HpcIntegratedSystemAccount dataTransferSystemAccount = null;
		if (dataTransferType.equals(HpcDataTransferType.GLOBUS)) {
			dataTransferSystemAccount = systemAccountLocator.getSystemAccount(dataTransferType, configurationId);
		} else if (dataTransferType.equals(HpcDataTransferType.S_3)) {
			dataTransferSystemAccount = systemAccountLocator
					.getSystemAccount(dataTransferConfiguration.getArchiveProvider());
		}

		if (dataTransferSystemAccount == null) {
			throw new HpcException("System account not registered for " + dataTransferType.value(),
					HpcErrorType.UNEXPECTED_ERROR);
		}

		// Authenticate with the data transfer system.
		Object token = dataTransferProxies.get(dataTransferType).authenticate(dataTransferSystemAccount,
				dataTransferConfiguration.getUrlOrRegion());
		if (token == null) {
			throw new HpcException("Invalid data transfer account credentials", HpcErrorType.DATA_TRANSFER_ERROR,
					dataTransferSystemAccount.getIntegratedSystem());
		}

		// Store token on the request context.
		HpcDataTransferAuthenticatedToken authenticatedToken = new HpcDataTransferAuthenticatedToken();
		authenticatedToken.setDataTransferAuthenticatedToken(token);
		authenticatedToken.setDataTransferType(dataTransferType);
		authenticatedToken.setConfigurationId(configurationId);
		authenticatedToken.setS3ArchiveConfigurationId(dataTransferConfiguration.getId());
		authenticatedToken.setSystemAccountId(dataTransferSystemAccount.getUsername());
		invoker.getDataTransferAuthenticatedTokens().add(authenticatedToken);
		HpcRequestContext.setRequestInvoker(invoker);

		return token;
	}

}
