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

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
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
     * Decode HpcIntegratedSystemAccount.
     *
     * @param doc The HpcDataTransferAccount document.
     * @param decoderContext The decoder context.
     * @param registry Codec registry.
     * @return Decoded HpcDataTransferAcccount object.
     */
    public static HpcIntegratedSystemAccount 
                  decodeIntegratedSystemAccount(Document doc, 
                		                        DecoderContext decoderContext,
    		                                    CodecRegistry registry)
    {
    	if(doc == null || registry == null) {
        	   return null;
       	}
    	
    	BsonDocumentReader docReader = 
    		new BsonDocumentReader(doc.toBsonDocument(Document.class, registry));
		return registry.get(HpcIntegratedSystemAccount.class).decode(docReader, decoderContext);
	}
	 
    //---------------------------------------------------------------------//
    // Dataset Domain Object Decoders
    //---------------------------------------------------------------------//
    
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
}

 