package org.repositoryminer.technicaldebt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.repositoryminer.model.Commit;
import org.repositoryminer.model.Contributor;
import org.repositoryminer.model.Reference;
import org.repositoryminer.model.Repository;
import org.repositoryminer.persistence.handler.CommitDocumentHandler;
import org.repositoryminer.persistence.handler.ReferenceDocumentHandler;
import org.repositoryminer.persistence.handler.RepositoryDocumentHandler;
import org.repositoryminer.persistence.handler.WorkingDirectoryDocumentHandler;
import org.repositoryminer.scm.ISCM;
import org.repositoryminer.scm.ReferenceType;
import org.repositoryminer.scm.SCMFactory;
import org.repositoryminer.technicaldebt.persistence.TechnicalCodeDebtDocumentHandler;
import org.repositoryminer.utility.StringUtils;

import com.mongodb.client.model.Projections;

public class TDAnalyzer {

	private static final Map<TechnicalDebtId, List<IndicatorId>> debts = new HashMap<TechnicalDebtId, List<IndicatorId>>();
	
	// Initializes the types of debts with theirs respectives indicators.
	static {
		final List<IndicatorId> codeDebt = new ArrayList<IndicatorId>(10);
		final List<IndicatorId> desingDebt = new ArrayList<IndicatorId>(8);
		
		codeDebt.add(IndicatorId.AUTOMATIC_STATIC_ANALYSIS_ISSUES);
		codeDebt.add(IndicatorId.GOD_CLASS);
		codeDebt.add(IndicatorId.COMPLEX_METHOD);
		codeDebt.add(IndicatorId.DUPLICATED_CODE);
		codeDebt.add(IndicatorId.DATA_CLASS);
		codeDebt.add(IndicatorId.BRAIN_METHOD);
		codeDebt.add(IndicatorId.REFUSED_PARENT_BEQUEST);
		
		desingDebt.addAll(codeDebt);
		
		codeDebt.add(IndicatorId.CODE_WITHOUT_STANDARDS);
		codeDebt.add(IndicatorId.SLOW_ALGORITHM);
		codeDebt.add(IndicatorId.MULTITHREAD_CORRECTNESS);
		
		desingDebt.add(IndicatorId.DEPTH_OF_INHERITANCE_TREE);
		
		debts.put(TechnicalDebtId.CODE_DEBT, codeDebt);
		debts.put(TechnicalDebtId.DESIGN_DEBT, desingDebt);
	}
	
	private final ReferenceDocumentHandler refPersist = new ReferenceDocumentHandler();
	private final CommitDocumentHandler commitPersist = new CommitDocumentHandler();
	private final WorkingDirectoryDocumentHandler wdHandler = new WorkingDirectoryDocumentHandler();
	private final TechnicalCodeDebtDocumentHandler codeTDHandler = new TechnicalCodeDebtDocumentHandler();

	private final CodeIndicatorsAnalyzer indicatorsAnalyzer = new CodeIndicatorsAnalyzer();
	
	private final ISCM scm;
	private final Repository repository;

	public TDAnalyzer(Repository repository) {
		this.repository = repository;
		scm = SCMFactory.getSCM(repository.getScm());
	}
	
	public TDAnalyzer(final String repositoryId) {
		final RepositoryDocumentHandler  handler = new RepositoryDocumentHandler();
		repository = Repository.parseDocument(handler.findById(repositoryId, Projections.include("scm", "path")));
		scm = SCMFactory.getSCM(repository.getScm());
	}

	public void execute(final String hash) {
		persistAnalysis(hash, null);
	}

	public void execute(final String name, final ReferenceType type) {
		final Document refDoc = refPersist.findByNameAndType(name, type, repository.getId(), Projections.slice("commits", 1));
		final Reference reference = Reference.parseDocument(refDoc);

		final String commitId = reference.getCommits().get(0);
		persistAnalysis(commitId, reference);
	}

	@SuppressWarnings("unchecked")
	private void persistAnalysis(final String commitId, final Reference ref) {
		final Commit commit = Commit.parseDocument(commitPersist.findById(commitId, Projections.include("commit_date")));
		final Document wd = wdHandler.findById(commit.getId());

		scm.open(repository.getPath());
		
		final List<Document> documents = new ArrayList<Document>();
		for (Document file : (List<Document>) wd.get("files")) {
			final Map<IndicatorId, Integer> indicators = indicatorsAnalyzer.detect(file.getString("file"),
					file.getString("checkout"), commit.getId());

			if (indicators.isEmpty()) {
				continue;
			}

			final Document doc = new Document();
			
			if (ref != null) {
				doc.append("reference_name", ref.getName());
				doc.append("reference_type", ref.getType().toString());
			}
			
			doc.append("commit", commit.getId());
			doc.append("commit_date", commit.getCommitDate());
			doc.append("repository", new ObjectId(repository.getId()));
			doc.append("filename", file.getString("file"));
			doc.append("filestate", file.getString("checkout"));
			doc.append("filehash", StringUtils.encodeToCRC32(file.getString("file")));
			doc.append("technical_debt", false);
			
			final List<Document> contribsDoc = new ArrayList<Document>();
			for (Contributor c : scm.getCommitters(file.getString("file"), commitId)) {
				contribsDoc.add(c.toDocument().append("colaborator", c.isCollaborator()));
			}
			doc.append("contributors", contribsDoc);
			
			final Document indicatorsDoc = new Document();
			for (Entry<IndicatorId, Integer> entry : indicators.entrySet()) {
				indicatorsDoc.append(entry.getKey().toString(), entry.getValue());
			}
 			
			final List<String> debtsNames = new ArrayList<String>();
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

		scm.close();
		codeTDHandler.insertMany(documents);
	}

}