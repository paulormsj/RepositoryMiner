package org.repositoryminer.technicaldebt.codedebt;

import org.bson.Document;
import org.repositoryminer.technicaldebt.TechnicalDebtId;

public interface ITechnicalCodeDebt {

	public TechnicalDebtId getId();
	
	public Document detect(String filename, String filestate, String snapshot);
	
}