/**
 * HpcManagedDataCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.dto.types.HpcManagedDataType;
import gov.nih.nci.hpc.domain.HpcManagedData;

import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * HPC Managed Data Codec. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDataCodec extends HpcCodec<HpcManagedData>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // BSON Document keys.
	private final static String ID_KEY = "id"; 
	private final static String TYPE_KEY = "type"; 
	private final static String CREATED_KEY = "created"; 
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
    public HpcManagedDataCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcManagedData> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, HpcManagedData managedData,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();
 
		// Extract the data from the domain object.
		String id = managedData.getId();
		HpcManagedDataType type = managedData.getType();
		Calendar created = managedData.getCreated();
		List<HpcDataset> datasets = managedData.getDatasets();
 
		// Set the data on the BSON document.
		if(id != null) {
		   document.put(ID_KEY, id);
		}
		if(type != null) {
		   document.put(TYPE_KEY, type.value());
		}
		if(created != null) {
		   document.put(CREATED_KEY, created.getTime());
		}
		if(datasets != null && datasets.size() > 0) {
		   document.put(DATASETS_KEY, datasets);
		}

		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcManagedData decode(BsonReader reader, 
			                     DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
				 getRegistry().get(Document.class).decode(reader, 
						                                  decoderContext);
		
		// Map the BSON Document to a domain object.
		HpcManagedData managedData = new HpcManagedData();
		managedData.setId(document.get(ID_KEY, String.class));
		managedData.setType( 
		       HpcManagedDataType.fromValue(document.get(TYPE_KEY, String.class)));
		Calendar created = Calendar.getInstance();
		created.setTime(document.get(CREATED_KEY, Date.class));
		managedData.setCreated(created);
		List<HpcDataset> datasets = 
				         (List<HpcDataset>) document.get(DATASETS_KEY);
		if(datasets != null) {
		   for(HpcDataset dataset : datasets) {
			   managedData.getDatasets().add(dataset);
		   }
		}
		
		return managedData;
	}
	
	@Override
	public Class<HpcManagedData> getEncoderClass() 
	{
		return HpcManagedData.class;
	}
}

 