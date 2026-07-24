package gov.nih.nci.hpc.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides per-path lock objects shared across components.
 */
public final class HpcExternalArchiveLinkLockManager {

	private static final class PathLockRef {
		private final Object lock = new Object();
		private int refCount = 0;
	}

	private static final Map<String, PathLockRef> PATH_LOCKS = new HashMap<>();

	private HpcExternalArchiveLinkLockManager() {
	}

	public static Object getPathLock(String path) {
		synchronized (PATH_LOCKS) {
			PathLockRef lockRef = PATH_LOCKS.get(path);
			if (lockRef == null) {
				lockRef = new PathLockRef();
				PATH_LOCKS.put(path, lockRef);
			}
			lockRef.refCount++;
			return lockRef.lock;
		}
	}

	public static void deletePathLock(String path) {
		if (path == null || path.isEmpty()) {
			return;
		}

		synchronized (PATH_LOCKS) {
			PathLockRef lockRef = PATH_LOCKS.get(path);
			if (lockRef == null) {
				return;
			}

			lockRef.refCount--;
			if (lockRef.refCount <= 0) {
				PATH_LOCKS.remove(path);
			}
		}
	}
}
