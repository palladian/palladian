package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * <p>A simple tree implementation. Identification of the nodes works via the labels. No tree node must have a label of
 * another tree node.</p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * 
 */
public class TreeNode implements Serializable {

    private static final long serialVersionUID = 2905428524425124865L;

    private String label = "rootNode";
    private HashMap<String, TreeNode> children = null;
    private Object value = null;
    private double weight = 0.0;
    private TreeNode parent = null;

    public TreeNode(String label) {
        this.label = label;
    }

    public TreeNode(String label, HashMap<String, TreeNode> children, TreeNode parent) {
        this.label = label;
        this.children = children;
        this.parent = parent;
    }

    /**
     * Add a node as a child to the tree node.
     * 
     * @param tn The tree node to add.
     * @return True, if the node was not present, false otherwise.
     */
    public boolean addNode(TreeNode tn) {
        if (children == null) {
            children = new HashMap<String, TreeNode>();
        }

        tn.setParent(this);
        TreeNode previousValue = children.put(tn.getLabel(), tn);
        if (previousValue != null) {
            return false;
        }
        return true;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Get the node with the specified label that is somewhere below this node.
     * 
     * @param label The label to search for.
     * @return The sought TreeNode or null if it was not found.
     */
    public TreeNode getNode(String label) {

        Set<TreeNode> nodeList = getDescendants();
        for (TreeNode node : nodeList) {
            if (node.getLabel().equalsIgnoreCase(label)) {
                return node;
            }
        }

        return null;
    }

    public Map<String, TreeNode> getChildren() {
        return children;
    }

    public void setChildren(HashMap<String, TreeNode> children) {
        this.children = children;
    }

    public Set<TreeNode> getDescendants() {
        HashSet<TreeNode> nodeList = new HashSet<TreeNode>();
        nodeList.add(this);
        if (children == null) {
            return nodeList;
        }

        for (Entry<String, TreeNode> entry : children.entrySet()) {
            nodeList.addAll(entry.getValue().getDescendants());
        }

        return nodeList;
    }

    /**
     * Set all weights of the descendant nodes to 0.0.
     */
    public void resetWeights() {
        Set<TreeNode> list = getDescendants();
        for (TreeNode tn : list) {
            tn.setWeight(0.0);
        }
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    /**
     * Get all parent nodes until the root node is reached.
     * 
     * @return An ordered list of parent nodes, ending with the root node.
     */
    public List<TreeNode> getRootPath() {
        List<TreeNode> list = new ArrayList<TreeNode>();

        TreeNode node = this;
        while (node != null) {
            list.add(node);
            node = node.getParent();
        }

        return list;
    }

    /**
     * Get all child nodes until the leaf node is reached. Follow the path of the highest weights.
     * 
     * @return An ordered list of child nodes, ending with the leaf node.
     */
    public List<TreeNode> getLeafPath() {
        ArrayList<TreeNode> list = new ArrayList<TreeNode>();

        list.add(this);
        if (children == null) {
            return list;
        }

        TreeNode highestWeightedChild = null;
        for (Entry<String, TreeNode> entry : children.entrySet()) {
            if (highestWeightedChild == null) {
                if (entry.getValue().getWeight() > 0) {
                    highestWeightedChild = entry.getValue();
                }
                continue;
            }
            if (entry.getValue().getWeight() > highestWeightedChild.getWeight()) {
                highestWeightedChild = entry.getValue();
            }
        }

        if (highestWeightedChild != null) {
            list.addAll(highestWeightedChild.getLeafPath());
        }

        return list;
    }

    /**
     * Get an ordered list of all nodes before and after this node. Follow the children that have the highest weight.
     * 
     * @return An ordered list of nodes from the leaf to the root.
     */
    public List<TreeNode> getFullPath() {
        List<TreeNode> list = getLeafPath();
        list.remove(0);
        Collections.reverse(list);
        list.addAll(getRootPath());
        Collections.reverse(list);
        return list;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        int children = 0;
        if (getChildren() != null) {
            children = getChildren().size();
        }
        String parentLabel = "unknown";
        if (getParent() != null) {
            parentLabel = getParent().getLabel();
        }
        return getLabel() + " (" + children + " children, parent: " + parentLabel + ")";
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        TreeNode arts = new TreeNode("arts");
        arts.setWeight(0.3);
        TreeNode movie = new TreeNode("movie");
        movie.setWeight(0.7);
        TreeNode music = new TreeNode("music");
        music.setWeight(0.1);
        TreeNode action = new TreeNode("action");
        action.setWeight(0.5);
        TreeNode thriller = new TreeNode("thriller");
        thriller.setWeight(0.2);
        TreeNode folk = new TreeNode("folk");
        folk.setWeight(0.1);

        arts.addNode(movie);
        arts.addNode(music);
        movie.addNode(action);
        movie.addNode(thriller);
        music.addNode(folk);

        TreeNode tn = arts.getNode("movie");
        System.out.println(tn + "\n");
        CollectionHelper.print(tn.getRootPath());
        CollectionHelper.print(tn.getLeafPath());
        CollectionHelper.print(tn.getFullPath());
    }

}