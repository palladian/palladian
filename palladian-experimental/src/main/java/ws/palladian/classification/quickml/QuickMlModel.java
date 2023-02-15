// package ws.palladian.classification.quickml;

// import java.util.Objects;
// import java.util.Set;

// import org.apache.commons.lang3.StringUtils;

// import quickml.supervised.classifier.Classifier;
// import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForest;
// import quickml.supervised.tree.decisionTree.DecisionTree;
// import quickml.supervised.tree.decisionTree.nodes.DTCatBranch;
// import quickml.supervised.tree.decisionTree.nodes.DTLeaf;
// import quickml.supervised.tree.decisionTree.nodes.DTNumBranch;
// import quickml.supervised.tree.decisionTree.valueCounters.ClassificationCounter;
// import quickml.supervised.tree.nodes.Branch;
// import quickml.supervised.tree.nodes.Node;
// import ws.palladian.core.Model;

// /**
//  * <p>
//  * Wrapper for QuickML's predictive models.
//  * </p>
//  * 
//  * @author Philipp Katz
//  */
// public class QuickMlModel implements Model {

// 	private static final class ToStringVistor implements TreeVisitor {
// 		final StringBuilder builder = new StringBuilder();
// 		int treeCount = 0;
// 		@Override
// 		public void tree(DecisionTree tree) {
// 			if (treeCount > 0) {
// 				builder.append('\n');
// 			}
// 			treeCount++;
// 			builder.append("Tree " + treeCount + ":\n");
// 		}
// 		@Override
// 		public void categoricalBranch(DTCatBranch branch, boolean trueChild) {
// 			appendBranch(branch, trueChild);
// 		}
// 		@Override
// 		public void numericalBranch(DTNumBranch branch, boolean trueChild) {
// 			appendBranch(branch, trueChild);
// 		}
// 		@Override
// 		public void leaf(DTLeaf leaf, boolean trueChild) {
// 			int depth = leaf.getDepth();
// 			String indent = getIndent(depth);
// 			builder.append(indent + String.valueOf(trueChild) + ": " + leaf + "\n");
// 		}
// 		private static String getIndent(int depth) {
// 			return StringUtils.repeat('\t', depth);
// 		}
// 		private void appendBranch(Branch<?> branch, boolean trueChild) {
// 			int depth = branch.getDepth();
// 			String indent = getIndent(depth);
// 			String childType = branch.getDepth() > 0 ? String.valueOf(trueChild) + ": " : "";
// 			builder.append(indent + childType + branch + "\n");
// 		}
// 		@Override
// 		public String toString() {
// 			return builder.toString();
// 		}
// 	}

//     private static final long serialVersionUID = 1L;

//     private final Classifier classifier;

//     private final Set<String> classes;

//     /** Package visibility, as it is to be instantiated by the QuickMlLearner only. */
//     QuickMlModel(Classifier classifier, Set<String> classes) {
//         this.classifier = classifier;
//         this.classes = classes;
//     }

//     public Classifier getClassifier() {
//         return classifier;
//     }

//     @Override
//     public Set<String> getCategories() {
//         return classes;
//     }

// 	@Override
// 	public String toString() {
// 		return traverseModel(new ToStringVistor()).toString();
// 	}

// 	/**
// 	 * Allows to traverse the model using a visitor.
// 	 * 
// 	 * @param visitor
// 	 *            The visitor.
// 	 * @return  The supplied visitor.
// 	 */
// 	public <TV extends TreeVisitor> TV traverseModel(TV visitor) {
// 		Objects.requireNonNull(visitor, "visitor must not be null");
// 		if (classifier instanceof RandomDecisionForest) {
// 			RandomDecisionForest forest = (RandomDecisionForest) classifier;
// 			for (DecisionTree tree : forest.decisionTrees) {
// 				traverseTree(tree, visitor);
// 			}
// 		} else if (classifier instanceof DecisionTree) {
// 			traverseTree((DecisionTree) classifier, visitor);
// 		} else {
// 			throw new IllegalStateException("Unsupported classifer type: " + classifier.getClass().getName());
// 		}
// 		return visitor;
// 	}

// 	private static void traverseTree(DecisionTree tree, TreeVisitor visitor) {
// 		visitor.tree(tree);
// 		traverseNode(tree.root, visitor, true);
// 	}

// 	private static void traverseNode(Node<ClassificationCounter> node, TreeVisitor visitor, boolean trueChild) {
// 		if (node instanceof DTCatBranch) {
// 			DTCatBranch catBranch = (DTCatBranch) node;
// 			visitor.categoricalBranch(catBranch, trueChild);
// 			traverseNode(catBranch.getTrueChild(), visitor, true);
// 			traverseNode(catBranch.getFalseChild(), visitor, false);
// 		} else if (node instanceof DTNumBranch) {
// 			DTNumBranch numBranch = (DTNumBranch) node;
// 			visitor.numericalBranch(numBranch, trueChild);
// 			traverseNode(numBranch.getTrueChild(), visitor, true);
// 			traverseNode(numBranch.getFalseChild(), visitor, false);
// 		} else if (node instanceof DTLeaf) {
// 			visitor.leaf((DTLeaf) node, trueChild);
// 		}
// 	}

// }
