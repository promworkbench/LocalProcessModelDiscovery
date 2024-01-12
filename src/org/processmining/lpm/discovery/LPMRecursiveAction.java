package org.processmining.lpm.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RecursiveAction;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelEvaluator;
import org.processmining.lpm.util.LocalProcessModelTopSet;
import org.processmining.lpm.util.ProcessTree2Petrinet;
import org.processmining.lpm.util.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.lpm.util.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.lpm.util.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.AbstractTask.Manual;
import org.processmining.processtree.impl.ProcessTreeImpl;

public class LPMRecursiveAction extends RecursiveAction{

	private static final long serialVersionUID = -1042982387393600631L;

	private enum OPERATOR {SEQ, XOR, XORLOOP, OR, AND, LOOP, ROOT};

	private static final String SEQ_REPRESENTATION = "!";
	private static final String XOR_REPRESENTATION = "@";
	private static final String XORLOOP_REPRESENTATION = "#";
	private static final String OR_REPRESENTATION = "$";
	private static final String AND_REPRESENTATION = "%";

	
	private PluginContext context;
	private LocalProcessModelParameters params;
	private ProcessTree pt;
	private int recursionDepth;
	private Set<Character> transitionNamesForNonDuplicateMode;
	private Set<Character> transitions;
	private Set<String> lpmTreeSet;
	private LocalProcessModelTopSet topSet;
	private Map<String, Integer> logActivityCountsMap;
	private Map<Character, Set<Character>> possibleNeighborsMap;
	private LocalProcessModelEvaluator scorer;
	
	public LPMRecursiveAction(PluginContext context, LocalProcessModelParameters params, Set<String> lpmTreeSet, ProcessTree pt, int recursionDepth, Set<Character> transitionNamesForNonDuplicateMode, Set<Character> transitions, LocalProcessModelTopSet topSet, Map<String, Integer> logActivityCountsMap, Map<Character, Set<Character>> possibleNeighborsMap,LocalProcessModelEvaluator scorer){
		this.context = context;
		this.params = params;
		this.lpmTreeSet = lpmTreeSet;
		this.pt = pt;
		this.recursionDepth = recursionDepth;
		this.transitionNamesForNonDuplicateMode = transitionNamesForNonDuplicateMode;
		this.transitions = transitions;
		this.topSet = topSet;
		this.logActivityCountsMap = logActivityCountsMap;
		this.possibleNeighborsMap = possibleNeighborsMap;
		this.scorer = scorer;
	}
	
	protected void compute() {
		List<LPMRecursiveAction> subtasks = new ArrayList<LPMRecursiveAction>();
		ProcessTree ptNew = pt;
		if(pt.size()>3)
			ptNew = normalizeTree(pt);
		String treeAsString = ptNew.toString();
		if(lpmTreeSet.contains(treeAsString))
			return;
		lpmTreeSet.add(treeAsString);
		int numExplored = params.getNumberOfExploredLpms().incrementAndGet();
		if((System.currentTimeMillis()-params.getLastNotificationTime())>params.getNotificationPeriod() && params.isVerbose()){
			params.setLastNotificationTime(System.currentTimeMillis());
			System.out.println(numExplored+" Petri nets explored");
			params.notifyNumExplored();
		}
		
		boolean supThreshMet = true;
		boolean detThresMet = true;
		boolean langRatThreshMet = true;
		boolean confThreshMet = true;
		
		if(ptNew.getNodes().size()>0){
			if(ptNew.getNodes().size()>1){
				LocalProcessModel scoredNet = null;
				PetrinetWithMarkings pwm = null;
				try {
					pwm = ProcessTree2Petrinet.convert(ptNew); // TODO: switch to new conversion plugin in PTConversion package, because old method is now deprecated
				} catch (NotYetImplementedException | InvalidProcessTreeException | NullPointerException e ) {
					e.printStackTrace();
				}
				if(pwm!=null){
					if(pwm.finalMarking.isEmpty())
						System.out.println("final marking is empty!!!");
					if(pwm.initialMarking.isEmpty())
						System.out.println("initial marking is empty!!!");
					AcceptingPetriNet apn = new AcceptingPetriNetImpl(pwm.petrinet, pwm.initialMarking, pwm.finalMarking);
					Map<String,Transition> nameToTransitionMap = new HashMap<String, Transition>();
					for(Transition t : apn.getNet().getTransitions())
						if(!t.isInvisible())
							nameToTransitionMap.put(t.getLabel(), t);
					scoredNet = scorer.evaluateNetOnLog(context, apn, logActivityCountsMap, false);
					supThreshMet = scoredNet.getFrequency() > params.getFrequencyMinimum();
					detThresMet = scoredNet.getDeterminism() > params.getDeterminismMinimum();
					langRatThreshMet = scoredNet.getLanguageFit() > params.getLanguageFitMinimum();
					confThreshMet = scoredNet.getConfidence() > params.getConfidenceMinimum();
					if(!supThreshMet || !detThresMet || !langRatThreshMet || !confThreshMet || ptNew.getNodes().size()<2){
						pwm = null;
						apn = null;
						nameToTransitionMap = null;
						scoredNet = null;
					}else{
						if(!topSet.add(scoredNet)){
							// not garbage collected automatically
							pwm = null;
							apn = null;
							nameToTransitionMap = null;
							scoredNet = null;
						} else {
							if(params.isStoreProcessTree())
								scoredNet.setProcessTree(new ProcessTreeImpl(ptNew));
						}
					}
				}
			}else{ // no alignment needed for initial LPMs, results already known
				supThreshMet = logActivityCountsMap.get(ptNew.getNodes().toArray(new Node[1])[0].getName())>params.getFrequencyMinimum();
				detThresMet = true;
				langRatThreshMet = true;
				confThreshMet = true;
			}
		}else{
			for(Character other : transitions){
				ProcessTree newpt = new ProcessTreeImpl(ptNew);
				AbstractTask.Manual manual = new AbstractTask.Manual(String.valueOf(other));
				newpt.addNode(manual);
				newpt.setRoot(manual);
				if(params.isDuplicateTransitions())
					subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newpt, recursionDepth+1, null, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
				else{
					Set<Character> newTransitionNames = new HashSet<Character>(transitions);
					newTransitionNames.remove(other);
					subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newpt, recursionDepth+1, newTransitionNames, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
				}
			}
			invokeAll(subtasks);
			return;
		}
		
		if(recursionDepth>=params.getNumTransitions() || (!params.isDuplicateTransitions() && transitionNamesForNonDuplicateMode.isEmpty()))
			return;
		else{
			// Identify Task node with highest depth in tree
			/*
			List<Manual> highestDepthNodes= new LinkedList<Manual>();
			int maxDepth = 0;
			for(Node node : ptNew.getNodes()){
				if(node instanceof AbstractTask.Manual){
					Manual manual = (Manual) node;
					Collection<Block> ps = node.getParents();
					int depth = 1;
					while(!ps.isEmpty()){
						Set<Block> psNew = new HashSet<Block>();
						for(Block p : ps)
							psNew.addAll(p.getParents());
						ps = psNew;
						depth++;
					}
					if(depth==maxDepth)
						highestDepthNodes.add(manual);	
					if(depth>maxDepth){
						highestDepthNodes.clear();
						highestDepthNodes.add(manual);
						maxDepth = depth;
					}					
				}
			}
			*/
			// Loop over highestDepthNodes for modification
			for(Node node : ptNew.getNodes()){
				if(!(node instanceof Manual))
					continue;
				Manual manual = (Manual) node;
			//for(Manual manual : ptNew.getN){
				//OPERATOR parentType = OPERATOR.ROOT;
				boolean firstChild = false;
				boolean hasLoopAncestor = false;
				boolean hasSeqAncestor = false;
				boolean hasOptionalAncestor = false;
				boolean hasCommutativeParent = false;
				boolean hasCommutativeSibling = false;
				if(!manual.getParents().isEmpty()){
					Block parent = manual.getParents().iterator().next(); // always one parent (we have a tree, not a graph)
					if(parent instanceof AbstractBlock.Seq){
						//parentType = OPERATOR.SEQ;
						hasSeqAncestor = true;
					}
					if(parent instanceof AbstractBlock.And){
						hasCommutativeParent = true;
						//parentType = OPERATOR.AND;
					}if(parent instanceof AbstractBlock.Xor){
						//parentType = OPERATOR.XOR;
						hasCommutativeParent = true;
						hasOptionalAncestor = true;
					}
					if(parent instanceof AbstractBlock.XorLoop){
						//parentType = OPERATOR.XORLOOP;
						hasLoopAncestor = true;
					}
					if(parent instanceof AbstractBlock.Or){
						//parentType = OPERATOR.OR;
						hasCommutativeParent = true;
						hasOptionalAncestor = true;
					}			
					if(parent.getChildren().size()>1){
						firstChild = parent.getChildren().get(0)==manual;
						for(int i=0; i<parent.getChildren().size(); i++){
							Node childOfParent = parent.getChildren().get(i);
							if(childOfParent!=manual) // test if self or sibling
								if(childOfParent instanceof AbstractBlock.And || childOfParent instanceof AbstractBlock.Xor || childOfParent instanceof AbstractBlock.Or)
									hasCommutativeSibling = true;
						}
					}else{
						firstChild = true;
						hasCommutativeSibling = false;
					}
					while(!parent.getParents().isEmpty() && (!hasLoopAncestor || !hasSeqAncestor || !hasOptionalAncestor)){
						// traverse to root
						parent = parent.getParents().iterator().next();
						if(parent instanceof AbstractBlock.Xor || parent instanceof AbstractBlock.Or)
							hasOptionalAncestor = true;
						if(parent instanceof AbstractBlock.Seq)
							hasSeqAncestor = true;
						if(parent instanceof AbstractBlock.XorLoop)
							hasLoopAncestor = true;
					}
				}
					
				UUID id = manual.getID();
				boolean isRoot = ptNew.getRoot().equals(manual);
				Character name = manual.getName().charAt(0);
				Set<Character> usedTransitions = new HashSet<Character>();
				if(params.isDuplicateTransitions())
					usedTransitions.addAll(transitions);
				else
					usedTransitions.addAll(transitionNamesForNonDuplicateMode);
				for(Character other : usedTransitions){
					Set<Character> newTransitionNames = new HashSet<Character>();
					if(!params.isDuplicateTransitions()){
						newTransitionNames.addAll(transitionNamesForNonDuplicateMode);
						newTransitionNames.remove(other);
					}
					operatorLoop:
					for(OPERATOR o : EnumSet.of(OPERATOR.XOR, OPERATOR.OR)){
						switch(o){
							case XOR:
								if(!params.isUseXor() || isRoot || !detThresMet ) // XOR expansions cannot increase determinism, prune if not met 
									continue operatorLoop;
								if(!hasCommutativeParent && !firstChild && !hasCommutativeSibling) // exp_1 restriction for commutative operators
									continue operatorLoop;
								ProcessTree newPt = new ProcessTreeImpl(ptNew);
								Manual newManual = (Manual) newPt.getNode(id);
								List<Edge> incoming = newManual.getIncomingEdges();
								newPt.removeNode(manual);
								Block newBlockNode = new AbstractBlock.Xor(XOR_REPRESENTATION, incoming, new LinkedList<Edge>());
								// test alphabetical order between name and other to decide order
								Character first = null;
								Character second = null;
								if(name.compareTo(other)>0){
									first = name;
									second = other;
								}else{
									continue operatorLoop;
									//first = other;
									//second = name;
								}
								newPt.addNode(newBlockNode);
								Manual newTaskNode1 = new AbstractTask.Manual(String.valueOf(first));
								newPt.addNode(newTaskNode1);
								newBlockNode.addChild(newTaskNode1);
								Manual newTaskNode2 = new AbstractTask.Manual(String.valueOf(second));
								for(Edge edge : incoming)
									edge.setTarget(newBlockNode);
								newPt.addNode(newTaskNode2);
								newBlockNode.addChild(newTaskNode2);
								if(isRoot)
									newPt.setRoot(newBlockNode);
								if(params.isDuplicateTransitions())
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								else
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								break;
							case OR:
								if(!params.isUseOr() || isRoot || !detThresMet ) // OR expansions cannot increase determinism, prune if not met 
									continue operatorLoop;
								if(!hasCommutativeParent && !firstChild && !hasCommutativeSibling) // exp_1 restriction for commutative operators
									continue operatorLoop;
								newPt = new ProcessTreeImpl(ptNew);
								newManual = (Manual) newPt.getNode(id);
								incoming = newManual.getIncomingEdges();
								newPt.removeNode(manual);
								newBlockNode = new AbstractBlock.Or(OR_REPRESENTATION, incoming, new LinkedList<Edge>());
								// test alphabetical order between name and other to decide order
								Character first2 = null;
								Character second2 = null;
								if(name.compareTo(other)>0){
									first2 = name;
									second2 = other;
								}else{
									continue operatorLoop;
									//first2 = other;
									//second2 = name;
								}
								newPt.addNode(newBlockNode);
								newTaskNode1 = new AbstractTask.Manual(String.valueOf(first2));
								newPt.addNode(newTaskNode1);
								newBlockNode.addChild(newTaskNode1);
								newTaskNode2 = new AbstractTask.Manual(String.valueOf(second2));
								for(Edge edge : incoming)
									edge.setTarget(newBlockNode);
								newPt.addNode(newTaskNode2);
								newBlockNode.addChild(newTaskNode2);
								if(isRoot)
									newPt.setRoot(newBlockNode);
								if(params.isDuplicateTransitions())
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								else
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								break;
						}
					}
				}
				
				if(possibleNeighborsMap.containsKey(name))
					usedTransitions.retainAll(possibleNeighborsMap.get(name));
				else
					usedTransitions.clear();
				for(Character other : usedTransitions){
					Set<Character> newTransitionNames = new HashSet<Character>();
					if(!params.isDuplicateTransitions()){
						newTransitionNames.addAll(transitionNamesForNonDuplicateMode);
						newTransitionNames.remove(other);
					}
					operatorLoop:
					for(OPERATOR o : EnumSet.of(OPERATOR.AND, OPERATOR.SEQ)){
						switch(o){
							case SEQ:
								if(!params.isUseSeq() || !supThreshMet )
									continue operatorLoop;
								if(!hasOptionalAncestor){ // if new activity occurs less than threshold and node for expansion has no choice ancestor, support cannot be met => prune
									if(logActivityCountsMap.containsKey(String.valueOf(other))){
										int val = logActivityCountsMap.get(String.valueOf(other));
										if(val<params.getFrequencyMinimum()){
											int i = 0;
											continue operatorLoop;
										}
									}else{
										int i = 0;
										continue operatorLoop;
									}
								}
								//if(name.compareTo(other)<=0)
								//	continue;
								ProcessTree newPt = new ProcessTreeImpl(ptNew);
								Manual newManual = (Manual) newPt.getNode(id);
								List<Edge> incoming = newManual.getIncomingEdges();
								newPt.removeNode(newManual);
								Block newBlockNode = new AbstractBlock.Seq(SEQ_REPRESENTATION, incoming, new LinkedList<Edge>());
								newPt.addNode(newBlockNode);
								for(Edge edge : incoming)
									edge.setTarget(newBlockNode);
								Manual newTaskNode1 = new AbstractTask.Manual(String.valueOf(name));
								newPt.addNode(newTaskNode1);
								newBlockNode.addChild(newTaskNode1);
								Manual newTaskNode2 = new AbstractTask.Manual(String.valueOf(other));
								newPt.addNode(newTaskNode2);
								newBlockNode.addChild(newTaskNode2);
								if(isRoot)
									newPt.setRoot(newBlockNode);
								if(params.isDuplicateTransitions())
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								else
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								break;
							case AND:
								if(!params.isUseAnd() || !supThreshMet || !detThresMet ) // Pruning: AND expansions cannot increase support, determinism.
									continue operatorLoop;
								if(!hasCommutativeParent && !firstChild && !hasCommutativeSibling) // exp_1 restriction for commutative operators
									continue operatorLoop;
								if(!hasOptionalAncestor){ // if new activity occurs less than threshold and node for expansion has no choice ancestor, support cannot be met => prune
									if(logActivityCountsMap.containsKey(String.valueOf(other))){
										int val = logActivityCountsMap.get(String.valueOf(other));
										if(val<params.getFrequencyMinimum()){
											int i = 0;
											continue operatorLoop;
										}
									}else{
										int i = 0;
										continue operatorLoop;
									}
								}
								newPt = new ProcessTreeImpl(ptNew);
								newManual = (Manual) newPt.getNode(id);
								incoming = newManual.getIncomingEdges();
								newPt.removeNode(manual);
								newBlockNode = new AbstractBlock.And(AND_REPRESENTATION, incoming, new LinkedList<Edge>());
								// test alphabetical order between name and other to decide order
								Character first3 = null;
								Character second3 = null;
								if(name.compareTo(other)>0){
									first3 = name;
									second3 = other;
								}else{
									continue operatorLoop;
									//first3 = other;
									//second3 = name;
								}
								newPt.addNode(newBlockNode);
								newTaskNode1 = new AbstractTask.Manual(String.valueOf(first3));
								newPt.addNode(newTaskNode1);
								newBlockNode.addChild(newTaskNode1);
								newTaskNode2 = new AbstractTask.Manual(String.valueOf(second3));
								for(Edge edge : incoming)
									edge.setTarget(newBlockNode);
								newPt.addNode(newTaskNode2);
								newBlockNode.addChild(newTaskNode2);
								if(isRoot)
									newPt.setRoot(newBlockNode);
								if(params.isDuplicateTransitions())
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								else
									subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
								break;
						}
					}
				}
				// XORLOOP
				if(!params.isUseXorloop() || !supThreshMet || !detThresMet || !langRatThreshMet || isRoot || hasLoopAncestor || !hasSeqAncestor) // Pruning: XORLOOP expansions cannot increase support, language ratio, and determinism.
					continue;
				
				ProcessTree newPt = new ProcessTreeImpl(ptNew);
				Manual newManual = (Manual) newPt.getNode(id);
				List<Edge> incoming = newManual.getIncomingEdges();
				newPt.removeNode(manual);
				AbstractBlock.XorLoop newBlockNode = new AbstractBlock.XorLoop(XORLOOP_REPRESENTATION, incoming, new LinkedList<Edge>());
				newPt.addNode(newBlockNode);
				AbstractTask.Manual newTaskNode1 = new AbstractTask.Manual(String.valueOf(name));
				newPt.addNode(newTaskNode1);
				newBlockNode.addChild(newTaskNode1);
				AbstractTask.Automatic newTaskNode2 = new AbstractTask.Automatic("t1");
				newPt.addNode(newTaskNode2);
				newBlockNode.addChild(newTaskNode2);
				AbstractTask.Automatic newTaskNode3 = new AbstractTask.Automatic("t2");
				newPt.addNode(newTaskNode3);
				newBlockNode.addChild(newTaskNode3);
				for(Edge edge : incoming)
					edge.setTarget(newBlockNode);
				if(isRoot)
					newPt.setRoot(newBlockNode);
				if(params.isDuplicateTransitions())
					subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
				else
					subtasks.add(new LPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, transitionNamesForNonDuplicateMode, transitions, topSet, logActivityCountsMap, possibleNeighborsMap, scorer));
			}
			invokeAll(subtasks);
		}
	}
	
	
	public ProcessTree normalizeTree(ProcessTree pt){
		ProcessTree newPt = new ProcessTreeImpl(pt);
		boolean madeChange = true;
		int i=1;
		while(madeChange){ // replace by bottom-up run through tree
			i++;
			madeChange = false;
			for(Node node : newPt.getNodes()){
				if(node instanceof Block){
					boolean isUnorderedBlock = (node instanceof AbstractBlock.And || node instanceof AbstractBlock.Xor || node instanceof AbstractBlock.Or);
					if(isUnorderedBlock){
						Block blockNode = (Block) node;
						List<Edge> children = new LinkedList<Edge>(blockNode.getOutgoingEdges());
						Collections.sort(children, new EdgeComparator());
						if(!children.equals(blockNode.getOutgoingEdges()))
							madeChange = true;
						List<Node> nodes = new LinkedList<Node>();
						for(Edge edge : children)
							nodes.add(edge.getTarget());
						int j = 0;
						for(Node node2 : nodes){
							blockNode.swapChildAt(node2, j);
							j++;
						}
					}
				}
			}
		}
		return newPt;
	}
	
	private class EdgeComparator implements Comparator<Edge> {
		public int compare(Edge one, Edge two) {
			NodeComparator comparator = new NodeComparator();
			return comparator.compare(one.getTarget(), two.getTarget());
		}
	}
	
	private class NodeComparator implements Comparator<Node> {
		public int compare(Node one, Node two) {
			boolean oneUnorderedBlock = (one instanceof AbstractBlock.And || one instanceof AbstractBlock.Xor || one instanceof AbstractBlock.Or);
			boolean twoUnorderedBlock = (two instanceof AbstractBlock.And || two instanceof AbstractBlock.Xor || two instanceof AbstractBlock.Or);
			//if(!oneUnorderedBlock || !twoUnorderedBlock)
			//	return oneUnorderedBlock ? 1 : -1;
			if(oneUnorderedBlock && !twoUnorderedBlock)
				return -1;
			if(!oneUnorderedBlock && twoUnorderedBlock)
				return 1;
			int compareResult = one.getName().compareTo(two.getName());
			if(!oneUnorderedBlock && !twoUnorderedBlock)
				return compareResult;
			if(oneUnorderedBlock && twoUnorderedBlock && (compareResult!=0))
				return compareResult;
			return new EdgesComparator().compare(((Block)one).getOutgoingEdges(), ((Block)two).getOutgoingEdges());
		}
	}
	
	private class EdgesComparator implements Comparator<List<Edge>> {
		public int compare(List<Edge> one, List<Edge> two) {
			int max = Math.min(one.size(), two.size());
			for(int i=0; i<max; i++){
				EdgeComparator comparator = new EdgeComparator();
				int result = comparator.compare(one.get(i), two.get(i));
				if(result!=0)
					return result;
			}
			return Integer.compare(one.size(), two.size());
		}
	}
}
