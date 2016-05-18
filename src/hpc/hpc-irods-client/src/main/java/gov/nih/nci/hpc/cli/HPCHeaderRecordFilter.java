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