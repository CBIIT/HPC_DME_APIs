package gov.nih.nci.hpc.cli.download;

import org.easybatch.core.record.GenericRecord;
import org.easybatch.core.record.Header;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;

public class HPCBatchCollectionDownloadRecord extends GenericRecord<HpcCollectionListingEntry> {

	/**
	 * Create a HPCBatchCollectionDownloadRecord.
	 *
	 * @param header
	 *            the record header
	 * @param payload
	 *            the record payload
	 */
	private Long recordNumber;

	public HPCBatchCollectionDownloadRecord(final Header header, final HpcCollectionListingEntry payload) {
		super(header, payload);
	}

	public Long getRecordNumber() {
		return recordNumber;
	}

	public void setRecordNumber(Long recordNumber) {
		this.recordNumber = recordNumber;
	}

	@Override
	public String toString() {
		return "HPCBatchCollectionDownloadRecord [recordNumber=" + recordNumber + "]";
	}

}