/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import org.easybatch.core.filter.RecordFilter;
import org.easybatch.core.record.Record;

public class HPCHeaderRecordFilter implements RecordFilter<Record> {

	@Override
	public Record processRecord(final Record record) {
		if (record.getHeader().getNumber() == 0) {
			return null;
		}
		return record;
	}

}
