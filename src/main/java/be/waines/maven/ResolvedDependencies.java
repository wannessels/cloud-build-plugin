package be.waines.maven;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ResolvedDependencies {

	private ConcurrentMap<ModuleIdentifier, Boolean> dependencies = new ConcurrentHashMap<ModuleIdentifier, Boolean>();

	public synchronized void add(ModuleIdentifier identifier, boolean isCleaned) {
		dependencies.put(identifier, isCleaned);
	}

	public synchronized boolean alreadyResolved(ModuleIdentifier identifier) {
		return dependencies.containsKey(identifier);
	}

	public synchronized boolean isUpdated(ModuleIdentifier identifier) {
		Boolean isCleaned = dependencies.get(identifier);
		return isCleaned != null && isCleaned;
	}

}
