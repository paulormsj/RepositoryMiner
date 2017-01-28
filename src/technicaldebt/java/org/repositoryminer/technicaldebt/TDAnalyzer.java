package org.repositoryminer.technicaldebt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.repositoryminer.model.Commit;
import org.repositoryminer.model.Reference;
import org.repositoryminer.persistence.handler.CommitDocumentHandler;
import org.repositoryminer.persistence.handler.ReferenceDocumentHandler;
import org.repositoryminer.persistence.handler.WorkingDirectoryDocumentHandler;
import org.repositoryminer.scm.ReferenceType;
import org.repositoryminer.technicaldebt.persistence.TechnicalCodeDebtDocumentHandler;
import org.repositoryminer.utility.StringUtils;

import com.mongodb.client.model.Projections;

public class TDAnalyzer {

	private static Map<TechnicalDebtId, List<IndicatorId>> debts = new HashMap<TechnicalDebtId, List<IndicatorId>>();
	
	// Initializes the types of debts with theirs respectives indicators.
	static {
		List<IndicatorId> codeDebt = new ArrayList<IndicatorId>(9);
		List<IndicatorId> desingDebt = new ArrayList<IndicatorId>(8);
		
		codeDebt.add(IndicatorId.AUTOMATIC_STATIC_ANALYSIS_ISSUES);
		codeDebt.add(IndicatorId.GOD_CLASS);
		codeDebt.add(IndicatorId.COMPLEX_METHOD);
		codeDebt.add(IndicatorId.DUPLICATED_CODE);
		codeDebt.add(IndicatorId.DATA_CLASS);
		codeDebt.add(IndicatorId.BRAIN_METHOD);
		codeDebt.add(IndicatorId.REFUSED_PARENT_BEQUEST);
		
		desingDebt.addAll(codeDebt);
		
		codeDebt.add(IndicatorId.SLOW_ALGORITHM);
		codeDebt.add(IndicatorId.MULTITHREAD_CORRECTNESS);
		
		desingDebt.add(IndicatorId.DEPTH_OF_INHERITANCE_TREE);
	}
	
	private String repositoryId;

	private ReferenceDocumentHandler refPersist = new ReferenceDocumentHandler();
	private CommitDocumentHandler commitPersist = new CommitDocumentHandler();
	private WorkingDirectoryDocumentHandler wdHandler = new WorkingDirectoryDocumentHandler();
	private TechnicalCodeDebtDocumentHandler codeTDHandler = new TechnicalCodeDebtDocumentHandler();

	private CodeIndicatorsAnalyzer indicatorsAnalyzer = new CodeIndicatorsAnalyzer();

	public TDAnalyzer(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public void analyzeTD(String hash) {
		analyze(hash);
	}

	public void analyzeTD(String name, ReferenceType type) {
		Document refDoc = refPersist.findByNameAndType(name, type, repositoryId, Projections.slice("commits", 1));
		Reference reference = Reference.parseDocument(refDoc);

		String commitId = reference.getCommits().get(0);
		analyze(commitId);
	}

	@SuppressWarnings("unchecked")
	private void analyze(String commitId) {
		Commit commit = Commit.parseDocument(commitPersist.findById(commitId, Projections.include("commit_date")));
		Document wd = wdHandler.findById(commit.getId());

		List<Document> documents = new ArrayList<Document>();

		for (Document file : (List<Document>) wd.get("files")) {
			Map<IndicatorId, Integer> indicators = indicatorsAnalyzer.detect(file.getString("file"),
					file.getString("checkout"), commit.getId());

			if (indicators.isEmpty()) {
				continue;
			}

			Document doc = new Document();
			doc.append("commit", commit.getId());
			doc.append("commit_date", commit.getCommitDate());
			doc.append("repository", new ObjectId(repositoryId));
			doc.append("filename", file.getString("file"));
			doc.append("filestate", file.getString("checkout"));
			doc.append("filehash", StringUtils.encodeToCRC32(file.getString("file")));
			doc.append("td_confirmed", false);
			
			Document indicatorsDoc = new Document();
			for (Entry<IndicatorId, Integer> entry : indicators.entrySet()) {
				indicatorsDoc.append(entry.getKey().toString(), entry.getValue());
			}
 			
			List<String> debtsNames = new ArrayList<String>();
			for (Entry<TechnicalDebtId, List<IndicatorId>> debtEntry : debts.entrySet()) {
				for (IndicatorId indicator : indicators.keySet()) {
					if (debtEntry.getValue().contains(indicator)) {
						debtsNames.add(debtEntry.getKey().toString());
						break;
					}
				}
			}
			
			doc.append("indicators", indicatorsDoc);
			doc.append("debts", debtsNames);

			documents.add(doc);
		}

		codeTDHandler.insertMany(documents);
	}

}