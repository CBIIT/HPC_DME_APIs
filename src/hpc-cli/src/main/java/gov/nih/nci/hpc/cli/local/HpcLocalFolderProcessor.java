package gov.nih.nci.hpc.cli.local;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.gson.Gson;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.easybatch.core.processor.RecordProcessingException;
import org.springframework.web.util.UriComponentsBuilder;

public class HpcLocalFolderProcessor extends HpcLocalEntityProcessor {

  private String filePath4LogFile;
  private String filePath4ErrorRecordsFile;


  public HpcLocalFolderProcessor(HpcServerConnection connection)
      throws IOException, FileNotFoundException {
    super(connection);
  }

  @Override
  public boolean process(HpcPathAttributes entity, String localPath, String filePathBaseName,
      String destinationBasePath,
      String logFile, String recordFile, boolean metadataOnly, boolean extractMetadata, boolean directUpload,
      boolean checksum, String metadataFile)
      throws RecordProcessingException {
    this.filePath4LogFile = logFile;
    this.filePath4ErrorRecordsFile = recordFile;
    String collectionPath = getCollectionPath(localPath, filePathBaseName, entity.getPath());
    if (!collectionPath.equals("/"))
      processCollection(entity, destinationBasePath, collectionPath);
    return true;
  }

  private void processCollection(HpcPathAttributes file, String basePath, String collectionPath)
      throws RecordProcessingException {
    performPathValidation(collectionPath, basePath, file);
    collectionPath = collectionPath.replace("//", "/");
    collectionPath = collectionPath.replace("\\", "/");
    List<HpcMetadataEntry> metadataList = getMetadata(file, false, null);

    if (metadataList == null || metadataList.isEmpty()) {
      System.out.println("No metadata to add. Skipping collection: " + file.getAbsolutePath());
      return;
    }

    HpcCollectionRegistrationDTO collectionDTO = new HpcCollectionRegistrationDTO();
    HpcBulkMetadataEntries bulkMetadataEntries = new HpcBulkMetadataEntries();
    collectionDTO.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);
    collectionDTO.getMetadataEntries().addAll(metadataList);
    List<HpcMetadataEntry> parentMetadataList = getParentCollectionMetadata(file, false);
    collectionDTO.setCreateParentCollections(true);
    collectionDTO.getParentCollectionsBulkMetadataEntries().getDefaultCollectionMetadataEntries().addAll(parentMetadataList);

    System.out.println("Registering Collection " + collectionPath);

    String apiUrl2Apply;
    try {
      apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(connection
          .getHpcServerURL()).path("/collection/{base-path}/{collection-path}")
          .buildAndExpand(basePath, collectionPath)
          .encode().toUri().toURL().toExternalForm();
    } catch (MalformedURLException mue) {
      final String pathUnderServerUrl = HpcClientUtil.constructPathString(
          "collection", basePath, collectionPath);
      final String informativeMsg = new StringBuilder("Error in attempt to")
          .append(" build URL for making REST service call.\nBase server URL [")
          .append(connection.getHpcServerURL()).append("].\nPath under base")
          .append(" serve URL [").append(pathUnderServerUrl).append("].\n")
          .toString();
      throw new RecordProcessingException(informativeMsg, mue);
    }
    WebClient client = HpcClientUtil.getWebClient(apiUrl2Apply,
        connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(),
        connection.getHpcCertPath(), connection.getHpcCertPassword());
    client.header("Authorization", "Bearer " + connection.getAuthToken());
    client.header("Connection", "Keep-Alive");
    client.type("application/json; charset=UTF-8");
    Response restResponse = client.invoke("PUT", collectionDTO);
    if (restResponse.getStatus() == 201 || restResponse.getStatus() == 200) {
      System.out.println("Success!");
      return;
    } else {
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
          new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
          new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser;
      try {
        parser = factory.createParser((InputStream) restResponse.getEntity());
        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new RecordProcessingException(exception.getMessage());
      } catch (IllegalStateException | IOException e) {
        throw new RecordProcessingException(e.getMessage());
      }
    }
  }


  private String getCollectionPath(String localPath, String collectionPathBaseName,
      String collectionPath) {
    String fullFilePathName = null;
    Path fullFile = Paths.get(localPath);
//    File fullFile = new File(Paths.generateFileSystemResourceUri(localPath));
    String fullLocalPathName = null;
    Path fullLocalFile = Paths.get(collectionPath);
    
    fullFilePathName = fullFile.normalize().toString();
    fullLocalPathName = fullLocalFile.toAbsolutePath().normalize().toString();

    collectionPath = collectionPath.replace("\\", "/");
    localPath = localPath.replace("\\", "/");
    fullFilePathName = fullFilePathName.replace('\\', '/');
    fullLocalPathName = fullLocalPathName.replace('\\', '/');
    if (collectionPath.equals(localPath)) {
      return "/";
    }

    if (collectionPathBaseName != null && collectionPathBaseName.isEmpty()) {
      String name = "/" + collectionPathBaseName;
      if (collectionPath.indexOf(name) != -1) {
        return collectionPath.substring(collectionPath.indexOf(name) + 1);
      }
    } else {
      if (fullLocalPathName.indexOf(fullFilePathName) != -1) {
        return fullLocalPathName
            .substring(collectionPath.indexOf(fullFilePathName) + fullFilePathName.length() + 1);
      }
    }
    return collectionPath;
  }


  private void performPathValidation(String collectionPath, String basePath,
      HpcPathAttributes attribs) throws RecordProcessingException {
    try {
      HpcClientUtil.validateDmeArchivePath(collectionPath);
    } catch (HpcCmdException e) {
      HpcLogWriter.getInstance().WriteLog(this.filePath4ErrorRecordsFile,
        new Gson().toJson(attribs));
      String errorMsg = "Failed to register Collection at DME archive" +
          " destination path '" + collectionPath + "'.\n     " + e.getMessage();
      HpcLogWriter.getInstance().WriteLog(this.filePath4LogFile, errorMsg);
      //System.out.println("...  ERROR!\n" + errorMsg);
      //System.out.println("--------------------------------------------------");

      throw new RecordProcessingException(errorMsg, e);
    }
  }

}