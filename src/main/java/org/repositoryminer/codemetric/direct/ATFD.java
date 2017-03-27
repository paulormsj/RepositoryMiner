package org.repositoryminer.codemetric.direct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.repositoryminer.ast.AST;
import org.repositoryminer.ast.AbstractClassDeclaration;
import org.repositoryminer.ast.MethodDeclaration;
import org.repositoryminer.ast.Statement;
import org.repositoryminer.ast.Statement.NodeType;
import org.repositoryminer.codemetric.CodeMetricId;

/**
 * <h1>Access To Foreign Data</h1>
 * <p>
 * ATFD is defined as the number of attributes from unrelated classes that are
 * accessed directly or by invoking accessor methods.
 */
public class ATFD implements IDirectCodeMetric {

	private List<Document> methodsDoc = new ArrayList<Document>();

	@Override
	public CodeMetricId getId() {
		return CodeMetricId.ATFD;
	}

	@Override
	public Document calculate(AbstractClassDeclaration type, AST ast) {
		methodsDoc.clear();
		return new Document("metric", CodeMetricId.ATFD.toString())
				.append("accumulated", calculate(type, type.getMethods(), true)).append("methods", methodsDoc);
	}

	public int calculate(AbstractClassDeclaration type, List<MethodDeclaration> methods, boolean calculateByMethod) {
		int atfdClass = 0;

		for (MethodDeclaration mDeclaration : methods) {
			int atfdMethod = countForeignAccessedFields(type, mDeclaration);

			atfdClass += atfdMethod;
			if (calculateByMethod) {
				methodsDoc.add(new Document("method", mDeclaration.getName()).append("value", atfdMethod));
			}
		}

		return atfdClass;
	}

	private int countForeignAccessedFields(AbstractClassDeclaration currType, MethodDeclaration method) {
		Set<String> accessedFields = new HashSet<String>();

		for (Statement stmt : method.getStatements()) {
			String exp, type;

			if (stmt.getNodeType() == NodeType.FIELD_ACCESS || stmt.getNodeType() == NodeType.METHOD_INVOCATION) {
				exp = stmt.getExpression();
				type = exp.substring(0, exp.lastIndexOf("."));
			} else {
				continue;
			}

			if (stmt.getNodeType().equals(NodeType.FIELD_ACCESS)) {
				if (!currType.getName().equals(type))
					accessedFields.add(exp.toLowerCase());
			} else if (stmt.getNodeType().equals(NodeType.METHOD_INVOCATION)) {
				String methodInv = exp.substring(exp.lastIndexOf(".") + 1);
				if (!currType.getName().equals(type)) {
					if ((methodInv.startsWith("get") || methodInv.startsWith("set")) && methodInv.length() > 3) {
						accessedFields.add((type + "." + methodInv.substring(3)).toLowerCase());
					} else if (methodInv.startsWith("is") && methodInv.length() > 2)
						accessedFields.add((type + "." + methodInv.substring(2)).toLowerCase());
				}
			}
		}

		return accessedFields.size();
	}

}