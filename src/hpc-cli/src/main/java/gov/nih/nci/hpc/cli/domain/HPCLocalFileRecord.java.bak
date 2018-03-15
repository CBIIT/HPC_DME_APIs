package gov.nih.nci.hpc.cli.domain;

import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Header;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecord;

import gov.nih.nci.hpc.cli.util.HpcPathAttributes;

public class HPCLocalFileRecord extends GenericRecord<HpcPathAttributes> {

	/**
	 * Create a {@link ApacheCommonCsvRecord}.
	 *
	 * @param header
	 *            the record header
	 * @param payload
	 *            the record payload
	 */
	private Long recordNumber;

	public HPCLocalFileRecord(final Header header, final HpcPathAttributes payload) {
		super(header, payload);
	}

	public Long getRecordNumber() {
		return recordNumber;
	}

	public void setRecordNumber(Long recordNumber) {
		this.recordNumber = recordNumber;
	}

}