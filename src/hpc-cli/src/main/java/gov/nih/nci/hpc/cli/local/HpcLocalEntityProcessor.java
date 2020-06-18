package gov.nih.nci.hpc.cli.local;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import gov.nih.nci.hpc.cli.domain.HpcMetadataAttributes;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.cli.util.Paths;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.easybatch.core.processor.RecordProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HpcLocalEntityProcessor {
  protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
  protected HpcServerConnection connection;

	public HpcLocalEntityProcessor(HpcServerConnection connection) throws IOException, FileNotFoundException {
		this.connection = connection;
	}

	public abstract boolean process(HpcPathAttributes entity, String localPath, String filePathBaseName, String destinationBasePath,
			String logFile, String recordFile, boolean metadataOnly, boolean directUpload, boolean checksum, String metadataFile)
			throws RecordProcessingException;

  protected List<HpcMetadataEntry> getMetadata(HpcPathAttributes file, boolean metadataOnly, String externalMetadataFile)
      throws HpcCmdException {
//		String fullPath = file.getAbsolutePath();
//		File metadataFile = new File(fullPath + ".metadata.json");
    logger.debug("getMetadata: "+file.toString());
    File metadataFile = null;
    if(externalMetadataFile != null) {
      metadataFile = new File(externalMetadataFile);
    } else {
      final String metadataFilePath = file.getAbsolutePath().concat(".metadata.json");
      metadataFile = new File(metadataFilePath);
    }
    List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
    if (metadataFile.exists()) {
      MappingJsonFactory factory = new MappingJsonFactory();
      JsonParser parser;
      try {
        parser = factory.createParser(new FileInputStream(metadataFile));
        HpcMetadataAttributes metadataAttributes = parser.readValueAs(HpcMetadataAttributes.class);
        metadataEntries = metadataAttributes.getMetadataEntries();
        // metadataEntries = parser.readValueAs(new
        // TypeReference<List<HpcMetadataEntry>>() {
        // });
      } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
        logger.error(e.getMessage(), e);
        throw new HpcCmdException(
            "Failed to read JSON metadata file: " + file.getAbsolutePath() + " Reason: " + e
                .getMessage());
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
        throw new HpcCmdException(
            "Failed to read JSON metadata file: " + file.getAbsolutePath() + " Reason: " + e
                .getMessage());
      }
    } else {
      if (metadataOnly) {
        return null;
      }
      HpcMetadataEntry nameEntry = new HpcMetadataEntry();
      nameEntry.setAttribute("name");
      nameEntry.setValue(file.getName());
      metadataEntries.add(nameEntry);
      HpcMetadataEntry dateEntry = new HpcMetadataEntry();
      dateEntry.setAttribute("modified_date");
      if (file.getUpdatedDate() != null) {
        dateEntry.setValue(file.getUpdatedDate());
      } else {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        dateEntry.setValue(sdf.format(new Date()));
      }
      metadataEntries.add(dateEntry);
      if (file.getIsDirectory()) {
        HpcMetadataEntry typeEntry = new HpcMetadataEntry();
        typeEntry.setAttribute("collection_type");
        typeEntry.setValue("Folder");
        metadataEntries.add(typeEntry);
      }
    }
    logger.debug("getMetadata: metadataEntries "+metadataEntries);
    return metadataEntries;
  }

	protected List<HpcMetadataEntry> getParentCollectionMetadata(HpcPathAttributes file, boolean metadataOnly) {
		List<HpcMetadataEntry> parentCollectionMetadataEntries = new ArrayList<HpcMetadataEntry>();
		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		String filePath = file.getAbsolutePath().replace("\\", "/");

		int pathIndex = filePath.lastIndexOf("/");

		String parentPath = filePath.substring(0, pathIndex == -1 ? filePath.length() : pathIndex);
		int nameIndex = parentPath.lastIndexOf(File.separator);
		String parentName = parentPath.substring(nameIndex != -1 ? nameIndex : 0, parentPath.length());
		pathAttributes.setPath(parentPath.replace("/", File.separator));
		pathAttributes.setName(parentName);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		pathAttributes.setUpdatedDate(sdf.format(new Date()));
		pathAttributes.setAbsolutePath(parentPath.replace("/", File.separator));
		List<HpcMetadataEntry> metadata = getMetadata(pathAttributes, metadataOnly, null);

		if (metadata == null)
			return null;
		else
			parentCollectionMetadataEntries.addAll(metadata);

		boolean collectionType = false;
		for (HpcMetadataEntry entry : parentCollectionMetadataEntries) {
			if (entry.getAttribute().equals("collection_type"))
				collectionType = true;
		}

		if (!collectionType) {
			HpcMetadataEntry typeEntry = new HpcMetadataEntry();
			typeEntry.setAttribute("collection_type");
			typeEntry.setValue("Folder");
			parentCollectionMetadataEntries.add(typeEntry);
		}

		return parentCollectionMetadataEntries;
	}

}
