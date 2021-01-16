// package ws.palladian.classification.quickml;

// import quickml.supervised.tree.decisionTree.DecisionTree;
// import quickml.supervised.tree.decisionTree.nodes.DTCatBranch;
// import quickml.supervised.tree.decisionTree.nodes.DTLeaf;
// import quickml.supervised.tree.decisionTree.nodes.DTNumBranch;

// /**
//  * Visitor for decision tree and random forest models. The given callback
//  * methods are invoked when traversing the tree, starting at each tree's root
//  * node, in depth first order.
//  * 
//  * @author pk
//  */
// public interface TreeVisitor {
// 	/**
// 	 * @param tree The current tree.
// 	 */
// 	void tree(DecisionTree tree);
// 	/**
// 	 * @param branch The current categorical branch.
// 	 * @param trueChild Whether this branch is the parent's true or false decision.
// 	 */
// 	void categoricalBranch(DTCatBranch branch, boolean trueChild);
// 	/**
// 	 * @param branch The current numerical branch.
// 	 * @param trueChild Whether this branch is the parent's true or false decision.
// 	 */
// 	void numericalBranch(DTNumBranch branch, boolean trueChild);
// 	/**
// 	 * @param leaf The current leaf.
// 	 * @param trueChild Whether this branch is the parent's true or false decision.
// 	 */
// 	void leaf(DTLeaf leaf, boolean trueChild);
// }
