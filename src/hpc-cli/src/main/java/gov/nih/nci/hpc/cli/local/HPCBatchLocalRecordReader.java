package gov.nih.nci.hpc.cli.local;

import static org.easybatch.core.util.Utils.checkNotNull;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.easybatch.core.reader.RecordReader;
import org.easybatch.core.reader.RecordReaderClosingException;
import org.easybatch.core.record.Header;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecord;

import gov.nih.nci.hpc.cli.domain.HPCLocalFileRecord;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;

public class HPCBatchLocalRecordReader implements RecordReader {
	private List<HpcPathAttributes> files;

	private Iterator<HpcPathAttributes> iterator;
	private Long recordCounter = 1L;

	/**
	 * Reader that uses
	 * <a href="http://commons.apache.org/proper/commons-csv/">Apache Common
	 * CSV</a> to read {@link ApacheCommonCsvRecord} instances from a CSV data
	 * source.
	 * <p/>
	 * This reader produces {@link ApacheCommonCsvRecord} instances.
	 */
	public HPCBatchLocalRecordReader(final List<HpcPathAttributes> files) {
		checkNotNull(files, "Local file list");
		this.files = files;
	}

	@Override
	public void open() {
		iterator = files.iterator();
	}

	@Override
	public boolean hasNextRecord() {
		return iterator.hasNext();
	}

	@Override
	public HPCLocalFileRecord readNextRecord() {
		Header header = new Header(recordCounter, getDataSourceName(), new Date());
		recordCounter++;
		HPCLocalFileRecord local = new HPCLocalFileRecord(header, iterator.next());
		local.setRecordNumber(recordCounter);
		return local;
	}

	@Override
	public Long getTotalRecords() {
		return null;
	}

	@Override
	public String getDataSourceName() {
		return null;
	}

	@Override
	public void close() throws RecordReaderClosingException {
	}

}