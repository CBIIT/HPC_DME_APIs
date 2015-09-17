/**
 * HpcDecoder.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataVersion;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;

import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * <p>
 * Helper class to decode domain objects.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

class HpcDecoder 
{   
    //---------------------------------------------------------------------//
    // User Domain Objects Decoders
    //---------------------------------------------------------------------//
	
    /**
     * Decode HpcNciAccount.
     *
     * @param doc The HpcNciAccount document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcNciAccount object.
     */
    public static HpcNciAccount decodeNciAccount(Document doc, 
    		                                     DecoderContext decoderContext,
    		                                     CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
        	   return null;
       	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcNciAccount.class).decode(docReader, decoderContext);
	}
    
    /**
     * Decode HpcDataTransferAccount.
     *
     * @param doc The HpcDataTransferAccount document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcDataTransferAcccount object.
     */
    public static HpcDataTransferAccount 
                  decodeDataTransferAccount(Document doc, 
                		                    DecoderContext decoderContext,
    		                                CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
        	   return null;
       	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcDataTransferAccount.class).decode(docReader, decoderContext);
	}
	 
    //---------------------------------------------------------------------//
    // Dataset Domain Object Decoders
    //---------------------------------------------------------------------//
    
    /**
     * Decode HpcFileSet.
     *
     * @param doc The HpcFileSet document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcFileSet object.
     */
    public static HpcFileSet decodeFileSet(Document doc, 
    		                               DecoderContext decoderContext, 
    		                               CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
    	   return null;
    	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcFileSet.class).decode(docReader, decoderContext);
	}
    
    /**
     * Decode HpcDataTransferRequest.
     *
     * @param doc The HpcDataTransferRequest document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcDataTransferRequest object.
     */
    public static HpcDataTransferRequest 
           decodeDataTransferRequest(Document doc, DecoderContext decoderContext,
        		                     CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
     	   return null;
     	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcDataTransferRequest.class).decode(docReader, decoderContext);
	}
    
    /**
     * Decode HpcFileLocation.
     *
     * @param doc The HpcFileLocation document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcFileLocation object.
     */
    public static HpcFileLocation decodeFileLocation(Document doc, 
    		                                         DecoderContext decoderContext,
    		                                         CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
      	   return null;
      	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcFileLocation.class).decode(docReader, decoderContext);
	}	
    
    /**
     * Decode HpcDataTransferLocations.
     *
     * @param doc The HpcDataTransferLocations document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcDataTransferLocations object.
     */
    public static HpcDataTransferLocations 
                  decodeDataTransferLocations(Document doc, 
    		                                  DecoderContext decoderContext,
    		                                  CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
       	   return null;
       	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcDataTransferLocations.class).decode(docReader, decoderContext);
	}	
	
    /**
     * Decode HpcDataTransferReport.
     *
     * @param doc The HpcDataTransferReport document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcDataTransferReport object.
     */
    public static HpcDataTransferReport decodeDataTransferReport(
    		                                  Document doc, 
    		                                  DecoderContext decoderContext,
    		                                  CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
       	   return null;
       	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcDataTransferReport.class).decode(docReader, decoderContext);
	}	
    
    /**
     * Decode HpcFileMetadata.
     *
     * @param doc The HpcFileMetadata document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcFileMetadata object.
     */
    public static HpcFileMetadata decodeFileMetadata(Document doc, 
    		                                         DecoderContext decoderContext,
    		                                         CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
        	   return null;
        }
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcFileMetadata.class).decode(docReader, decoderContext);
	}
    
    /**
     * Decode HpcFileMetadataVersion.
     *
     * @param doc The HpcFileMetadataVersion document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcFileMetadataVersion object.
     */
    public static HpcFileMetadataVersion 
                  decodeFileMetadataVersion(Document doc, 
    		                                DecoderContext decoderContext,
    		                                CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
        	   return null;
        }
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcFileMetadataVersion.class).decode(docReader, decoderContext);
	}
	
    /**
     * Decode HpcFilePrimaryMetadata.
     *
     * @param doc The HpcFilePrimaryMetadata document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcFilePrimaryMetadata object.
     */
    public static HpcFilePrimaryMetadata 
                  decodeFilePrimaryMetadata(Document doc, 
    		                                DecoderContext decoderContext,
    		                                CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
     	   return null;
    	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class,  registry));
		return registry.get(HpcFilePrimaryMetadata.class).decode(docReader, decoderContext);
	}
    
    /**
     * Decode HpcMetadataItem.
     *
     * @param doc The HpcMetadataItem document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcMetadataItem object.
     */
    public static HpcMetadataItem decodeMetadataItem(Document doc, 
    		                                         DecoderContext decoderContext,
    		                                         CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
     	   return null;
    	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcMetadataItem.class).decode(docReader, decoderContext);
	}
    
    /**
     * Decode HpcFile.
     *
     * @param doc The HpcFile document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcFile object.
     */
    public static HpcFile decodeFile(Document doc, DecoderContext decoderContext,
    		                         CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
      	   return null;
     	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcFile.class).decode(docReader, decoderContext);
	}
    
    //---------------------------------------------------------------------//
    // Project Domain Object Decoders
    //---------------------------------------------------------------------//
    
	/**
     * Decode HpcProjectMetadata.
     *
     * @param doc The HpcProjectMetadata document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcProjectMetadata object.
     */
    public static HpcProjectMetadata decodeProjectMetadata(Document doc, 
    		                                               DecoderContext decoderContext,
    		                                               CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
       	   return null;
      	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcProjectMetadata.class).decode(docReader, decoderContext);
	}
}

 