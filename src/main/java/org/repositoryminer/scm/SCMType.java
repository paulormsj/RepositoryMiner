package org.repositoryminer.scm;

public enum SCMType {

	GIT;

	public static SCMType parse(String name) {
		if (name == null) {
			return null;
		}
		
		for (SCMType scm : SCMType.values()) {
			if (scm.toString().equals(name)) {
				return scm;
			}
		}
		
		return null;
	}
	
}