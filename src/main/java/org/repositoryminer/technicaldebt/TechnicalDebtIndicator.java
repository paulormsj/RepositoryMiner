package org.repositoryminer.technicaldebt;

public enum TechnicalDebtIndicator {

	GOD_CLASS,
	COMPLEX_METHOD,
	DUPLICATED_CODE,
	BRAIN_METHOD,
	SLOW_ALGORITHM,
	MULTITHREAD_CORRECTNESS,
	AUTOMATIC_STATIC_ANALYSIS_ISSUES;

	public static TechnicalDebtIndicator getTechnicalDebtIndicator(String indicatorName) {
		for (TechnicalDebtIndicator indicator : TechnicalDebtIndicator.values()) {
			if (indicator.toString().equals(indicatorName)) {
				return indicator;
			}
		}
		
		return null;
	}
	
}