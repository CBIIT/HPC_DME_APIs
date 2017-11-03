/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonObjectMapper extends ObjectMapper {
	@PostConstruct
	public void customConfiguration() {
		// Uses Enum.toString() for serialization of an Enum
		this.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		// Uses Enum.toString() for deserialization of an Enum
		this.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
	}
}
