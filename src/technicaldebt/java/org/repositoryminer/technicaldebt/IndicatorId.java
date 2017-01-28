package org.repositoryminer.technicaldebt;

public enum IndicatorId {

	GOD_CLASS,
	COMPLEX_METHOD,
	DUPLICATED_CODE,
	BRAIN_METHOD,
	SLOW_ALGORITHM,
	MULTITHREAD_CORRECTNESS,
	AUTOMATIC_STATIC_ANALYSIS_ISSUES,
	DATA_CLASS,
	REFUSED_PARENT_BEQUEST,
	DEPTH_OF_INHERITANCE_TREE;

	public static IndicatorId getTechnicalDebtIndicator(String indicatorName) {
		for (IndicatorId indicator : IndicatorId.values()) {
			if (indicator.toString().equals(indicatorName)) {
				return indicator;
			}
		}
		
		return null;
	}
	
}