package org.repositoryminer.technicaldebt.persistence;

import org.repositoryminer.persistence.Connection;
import org.repositoryminer.persistence.handler.DocumentHandler;

public class TechnicalCodeDebtDocumentHandler extends DocumentHandler {

	private static final String COLLECTION_NAME = "td_technical_code_debt";

	public TechnicalCodeDebtDocumentHandler() {
		super.collection = Connection.getInstance().getCollection(COLLECTION_NAME);
	}
	
}