/**
 * HpcManagedDatasetsCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.domain.HpcManagedDatasets;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>
 * HPC Managed Datasets Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetsCodec extends HpcCodec<HpcManagedDatasets>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // BSON Document keys.
    private final static String DATASETS_KEY = "datasets"; 
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcManagedDatasetsCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // CollectibleCodec<HpcManagedDatasets> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
					   HpcManagedDatasets managedDatasets,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		List<HpcDataset> datasets = managedDatasets.getDatasets();
 
		// Set the data on the BSON document.
		if(datasets != null && datasets.size() > 0) {
		   document.put(DATASETS_KEY, datasets);
		}

		getRegistry().get(Document.class).encode(writer, document, encoderContext);
	}
 
	@Override
	public HpcManagedDatasets decode(BsonReader reader, 
			                         DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcManagedDatasets managedDatasets = new HpcManagedDatasets();
		List<HpcDataset> datasets = 
				         (List<HpcDataset>) document.get(DATASETS_KEY);
		if(datasets != null) {
		   for(HpcDataset dataset : datasets) {
			   managedDatasets.getDatasets().add(dataset);
		   }
		}
		
		return managedDatasets;
	}
	
	@Override
	public Class<HpcManagedDatasets> getEncoderClass() 
	{
		return HpcManagedDatasets.class;
	}
}

 