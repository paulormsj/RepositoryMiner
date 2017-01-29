package org.repositoryminer.checkstyle.logger;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

public class RepositoryMinerAudit implements AuditListener {

	@Override
	public void auditStarted(AuditEvent event) {
		// Nothing is needed here
	}

	@Override
	public void auditFinished(AuditEvent event) {
		// Nothing is needed here
	}

	@Override
	public void fileStarted(AuditEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void fileFinished(AuditEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addError(AuditEvent event) {
		// Nothing is needed here
	}

	@Override
	public void addException(AuditEvent event, Throwable throwable) {
		// Nothing is needed here
	}

}