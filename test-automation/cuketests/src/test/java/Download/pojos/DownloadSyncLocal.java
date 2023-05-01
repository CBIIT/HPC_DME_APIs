package Download.pojos;

public class DownloadSyncLocal {

	public SyncType getSynchronousDownloadFilter() {
		return synchronousDownloadFilter;
	}

	public void setSynchronousDownloadFilter(SyncType synchronousDownloadFilter) {
		this.synchronousDownloadFilter = synchronousDownloadFilter;
	}

	SyncType synchronousDownloadFilter;

}
