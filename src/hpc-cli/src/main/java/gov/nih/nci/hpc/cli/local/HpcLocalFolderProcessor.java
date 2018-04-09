package gov.nih.nci.hpc.cli.local;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.cli.util.Paths;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.easybatch.core.processor.RecordProcessingException;

public class HpcLocalFolderProcessor extends HpcLocalEntityProcessor {

	public HpcLocalFolderProcessor(HpcServerConnection connection) throws IOException, FileNotFoundException {
		super(connection);
	}

	@Override
	public boolean process(HpcPathAttributes entity, String localPath, String filePathBaseName, String destinationBasePath,
			String logFile, String recordFile, boolean metadataOnly, boolean directUpload, boolean checksum)
			throws RecordProcessingException {
		String collectionPath = getCollectionPath(localPath, filePathBaseName, entity.getPath());
		if(!collectionPath.equals("/"))
		  processCollection(entity, destinationBasePath, collectionPath);
		return true;
	}

	private void processCollection(HpcPathAttributes file, String basePath, String collectionPath)
			throws RecordProcessingException {
		collectionPath = collectionPath.replace("//", "/");
		collectionPath = collectionPath.replace("\\", "/");
		List<HpcMetadataEntry> metadataList = getMetadata(file, false);

		if (metadataList == null || metadataList.isEmpty()) {
			System.out.println("No metadata to add. Skipping collection: " + file.getAbsolutePath());
			return;
		}

		HpcCollectionRegistrationDTO collectionDTO = new HpcCollectionRegistrationDTO();
		collectionDTO.getMetadataEntries().addAll(metadataList);
		List<HpcMetadataEntry> parentMetadataList = getParentCollectionMetadata(file, false);
		collectionDTO.setCreateParentCollections(true);
		collectionDTO.getParentCollectionMetadataEntries().addAll(parentMetadataList);

		System.out.println("Registering Collection " + collectionPath);

		if(!basePath.startsWith("/"))
			basePath = "/"+basePath;
		
		WebClient client = HpcClientUtil.getWebClient(
				connection.getHpcServerURL() + "/collection" + basePath + "/" + collectionPath,
				connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(), connection.getHpcCertPath(),
				connection.getHpcCertPassword());
		client.header("Authorization", "Bearer " + connection.getAuthToken());
		client.header("Connection", "Keep-Alive");

		Response restResponse = client.invoke("PUT", collectionDTO);
		if (restResponse.getStatus() == 201 || restResponse.getStatus() == 200) {
			System.out.println("Success!");
			return;
		} else {
			ObjectMapper mapper = new ObjectMapper();
			AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
					new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()), new JacksonAnnotationIntrospector());
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
	   File fullFile = new File(localPath);
//    File fullFile = new File(Paths.generateFileSystemResourceUri(localPath));
    String fullLocalPathName = null;
    File fullLocalFile = new File(collectionPath);
//    File fullLocalFile = new File(Paths.generateFileSystemResourceUri(collectionPath));
    try {
      fullFilePathName = fullFile.getCanonicalPath();
      fullLocalPathName = fullLocalFile.getCanonicalPath();
    } catch (IOException e) {
      System.out.println("Failed to read file path: " + localPath);
    }

    collectionPath = collectionPath.replace("\\", "/");
    localPath = localPath.replace("\\", "/");
    fullFilePathName = fullFilePathName.replace('\\', '/');
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

}
