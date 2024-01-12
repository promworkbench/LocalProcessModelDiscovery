package org.processmining.lpm.util;

import org.deckfour.xes.classification.XEventClasses;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;

// FIXME Check class contents
// FIXME UNTESTED CODE!
public class NAryTreeToProcessTree {

	/**
	 * Converts the given NAryTree to a ProcessTree without converting the leaf
	 * pointers to event class names
	 * 
	 * @param tree
	 *            NAryTree to convert
	 * @return ProcessTree the processTree equivalent
	 */
	public static ProcessTree convert(NAryTree tree) {
		return convert(tree, null);
	}

	/**
	 * Converts the given NAryTree to a ProcessTree while converting the leaf
	 * pointers to event class names
	 * 
	 * @param classes
	 *            XEventClasses list of event classes
	 * @param tree
	 *            NAryTree to convert
	 * @return ProcessTree the processTree equivalent
	 */
	public static ProcessTree convert(NAryTree tree, XEventClasses classes) {
		return convert(classes, tree, "");
	}

	/**
	 * Converts the given NAryTree to a ProcessTree while converting the leaf
	 * pointers to event class names
	 * 
	 * @param classes
	 *            XEventClasses list of event classes
	 * @param tree
	 *            NAryTree to convert
	 * @param name
	 *            The name of the ProcessTree
	 * @return ProcessTree the processTree equivalent
	 */
	public static ProcessTree convert(XEventClasses classes, NAryTree tree, String name) {
		ProcessTree pt = new ProcessTreeImpl(name);

		pt.setRoot(convertNode(pt, classes, tree, 0));

		return pt;
	}

	/**
	 * Converts the given NAryTree to a ProcessTree while converting the leaf
	 * pointers to event class names and including the provided configurations
	 * of the NAryTree
	 * 
	 * @param classes
	 *            XEventClasses list of event classes
	 * @param tree
	 *            NAryTree to convert
	 * @param name
	 *            The name of the ProcessTree
	 * @return Pair<ProcessTree, ArrayList<Configuration>> The ProcessTree and a
	 *         list of configurations to be applied on this tree
	 */
	public static ProcessTree convertWithoutConfiguration(XEventClasses classes,
			NAryTree tree, String name) {
		ProcessTree pt = new ProcessTreeImpl(name);

		//TRANSLATE!
		Node semiRoot = convertNode(pt, classes, tree, 0);

		pt.setRoot(semiRoot);

		return pt;
	}

	/**
	 * Convert a single node of the given NAryTree to a node in the ProcessTree
	 * 
	 * @param processTree
	 *            ProcessTree to add the converted node to
	 * @param tree
	 *            NAryTree to convert the node from
	 * @param node
	 *            int index of the node to convert (including subtree!)
	 * @return ProcessTree Node of the corresponding NAryTree node.
	 */
	public static Node convertNode(ProcessTree processTree, NAryTree tree, int node) {
		return convertNode(processTree, null, tree, node);
	}

	/**
	 * Convert a single node of the given NAryTree to a node in the ProcessTree,
	 * using the provided list of event classes for the translation
	 * 
	 * @param processTree
	 *            ProcessTree to add the converted node to
	 * @param classes
	 *            XEventClasses used to translate the NAryTree leaf pointers to
	 *            class names
	 * @param tree
	 *            NAryTree to convert the node from
	 * @param node
	 *            int index of the node to convert (including subtree!)
	 * @param configurations
	 *            An ArrayList of configurations to be filled with the correct
	 *            settings. This method is tolerant regarding the list, e.g. a
	 *            null instance will not break it and if the size if different
	 *            than the number of configurations for the NAryTree it will be
	 *            handled correctly.
	 * @return ProcessTree Node of the corresponding NAryTree node.
	 */
	public static Node convertNode(ProcessTree processTree, XEventClasses classes, NAryTree tree, int node) {
		return convertNode(processTree, null, classes, tree, node);
	}

	/**
	 * Convert a single node of the given NAryTree to a node in the ProcessTree,
	 * using the provided list of event classes for the translation
	 * 
	 * @param processTree
	 *            ProcessTree to add the converted node to
	 * @param parentNode
	 *            The node in the ProcessTree that is the parent of the subtree
	 *            to be translated. If NULL then no parent is assumed. The
	 *            parent reference is used to attach the child to the parent and
	 *            correctly set the configurations.
	 * @param classes
	 *            XEventClasses used to translate the NAryTree leaf pointers to
	 *            class names
	 * @param tree
	 *            NAryTree to convert the node from
	 * @param node
	 *            int index of the node to convert (including subtree!)
	 * @param configurations
	 *            An ArrayList of configurations to be filled with the correct
	 *            settings. This method is tolerant regarding the list, e.g. a
	 *            null instance will not break it and if the size if different
	 *            than the number of configurations for the NAryTree it will be
	 *            handled correctly.
	 * @return ProcessTree Node of the corresponding NAryTree node.
	 */
	public static Node convertNode(ProcessTree processTree, Block parentNode, XEventClasses classes, NAryTree tree,
			int node) {
		//Rewrite all REVSEQ operators
		Node newNode = null;

		boolean addPlaceholderRoot = false;

		/*
		 * We might need an extra node above since the root in the NAT might be
		 * blocked and because this is put on the egde for PTs we need an edge
		 * and hence a higher (placeholder) node
		 */
		//If we convert the root node and the pt does not have a root yet
		if (parentNode == null) {
			//First test if this is necessary...
			//Then add placeholder root or just the translated tree as root
			if (addPlaceholderRoot) {
				newNode = new AbstractBlock.PlaceHolder("ROOT");
			}
		}

		//Only create a 'real' new node if we did not add a placeholder node
		if (!addPlaceholderRoot) {
			short nodeType = tree.getType(node);

			switch (nodeType) {
				/*case NAryTree.REVSEQ :
					//We rewrote the tree to not have reversed sequences :(
					assert false;
					break;*/
				case NAryTree.SEQ :
					newNode = new AbstractBlock.Seq("SEQ (" + node + ")");
					break;
				case NAryTree.XOR :
					newNode = new AbstractBlock.Xor("XOR (" + node + ")");
					break;
				/*
				case NAryTree.OR :
					newNode = new AbstractBlock.Or("OR (" + node + ")");
					break;
				case NAryTree.ILV :
					//FIXME: Interleaving translated to AND
					newNode = new AbstractBlock.And("AND (" + node + ")");
					break;*/
				case NAryTree.AND :
					newNode = new AbstractBlock.And("AND (" + node + ")");
					break;
				case NAryTree.LOOP :
					newNode = new AbstractBlock.XorLoop("LOOP (" + node + ")");
					break;
				case NAryTree.TAU :
					newNode = new AbstractTask.Automatic("Tau");

					//Allow only 1 TAU instance...
					/*-
					for (Node existingNode : processTree.getNodes()) {
						if (existingNode instanceof AbstractTask.Automatic) {
							if (existingNode.getName().equals("Tau")) {
								return existingNode;
							}
						}
					}/**/

					break;
				case NAryTree.NONE :
					break;
				default :
					String taskName;

					if (classes == null) {
						taskName = "" + nodeType;
					} else {
						taskName = classes.getByIndex(nodeType).toString();
					}

					//Check if this activity is already present, if so, take that node and connect it!
					/*-
					for (Node existingNode : processTree.getNodes()) {
						if (existingNode.isLeaf()) {
							if (existingNode.getName().equals(taskName)) {
								return existingNode;
							}
						}
					}/**/

					//Otherwise, instantiate a new node and continue
					newNode = new AbstractTask.Manual(taskName);

					break;
			}
		}

		//The new node should be set
		assert (newNode != null);

		newNode.setProcessTree(processTree);
		processTree.addNode(newNode);

		if (parentNode != null) {
			//Create an edge to our parent block
			Edge e = parentNode.addChild(newNode);
			newNode.addIncomingEdge(e);
			processTree.addEdge(e);
		}

		/*
		 * Give blocks their children
		 */
		if (newNode instanceof Block) {

			//Cast the node safely to a block
			Block newBlock = (Block) newNode;

			if (addPlaceholderRoot) {
				//If we just added a placeholder node, we call ourselves but now with the placeholder node as the parent block
				convertNode(processTree, newBlock, classes, tree, node);
			} else {
				//Add the children!
				for (int c = 0; c < tree.nChildren(node); c++) {
					convertNode(processTree, newBlock, classes, tree,
							tree.getChildAtIndex(node, c));
				}
			}
		}

		return newNode;
	}
}
