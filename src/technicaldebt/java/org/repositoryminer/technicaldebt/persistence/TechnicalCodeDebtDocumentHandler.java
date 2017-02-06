package org.repositoryminer.technicaldebt.persistence;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.repositoryminer.persistence.Connection;
import org.repositoryminer.persistence.handler.DocumentHandler;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

public class TechnicalCodeDebtDocumentHandler extends DocumentHandler {

	private static final String COLLECTION_NAME = "td_technical_code_debt";

	public TechnicalCodeDebtDocumentHandler() {
		super.collection = Connection.getInstance().getCollection(COLLECTION_NAME);
	}

	public List<Document> findByCommit(String commit) {
		final Bson where = new BasicDBObject("commit", commit);
		return findMany(where);
	}

	public List<Document> findByTypes(String commit, List<String> types) {
		final List<Bson> clauses = new ArrayList<Bson>();

		final Bson clause1 = new BasicDBObject("commit", commit);
		clauses.add(clause1);

		if (types != null && !types.isEmpty()) {
			final Bson clause2 = Filters.in("debts", types);
			clauses.add(clause2);
		}

		final Bson where = Filters.and(clauses);
		return findMany(where);
	}

	public List<Document> findByIndicators(String commit, List<String> indicators) {
		final List<Bson> clauses = new ArrayList<Bson>();

		final Bson clause1 = new BasicDBObject("commit", commit);
		clauses.add(clause1);

		if (indicators != null && !indicators.isEmpty()) {
			final List<Bson> existClauses = new ArrayList<Bson>();
			for (String i : indicators) {
				existClauses.add(Filters.exists("indicators."+i));
			}
			clauses.add(Filters.or(existClauses));
		}

		final Bson where = Filters.and(clauses);
		return findMany(where);
	}

	public List<Document> findByConfirmation(String commit, boolean isTD) {
		final Bson clause1 = new BasicDBObject("commit", commit);
		final Bson clause2 = new BasicDBObject("technical_debt", isTD);
		final Bson where = Filters.and(clause1, clause2);
		return findMany(where);
	}

	public List<Document> findToManagement(String commit, List<String> types, List<String> indicators, Boolean isTD) {
		final List<Bson> clauses = new ArrayList<Bson>();

		final Bson clause1 = new BasicDBObject("commit", commit);
		clauses.add(clause1);
		
		if (types != null && !types.isEmpty()) {
			final Bson clause2 = Filters.in("debts", types);
			clauses.add(clause2);
		}
		
		if (indicators != null && !indicators.isEmpty()) {
			final List<Bson> existClauses = new ArrayList<Bson>();
			for (String i : indicators) {
				existClauses.add(Filters.exists("indicators."+i));
			}
			clauses.add(Filters.or(existClauses));
		}
		
		if (isTD != null) {
			final Bson clause3 = new BasicDBObject("technical_debt", isTD);
			clauses.add(clause3);
		}
		
		final Bson where = Filters.and(clauses);
		return findMany(where);
	}

	public Document findByFile(long filehash, String commit, Bson projection) {
		Bson clause1 = new BasicDBObject("filehash", filehash);
		Bson clause2 = new BasicDBObject("commit", commit);
		return findOne(Filters.and(clause1, clause2), projection);
	}
	
}