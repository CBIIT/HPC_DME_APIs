/**
 * HpcFileMetadataHistoryCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import static gov.nih.nci.hpc.dao.mongo.codec.HpcDecoder.decodeFileMetadataVersion;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataHistory;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataVersion;

import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC File Metadata History Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcFileMetadataHistoryCodec extends HpcCodec<HpcFileMetadataHistory>
{ 
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcFileMetadataHistoryCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcFileMetadataHistory> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcFileMetadataHistory metadataHistory,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		String fileId = metadataHistory.getFileId();
		int maxVersion = metadataHistory.getMaxVersion();
		List<HpcFileMetadataVersion> versions = metadataHistory.getVersions();
 
		// Set the data on the BSON document.
		if(fileId != null) {
		   document.put(FILE_METADATA_HISTORY_FILE_ID_KEY, fileId);
		}
		document.put(FILE_METADATA_HISTORY_MAX_VERSION_KEY, maxVersion);
		if(versions != null && !versions.isEmpty()) {
		   document.put(FILE_METADATA_HISTORY_VERSIONS_KEY, versions);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	@SuppressWarnings("unchecked")
	public HpcFileMetadataHistory decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcFileMetadataHistory metadataHistory = new HpcFileMetadataHistory();
		metadataHistory.setFileId(
				        document.getString(FILE_METADATA_HISTORY_FILE_ID_KEY));
		metadataHistory.setMaxVersion(
				        document.getInteger(FILE_METADATA_HISTORY_MAX_VERSION_KEY));
		
		List<Document> versionDocuments = 
			(List<Document>) document.get(FILE_METADATA_HISTORY_VERSIONS_KEY);
		if(versionDocuments != null) {
		   for(Document versionDocument : versionDocuments) {
			   metadataHistory.getVersions().add(
					   decodeFileMetadataVersion(versionDocument, 
		    				                     decoderContext,
		    				                     getRegistry()));
		   }
		}
		
		return metadataHistory;
	}
	
	@Override
	public Class<HpcFileMetadataHistory> getEncoderClass() 
	{
		return HpcFileMetadataHistory.class;
	}
}

 