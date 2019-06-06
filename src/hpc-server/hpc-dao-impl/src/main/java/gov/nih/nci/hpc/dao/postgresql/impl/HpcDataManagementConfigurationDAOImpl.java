/**
 * HpcDataManagementConfigurationDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.postgresql.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;
import gov.nih.nci.hpc.dao.HpcDataManagementConfigurationDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcPermTempArchiveType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.model.HpcArchiveDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Management Configuration DAO Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementConfigurationDAOImpl implements HpcDataManagementConfigurationDAO {
  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // SQL Queries.
  private static final String GET_DATA_MANAGEMENT_CONFIGURATIONS_SQL =
      "select * from public.\"HPC_DATA_MANAGEMENT_CONFIGURATION\"";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Spring JDBC Template instance.
  @Autowired
  private JdbcTemplate jdbcTemplate = null;

  // HpcDataManagementConfiguration Table to Object mapper.
  private RowMapper<HpcDataManagementConfiguration> rowMapper = (rs, rowNum) -> {
    HpcDataManagementConfiguration dataManagementConfiguration =
        new HpcDataManagementConfiguration();
    dataManagementConfiguration.setId(rs.getString("ID"));
    dataManagementConfiguration.setBasePath(rs.getString("BASE_PATH"));
    dataManagementConfiguration.setDoc(rs.getString("DOC"));
    String archiveType = rs.getString("ARCHIVE_TYPE");
    if (!StringUtils.isEmpty(archiveType)) {
      dataManagementConfiguration.setArchiveType(HpcArchiveType.fromValue(archiveType));
    }

    // 2-hop REMOVE ------------------------------------------------------------
    // Map the S3 Configuration.
    HpcDataTransferConfiguration s3Configuration = new HpcDataTransferConfiguration();
    s3Configuration.setUrl(rs.getString("S3_URL"));
    HpcArchive s3BaseArchiveDestination = new HpcArchive();
    HpcFileLocation s3ArchiveLocation = new HpcFileLocation();
    s3ArchiveLocation.setFileContainerId(rs.getString("S3_VAULT"));
    s3ArchiveLocation.setFileId(rs.getString("S3_OBJECT_ID"));
    s3BaseArchiveDestination.setFileLocation(s3ArchiveLocation);
    String s3ArchiveType = rs.getString("S3_ARCHIVE_TYPE");
    if (!StringUtils.isEmpty(s3ArchiveType)) {
      s3BaseArchiveDestination
          .setPermTempArchiveType(HpcPermTempArchiveType.fromValue(s3ArchiveType));
    }
    s3Configuration.setBaseArchiveDestination(s3BaseArchiveDestination);
    s3Configuration.setUploadRequestURLExpiration(rs.getInt("S3_UPLOAD_REQUEST_URL_EXPIRATION"));

    dataManagementConfiguration.setS3Configuration(s3Configuration);

    // Map the Globus configuration.
    HpcDataTransferConfiguration globusConfiguration = new HpcDataTransferConfiguration();
    globusConfiguration.setUrl(rs.getString("GLOBUS_URL"));

    HpcArchive globusBaseArchiveDestination = new HpcArchive();
    HpcFileLocation globusArchiveLocation = new HpcFileLocation();
    globusArchiveLocation.setFileContainerId(rs.getString("GLOBUS_ARCHIVE_ENDPOINT"));
    globusArchiveLocation.setFileId(rs.getString("GLOBUS_ARCHIVE_PATH"));
    globusBaseArchiveDestination.setFileLocation(globusArchiveLocation);
    globusBaseArchiveDestination.setPermTempArchiveType(
        HpcPermTempArchiveType.fromValue(rs.getString("GLOBUS_ARCHIVE_TYPE")));
    globusBaseArchiveDestination.setDirectory(rs.getString("GLOBUS_ARCHIVE_DIRECTORY"));
    globusConfiguration.setBaseArchiveDestination(globusBaseArchiveDestination);

    HpcArchive globusBaseDownloadSource = new HpcArchive();
    HpcFileLocation globusDownloadLocation = new HpcFileLocation();
    globusDownloadLocation.setFileContainerId(rs.getString("GLOBUS_DOWNLOAD_ENDPOINT"));
    globusDownloadLocation.setFileId(rs.getString("GLOBUS_DOWNLOAD_PATH"));
    globusBaseDownloadSource.setFileLocation(globusDownloadLocation);
    globusBaseDownloadSource.setDirectory(rs.getString("GLOBUS_DOWNLOAD_DIRECTORY"));
    globusConfiguration.setBaseDownloadSource(globusBaseDownloadSource);

    dataManagementConfiguration.setGlobusConfiguration(globusConfiguration);
    // -----------------------------------------------------------------------------------------

    // Map the Archive S3 Configuration.
    HpcArchiveDataTransferConfiguration archiveS3Configuration = new HpcArchiveDataTransferConfiguration();
    archiveS3Configuration.setUrl(rs.getString("ARCHIVE_S3_URL"));
    HpcFileLocation archiveS3FileLocation = new HpcFileLocation();
    archiveS3FileLocation.setFileContainerId(rs.getString("ARCHIVE_S3_VAULT"));
    archiveS3FileLocation.setFileId(rs.getString("ARCHIVE_S3_OBJECT_ID"));
    archiveS3Configuration.setArchiveFileLocation(archiveS3FileLocation);
    archiveS3Configuration.setUploadRequestURLExpiration(rs.getInt("ARCHIVE_S3_UPLOAD_REQUEST_URL_EXPIRATION"));
    dataManagementConfiguration.setArchiveS3Configuration(archiveS3Configuration);
    

    // Map the Archive Globus configuration.
    HpcArchiveDataTransferConfiguration archiveGlobusConfiguration = new HpcArchiveDataTransferConfiguration();
    archiveGlobusConfiguration.setUrl(rs.getString("ARCHIVE_GLOBUS_URL"));
    HpcFileLocation archiveGlobusFileLocation = new HpcFileLocation();
    archiveGlobusFileLocation.setFileContainerId(rs.getString("ARCHIVE_GLOBUS_ENDPOINT"));
    archiveGlobusFileLocation.setFileId(rs.getString("ARCHIVE_GLOBUS_PATH"));
    archiveGlobusConfiguration.setArchiveFileLocation(archiveGlobusFileLocation);
    archiveGlobusConfiguration.setArchiveDirectory(rs.getString("ARCHIVE_GLOBUS_DIRECTORY"));
    dataManagementConfiguration.setArchiveGlobusConfiguration(archiveGlobusConfiguration);
    
    try {
      dataManagementConfiguration
          .setDataHierarchy(getDataHierarchyFromJSONStr(rs.getString("DATA_HIERARCHY")));
      dataManagementConfiguration.getCollectionMetadataValidationRules()
          .addAll(getMetadataValidationRulesFromJSONStr(
              rs.getString("COLLECTION_METADATA_VALIDATION_RULES")));
      dataManagementConfiguration.getDataObjectMetadataValidationRules()
          .addAll(getMetadataValidationRulesFromJSONStr(
              rs.getString("DATA_OBJECT_METADATA_VALIDATION_RULES")));

    } catch (HpcException e) {
      throw new SQLException(e);
    }

    return dataManagementConfiguration;
  };

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcDataManagementConfigurationDAOImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcDataManagementConfigDAO Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public List<HpcDataManagementConfiguration> getDataManagementConfigurations()
      throws HpcException {
    try {
      return jdbcTemplate.query(GET_DATA_MANAGEMENT_CONFIGURATIONS_SQL, rowMapper);

    } catch (DataAccessException e) {
      throw new HpcException("Failed to get data management configurations: " + e.getMessage(),
          HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
    }
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Verify connection to DB. Called by Spring as init-method.
   *
   * @throws HpcException If it failed to connect to the database.
   */
  @SuppressWarnings("unused")
  private void dbConnect() throws HpcException {
    try {
      jdbcTemplate.getDataSource().getConnection();

    } catch (Exception e) {
      throw new HpcException("Failed to connect to PostgreSQL DB. Check credentials config",
          HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
    }
  }

  /**
   * Instantiate a data hierarchy from a string.
   *
   * @param dataHierarchyJSONStr The data hierarchy JSON string.
   * @return HpcDataHierarchy
   * @throws HpcException If failed to parse JSON.
   */
  private HpcDataHierarchy getDataHierarchyFromJSONStr(String dataHierarchyJSONStr)
      throws HpcException {
    if (dataHierarchyJSONStr == null) {
      return null;
    }

    try {
      return dataHierarchyFromJSON((JSONObject) new JSONParser().parse(dataHierarchyJSONStr));

    } catch (Exception e) {
      throw new HpcException("Failed to parse data hierarchy JSON: " + dataHierarchyJSONStr,
          HpcErrorType.DATABASE_ERROR, e);
    }
  }

  /**
   * Instantiate a HpcDataHierarchy from JSON object.
   *
   * @param jsonDataHierarchy The data hierarchy JSON object.
   * @return HpcDataHierarchy
   * @throws HpcException If failed to parse the JSON.
   */
  @SuppressWarnings("unchecked")
  private HpcDataHierarchy dataHierarchyFromJSON(JSONObject jsonDataHierarchy) throws HpcException {
    HpcDataHierarchy dataHierarchy = new HpcDataHierarchy();

    if (!jsonDataHierarchy.containsKey("collectionType")
        || !jsonDataHierarchy.containsKey("isDataObjectContainer")) {
      throw new HpcException("Invalid Data Hierarchy Definition: " + jsonDataHierarchy,
          HpcErrorType.DATABASE_ERROR);
    }

    dataHierarchy.setCollectionType((String) jsonDataHierarchy.get("collectionType"));
    dataHierarchy
        .setIsDataObjectContainer((Boolean) jsonDataHierarchy.get("isDataObjectContainer"));

    // Iterate through the sub collections.
    JSONArray jsonSubCollections = (JSONArray) jsonDataHierarchy.get("subCollections");
    if (jsonSubCollections != null) {
      Iterator<JSONObject> subCollectionsIterator = jsonSubCollections.iterator();
      while (subCollectionsIterator.hasNext()) {
        dataHierarchy.getSubCollectionsHierarchies()
            .add(dataHierarchyFromJSON(subCollectionsIterator.next()));
      }
    }

    return dataHierarchy;
  }

  /**
   * Instantiate metadata validation rules from string.
   *
   * @param metadataValidationRulesJSONStr The metadata validation rules JSON String.
   * @return a list of metadata validation rules.
   * @throws HpcException on service failure.
   */
  private List<HpcMetadataValidationRule> getMetadataValidationRulesFromJSONStr(
      String metadataValidationRulesJSONStr) throws HpcException {
    if (metadataValidationRulesJSONStr == null) {
      return new ArrayList<HpcMetadataValidationRule>();
    }

    try {
      JSONArray jsonMetadataValidationRules =
          (JSONArray) ((JSONObject) new JSONParser().parse(metadataValidationRulesJSONStr))
              .get("metadataValidationRules");
      if (jsonMetadataValidationRules == null) {
        throw new HpcException("Empty validation rules", HpcErrorType.DATABASE_ERROR);
      }

      return metadataValidationRulesFromJSON(jsonMetadataValidationRules);

    } catch (Exception e) {
      throw new HpcException(
          "Failed to parse metadata validation rules JSON: " + metadataValidationRulesJSONStr,
          HpcErrorType.DATABASE_ERROR, e);
    }
  }

  /**
   * Instantiate list metadata validation rules from JSON.
   *
   * @param jsonMetadataValidationRules The validation rules JSON array.
   * @return A collection of metadata validation rules.
   * @throws HpcException If failed to parse the JSON.
   */
  @SuppressWarnings("unchecked")
  private List<HpcMetadataValidationRule> metadataValidationRulesFromJSON(
      JSONArray jsonMetadataValidationRules) throws HpcException {
    List<HpcMetadataValidationRule> validationRules = new ArrayList<>();

    // Iterate through the rules and map to POJO.
    Iterator<JSONObject> rulesIterator = jsonMetadataValidationRules.iterator();
    while (rulesIterator.hasNext()) {
      JSONObject jsonMetadataValidationRule = rulesIterator.next();

      if (!jsonMetadataValidationRule.containsKey("attribute")
          || !jsonMetadataValidationRule.containsKey("mandatory")
          || !jsonMetadataValidationRule.containsKey("ruleEnabled")) {
        throw new HpcException("Invalid rule JSON object: " + jsonMetadataValidationRule,
            HpcErrorType.DATABASE_ERROR);
      }

      // JSON -> POJO.
      HpcMetadataValidationRule metadataValidationRule = new HpcMetadataValidationRule();
      metadataValidationRule.setAttribute((String) jsonMetadataValidationRule.get("attribute"));
      metadataValidationRule.setMandatory((Boolean) jsonMetadataValidationRule.get("mandatory"));
      metadataValidationRule
          .setRuleEnabled((Boolean) jsonMetadataValidationRule.get("ruleEnabled"));
      metadataValidationRule
          .setDefaultValue((String) jsonMetadataValidationRule.get("defaultValue"));
      metadataValidationRule.setDefaultUnit((String) jsonMetadataValidationRule.get("defaultUnit"));

      JSONArray jsonCollectionTypes = (JSONArray) jsonMetadataValidationRule.get("collectionTypes");
      if (jsonCollectionTypes != null) {
        Iterator<String> collectionTypeIterator = jsonCollectionTypes.iterator();
        while (collectionTypeIterator.hasNext()) {
          metadataValidationRule.getCollectionTypes().add(collectionTypeIterator.next());
        }
      }

      // Extract the valid values.
      JSONArray jsonValidValues = (JSONArray) jsonMetadataValidationRule.get("validValues");
      if (jsonValidValues != null) {
        Iterator<String> validValuesIterator = jsonValidValues.iterator();
        while (validValuesIterator.hasNext()) {
          metadataValidationRule.getValidValues().add(validValuesIterator.next());
        }
      }

      validationRules.add(metadataValidationRule);
    }

    return validationRules;
  }
}
