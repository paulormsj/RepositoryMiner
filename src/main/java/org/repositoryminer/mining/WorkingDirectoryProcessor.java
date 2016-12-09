package org.repositoryminer.mining;

import static org.repositoryminer.scm.DiffType.ADD;
import static org.repositoryminer.scm.DiffType.COPY;
import static org.repositoryminer.scm.DiffType.DELETE;
import static org.repositoryminer.scm.DiffType.MODIFY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.repositoryminer.listener.IMiningListener;
import org.repositoryminer.model.Diff;
import org.repositoryminer.model.Reference;
import org.repositoryminer.model.WorkingDirectory;
import org.repositoryminer.persistence.handler.CommitDocumentHandler;
import org.repositoryminer.persistence.handler.ReferenceDocumentHandler;
import org.repositoryminer.persistence.handler.WorkingDirectoryDocumentHandler;

import com.mongodb.client.model.Projections;

public class WorkingDirectoryProcessor {

	private static final int COMMIT_RANGE = 1000;

	private CommitDocumentHandler commitHandler;
	private ReferenceDocumentHandler referenceHandler;
	private WorkingDirectoryDocumentHandler wdHandler;
	
	private Set<String> commitsProcessed;
	private String repositoryId; 
	private List<Reference> references;
	private IMiningListener listener;
	private WorkingDirectory workingDirectory;
	
	public WorkingDirectoryProcessor() {
		commitsProcessed = new HashSet<String>();
		commitHandler = new CommitDocumentHandler();
		referenceHandler = new ReferenceDocumentHandler();
		wdHandler = new WorkingDirectoryDocumentHandler();
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public void setReferences(List<Reference> references) {
		this.references = references;
	}

	public void setListener(IMiningListener listener) {
		this.listener = listener;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void processWorkingDirectories() {
		for (Reference ref : references) {
			workingDirectory = new WorkingDirectory(repositoryId);
			Document refDoc = referenceHandler.findById(ref.getId(), Projections.include("commits"));

			List<String> commits = (List<String>) refDoc.get("commits");
			Collections.reverse(commits);

			int begin = 0;
			int end = Math.min(commits.size(), COMMIT_RANGE);

			while (end < commits.size()) {
				processCommits(new ArrayList(commits.subList(begin, end)), ref.getName(), begin, commits.size());
				begin = end;
				end = Math.min(commits.size(), COMMIT_RANGE + end);
			}

			processCommits(new ArrayList(commits.subList(begin, end)), ref.getName(), begin, commits.size());
		}
	}

	@SuppressWarnings("unchecked")
	private void processCommits(List<String> commits, String refName, int progress, int qtdCommits) {
		// not selects already processed commits
		List<String> newCommits = new ArrayList<String>();
		String prevCommit = null;
		
		for (String commit : commits) {
			if (commitsProcessed.contains(commit)) {
				prevCommit = commit;
				listener.workingDirectoryProgressChange(refName, commit, ++progress, qtdCommits);
			} else {
				newCommits.add(commit);
			}
		}
		
		if (newCommits.size() == 0) {
			return;
		}
		
		if (prevCommit != null) {
			workingDirectory = WorkingDirectory.parseDocument(wdHandler.findById(prevCommit));
		}
		
		List<Document> wdDocs = new ArrayList<Document>();
		for (Document doc : commitHandler.findByIdColl(repositoryId, newCommits, Projections.include("diffs"))) {
			String commitId = doc.get("_id").toString();
			
			listener.workingDirectoryProgressChange(refName, commitId, ++progress, qtdCommits);
			
			commitsProcessed.add(commitId);
			workingDirectory.setId(commitId);
			
			processDiff(Diff.parseDocuments((List<Document>) doc.get("diffs")));
			wdDocs.add(workingDirectory.toDocument());
		}
		
		wdHandler.insertMany(wdDocs);
	}

	private void processDiff(List<Diff> diffs) {
		for (Diff d : diffs) {
			if (d.getType() == ADD || d.getType() == MODIFY || d.getType() == COPY) {
				workingDirectory.getFiles().put(d.getPath(), workingDirectory.getId());
			} else if (d.getType() == DELETE) {
				workingDirectory.getFiles().remove(d.getPath());
			} else { // d.getType() == RENAME
				workingDirectory.getFiles().remove(d.getOldPath());
				workingDirectory.getFiles().put(d.getPath(), workingDirectory.getId());
			}
		}
	}

}