package org.repositoryminer.checkstyle.persistence;

import org.repositoryminer.persistence.Connection;
import org.repositoryminer.persistence.handler.DocumentHandler;

public class CheckstyleDocumentHandler extends DocumentHandler {

	private static final String COLLECTION_NAME = "checkstyle_audit";

	public CheckstyleDocumentHandler() {
		super.collection = Connection.getInstance().getCollection(COLLECTION_NAME);
	}
	
}