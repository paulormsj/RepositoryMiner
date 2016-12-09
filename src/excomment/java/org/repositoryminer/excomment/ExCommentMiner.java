package org.repositoryminer.excomment;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.repositoryminer.excomment.model.Comment;
import org.repositoryminer.excomment.model.Heuristic;
import org.repositoryminer.excomment.model.Pattern;
import org.repositoryminer.excomment.persistence.ExCommentDocumentHandler;
import org.repositoryminer.model.Commit;
import org.repositoryminer.model.Reference;
import org.repositoryminer.model.Repository;
import org.repositoryminer.persistence.handler.CommitDocumentHandler;
import org.repositoryminer.persistence.handler.ReferenceDocumentHandler;
import org.repositoryminer.persistence.handler.RepositoryDocumentHandler;
import org.repositoryminer.scm.ReferenceType;

import com.mongodb.client.model.Projections;

public class ExCommentMiner {

	private static final String[] COMMENTS_HEADER = { "idcomment", "total_pattern", "total_heuristic", "total_score",
			"comment", "path", "class", "method" };
	private static final String[] PATTERNS_HEADER = { "idcomment", "pattern", "pattern_score", "pattern_class", "theme",
			"tdtype" };
	private static final String[] HEURISTICS_HEADER = { "idcomment", "heuristic_description", "heuristic_status",
			"heuristic_score" };
	
	private String commentsCSV, patternsCSV, heuristicsCSV;
	private char delimiter = ';';
	private Repository repository;

	private CommitDocumentHandler commitPersist;
	private ReferenceDocumentHandler refPersist;
	private ExCommentDocumentHandler exCommPersist;
	
	private Map<Integer, Comment> commentsMap;
	
	public ExCommentMiner() {
		commitPersist = new CommitDocumentHandler();
		refPersist = new ReferenceDocumentHandler();
		exCommPersist = new ExCommentDocumentHandler();
	}
	
	public void mineToCommit(String hash) throws IOException {
		Document commitDoc = commitPersist.findById(hash, Projections.include("commit_date"));
		Commit commit = Commit.parseDocument(commitDoc);

		readCSVs();
		
		Document doc = new Document();
		doc.append("commit", commit.getId());
		doc.append("commit_date", commit.getCommitDate());
		doc.append("repository", new ObjectId(repository.getId()));
		doc.append("comments", Comment.toDocumentList(commentsMap.values()));
		
		exCommPersist.insert(doc);
	}

	public void mineToReference(String name, ReferenceType type) throws IOException {
		Document refDoc = refPersist.findByNameAndType(name, type, repository.getId(), Projections.slice("commits", 1));
		Reference reference = Reference.parseDocument(refDoc);
		
		String commitId = reference.getCommits().get(0);
		Commit commit = Commit.parseDocument(commitPersist.findById(commitId, Projections.include("commit_date")));

		readCSVs();
		
		Document doc = new Document();
		doc.append("reference_name", reference.getName());
		doc.append("reference_type", reference.getType().toString());
		doc.append("commit", commit.getId());
		doc.append("commit_date", commit.getCommitDate());
		doc.append("repository", new ObjectId(repository.getId()));
		doc.append("comments", Comment.toDocumentList(commentsMap.values()));
		
		exCommPersist.insert(doc);
	}
	
	private List<CSVRecord> readCSV(String[] header, String filename) throws IOException {
		FileReader fileReader = new FileReader(filename);

		CSVFormat format = CSVFormat.DEFAULT.withDelimiter(delimiter).withHeader(header).withSkipHeaderRecord();

		CSVParser csvParser = new CSVParser(fileReader, format);

		List<CSVRecord> records = csvParser.getRecords();

		fileReader.close();
		csvParser.close();

		return records;
	}

	private void readCSVs() throws IOException {
		commentsMap = new HashMap<Integer, Comment>();

		readComments();
		readHeuristics();
		readPatterns();
	}
	
	private void readComments() throws IOException {
		List<CSVRecord> records = readCSV(COMMENTS_HEADER, commentsCSV);

		for (CSVRecord record : records) {
			Comment comment = new Comment(Integer.parseInt(record.get(0)), Float.parseFloat(record.get(1)),
					Float.parseFloat(record.get(2)), Float.parseFloat(record.get(3)), record.get(4), null,
					record.get(6), record.get(7));
			
			String path = FilenameUtils.normalize(record.get(5), true);
			path = path.substring(repository.getPath().length()+1);
			comment.setPath(path);
			
			commentsMap.put(comment.getId(), comment);
		}
	}

	private void readPatterns() throws IOException {
		List<CSVRecord> records = readCSV(PATTERNS_HEADER, patternsCSV);

		for (CSVRecord record : records) {
			Pattern pattern = new Pattern(record.get(1), Float.parseFloat(record.get(2)), record.get(3), record.get(4),
					record.get(5));

			Comment comment = commentsMap.get(Integer.parseInt(record.get(0)));
			if (comment == null) {
				continue;
			}

			comment.getPatterns().add(pattern);
		}
	}

	private void readHeuristics() throws IOException {
		List<CSVRecord> records = readCSV(HEURISTICS_HEADER, heuristicsCSV);

		for (CSVRecord record : records) {
			Heuristic heuristic = new Heuristic(record.get(1), Integer.parseInt(record.get(2)),
					Float.parseFloat(record.get(3)));

			Comment comment = commentsMap.get(Integer.parseInt(record.get(0)));
			if (comment == null) {
				continue;
			}

			comment.getHeuristics().add(heuristic);
		}
	}

	public void setCommentsCSV(String commentsCSV) {
		this.commentsCSV = commentsCSV;
	}

	public void setPatternsCSV(String patternsCSV) {
		this.patternsCSV = patternsCSV;
	}

	public void setHeuristicsCSV(String heuristicsCSV) {
		this.heuristicsCSV = heuristicsCSV;
	}

	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setRepository(String repositoryId) {
		RepositoryDocumentHandler repoHandler = new RepositoryDocumentHandler();
		this.repository = Repository
				.parseDocument(repoHandler.findById(repositoryId, Projections.include("scm", "path")));
	}

}