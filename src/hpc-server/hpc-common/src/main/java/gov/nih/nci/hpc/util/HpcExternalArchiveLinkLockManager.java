package gov.nih.nci.hpc.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides per-path lock objects shared across components.
 */
public final class HpcExternalArchiveLinkLockManager {

	private static final Map<String, Object> PATH_LOCKS = new HashMap<>();

	private HpcExternalArchiveLinkLockManager() {
	}

	public static Object getPathLock(String path) {
		synchronized (PATH_LOCKS) {
			Object lock = PATH_LOCKS.get(path);
			if (lock == null) {
				lock = new Object();
				PATH_LOCKS.put(path, lock);
			}
			return lock;
		}
	}
}
