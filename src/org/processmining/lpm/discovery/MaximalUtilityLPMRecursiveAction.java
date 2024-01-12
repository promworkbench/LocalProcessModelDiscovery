package org.processmining.lpm.discovery;

import java.util.ArrayList;
import java.util.Collection;
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
import java.util.concurrent.atomic.AtomicInteger;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters;
import org.processmining.lpm.postprocess.FilterBasedOnStrategyDiscoverability;
import org.processmining.lpm.util.UtilityAlignmentAcceptingPetriNetScorer;
import org.processmining.lpm.util.UtilityAlignmentScoredAcceptingPetrinetContainer;
import org.processmining.lpm.util.UtilityLocalProcessModel;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.AbstractTask.Manual;
import org.processmining.processtree.impl.ProcessTreeImpl;

public class MaximalUtilityLPMRecursiveAction extends RecursiveAction{

	private static final long serialVersionUID = -1042982387393600631L;

	private enum OPERATOR {SEQ, XOR, XORLOOP, OR, AND, LOOP, ROOT};

	private PluginContext context;
	private UtilityLocalProcessModelParameters params;
	private ProcessTree pt;
	private int recursionDepth;
	private Set<Character> transitionNamesForNonDuplicateMode;
	private Set<Character> transitions;
	private Set<String> lpmTreeSet;
	private AtomicInteger petriNetsExplored;
	private UtilityAlignmentScoredAcceptingPetrinetContainer topSet;
	private Map<String, Integer> logActivityCountsMap;
	private UtilityLocalProcessModel previousLPM;

	public MaximalUtilityLPMRecursiveAction(PluginContext context, UtilityLocalProcessModelParameters params, Set<String> lpmTreeSet, ProcessTree pt, int recursionDepth, Set<Character> transitionNamesForNonDuplicateMode, Set<Character> transitions, AtomicInteger petriNetsExplored, UtilityAlignmentScoredAcceptingPetrinetContainer topSet, Map<String, Integer> logActivityCountsMap, UtilityLocalProcessModel previousLPM){
		this.context = context;
		this.params = params;
		this.lpmTreeSet = lpmTreeSet;
		this.pt = pt;
		this.recursionDepth = recursionDepth;
		this.transitionNamesForNonDuplicateMode = transitionNamesForNonDuplicateMode;
		this.transitions = transitions;
		this.petriNetsExplored = petriNetsExplored;
		this.topSet = topSet;
		this.logActivityCountsMap = logActivityCountsMap;
		this.previousLPM = previousLPM;
	}
	protected void compute() {
		List<MaximalUtilityLPMRecursiveAction> subtasks = new ArrayList<MaximalUtilityLPMRecursiveAction>();
		ProcessTree ptNew = normalizeTree(pt);
		String treeAsString = ptNew.toString();
		if(lpmTreeSet.contains(treeAsString))
			return;
		lpmTreeSet.add(treeAsString);
		petriNetsExplored.incrementAndGet();
		if(petriNetsExplored.intValue()%1000==0){
			System.out.println(petriNetsExplored+" Petri nets explored");
			params.setNumberOfExploredLpms(petriNetsExplored);
		}

		boolean frequencyMinimumMet = true;
		boolean avgEnabledTransMinimumMet = true;
		boolean languageRatioMinimumMet = true;
		boolean confidenceMinimumMet = true;
		boolean utilityMinimumMet = true;
		UtilityLocalProcessModel previousLPM = this.previousLPM;

		if(ptNew.getNodes().size()>0){
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
				UtilityLocalProcessModel thisLPM = UtilityAlignmentAcceptingPetriNetScorer.evaluateNetOnLog(context, apn, logActivityCountsMap, false, params, false);
				
				frequencyMinimumMet = thisLPM.getFrequency() > params.getFrequencyMinimum();
				avgEnabledTransMinimumMet = thisLPM.getDeterminism() > params.getDeterminismMinimum();
				languageRatioMinimumMet = thisLPM.getLanguageFit() > params.getLanguageFitMinimum();
				confidenceMinimumMet = thisLPM.getConfidence() > params.getConfidenceMinimum();
				utilityMinimumMet = thisLPM.getUtility() > params.getUtilityMinimum();

				if(previousLPM!=null){
					thisLPM.copyUtilityList(previousLPM.getUtilityList());
					thisLPM.addToUtilityList(previousLPM.getUtility());
				}
				if(!FilterBasedOnStrategyDiscoverability.expandModel(thisLPM, params.getPruningStrategy(), params.getPruningK()))
					return;
				
				if(!frequencyMinimumMet || !avgEnabledTransMinimumMet || !languageRatioMinimumMet || !confidenceMinimumMet || !utilityMinimumMet){
					pwm = null;
					apn = null;
					nameToTransitionMap = null;
					//previousLPM = null;
				}else{
					topSet.remove(previousLPM);
					if(!topSet.add(thisLPM)){
						// not garbage collected automatically
						pwm = null;
						apn = null;
						nameToTransitionMap = null;
						//previousLPM = null;
					}
					previousLPM = thisLPM;
	
				}

			}
		}else{
			for(Character other : transitions){
				ProcessTree newpt = new ProcessTreeImpl(ptNew);
				AbstractTask.Manual manual = new AbstractTask.Manual(""+other);
				newpt.addNode(manual);
				newpt.setRoot(manual);
				if(params.isDuplicateTransitions())
					subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newpt, recursionDepth+1, null, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
				else{
					Set<Character> newTransitionNames = new HashSet<Character>(transitions);
					newTransitionNames.remove(other);
					subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newpt, recursionDepth+1, newTransitionNames, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
				}
			}
			invokeAll(subtasks);
			return;
		}

		if(recursionDepth>=params.getNumTransitions() || (!params.isDuplicateTransitions() && transitionNamesForNonDuplicateMode.isEmpty()))
			return;
		else{
			// Identify Task node with highest depth in tree
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
					if(depth>maxDepth){
						highestDepthNodes.clear();
						highestDepthNodes.add(manual);
						maxDepth = depth;
					}
					if(depth==maxDepth)
						highestDepthNodes.add(manual);						
				}
			}
			// Loop over highestDepthNodes for modification
			for(Manual manual : highestDepthNodes){
				OPERATOR parentType = OPERATOR.ROOT;
				boolean firstChild = false;
				boolean hasLoopAncestor = false;
				boolean hasSeqAncestor = false;
				if(!manual.getParents().isEmpty()){
					Block parent = manual.getParents().iterator().next(); // always one parent (we have a tree, not a graph)
					if(parent instanceof AbstractBlock.Seq){
						parentType = OPERATOR.SEQ;
						hasSeqAncestor = true;
					}
					if(parent instanceof AbstractBlock.And)
						parentType = OPERATOR.AND;
					if(parent instanceof AbstractBlock.Xor)
						parentType = OPERATOR.XOR;
					if(parent instanceof AbstractBlock.XorLoop){
						parentType = OPERATOR.XORLOOP;
						hasLoopAncestor = true;
					}
					if(parent instanceof AbstractBlock.Or)
						parentType = OPERATOR.OR;
					firstChild = parent.getChildren().get(0)==manual;
					while(!parent.getParents().isEmpty() && !hasLoopAncestor){
						// traverse to root
						parent = parent.getParents().iterator().next();
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
						for(OPERATOR o : EnumSet.of(OPERATOR.XOR, OPERATOR.OR, OPERATOR.AND, OPERATOR.SEQ)){
							switch(o){
								case SEQ:
									if(!params.isUseSeq() || !languageRatioMinimumMet || !frequencyMinimumMet || !confidenceMinimumMet || (parentType==OPERATOR.SEQ && !firstChild))
										continue operatorLoop;
									ProcessTree newPt = new ProcessTreeImpl(ptNew);
									Manual newManual = (Manual) newPt.getNode(id);
									List<Edge> incoming = newManual.getIncomingEdges();
									newPt.removeNode(newManual);
									Block newBlockNode = new AbstractBlock.Seq("seq", incoming, new LinkedList<Edge>());
									newPt.addNode(newBlockNode);
									for(Edge edge : incoming)
										edge.setTarget(newBlockNode);
									Manual newTaskNode1 = new AbstractTask.Manual(""+name);
									newPt.addNode(newTaskNode1);
									newBlockNode.addChild(newTaskNode1);
									Manual newTaskNode2 = new AbstractTask.Manual(""+other);
									newPt.addNode(newTaskNode2);
									newBlockNode.addChild(newTaskNode2);
									if(isRoot)
										newPt.setRoot(newBlockNode);
									if(params.isDuplicateTransitions())
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									else
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									break;
								case XOR:
									if(!params.isUseXor() || isRoot || !avgEnabledTransMinimumMet || (parentType==OPERATOR.XOR && !firstChild))
										continue operatorLoop;
									newPt = new ProcessTreeImpl(ptNew);
									newManual = (Manual) newPt.getNode(id);
									incoming = newManual.getIncomingEdges();
									newPt.removeNode(manual);
									newBlockNode = new AbstractBlock.Xor("xor", incoming, new LinkedList<Edge>());
									// test alphabetical order between name and other to decide order
									Character first = null;
									Character second = null;
									if(name.compareTo(other)>0){
										first = name;
										second = other;
									}else{
										first = other;
										second = name;
									}
									newPt.addNode(newBlockNode);
									newTaskNode1 = new AbstractTask.Manual(""+first);
									newPt.addNode(newTaskNode1);
									newBlockNode.addChild(newTaskNode1);
									newTaskNode2 = new AbstractTask.Manual(""+second);
									for(Edge edge : incoming)
										edge.setTarget(newBlockNode);
									newPt.addNode(newTaskNode2);
									newBlockNode.addChild(newTaskNode2);
									if(isRoot)
										newPt.setRoot(newBlockNode);
									if(params.isDuplicateTransitions())
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									else
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									break;
								case OR:
									if(!params.isUseOr() || isRoot || !avgEnabledTransMinimumMet || (parentType==OPERATOR.OR && !firstChild))
										continue operatorLoop;
									newPt = new ProcessTreeImpl(ptNew);
									newManual = (Manual) newPt.getNode(id);
									incoming = newManual.getIncomingEdges();
									newPt.removeNode(manual);
									newBlockNode = new AbstractBlock.Or("or", incoming, new LinkedList<Edge>());
									// test alphabetical order between name and other to decide order
									Character first2 = null;
									Character second2 = null;
									if(name.compareTo(other)>0){
										first2 = name;
										second2 = other;
									}else{
										first2 = other;
										second2 = name;
									}
									newPt.addNode(newBlockNode);
									newTaskNode1 = new AbstractTask.Manual(""+first2);
									newPt.addNode(newTaskNode1);
									newBlockNode.addChild(newTaskNode1);
									newTaskNode2 = new AbstractTask.Manual(""+second2);
									for(Edge edge : incoming)
										edge.setTarget(newBlockNode);
									newPt.addNode(newTaskNode2);
									newBlockNode.addChild(newTaskNode2);
									if(isRoot)
										newPt.setRoot(newBlockNode);
									if(params.isDuplicateTransitions())
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									else
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									break;
								case AND:
									if(!params.isUseAnd() || !frequencyMinimumMet || !languageRatioMinimumMet || !avgEnabledTransMinimumMet || !confidenceMinimumMet || (parentType==OPERATOR.AND && !firstChild))
										continue operatorLoop;
									newPt = new ProcessTreeImpl(ptNew);
									newManual = (Manual) newPt.getNode(id);
									incoming = newManual.getIncomingEdges();
									newPt.removeNode(manual);
									newBlockNode = new AbstractBlock.And("and", incoming, new LinkedList<Edge>());
									// test alphabetical order between name and other to decide order
									Character first3 = null;
									Character second3 = null;
									if(name.compareTo(other)>0){
										first3 = name;
										second3 = other;
									}else{
										first3 = other;
										second3 = name;
									}
									newPt.addNode(newBlockNode);
									newTaskNode1 = new AbstractTask.Manual(""+first3);
									newPt.addNode(newTaskNode1);
									newBlockNode.addChild(newTaskNode1);
									newTaskNode2 = new AbstractTask.Manual(""+second3);
									for(Edge edge : incoming)
										edge.setTarget(newBlockNode);
									newPt.addNode(newTaskNode2);
									newBlockNode.addChild(newTaskNode2);
									if(isRoot)
										newPt.setRoot(newBlockNode);
									if(params.isDuplicateTransitions())
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									else
										subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, newTransitionNames, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
									break;
							}
						}
				}
				// XORLOOP
				if(!params.isUseXorloop() || !frequencyMinimumMet || !avgEnabledTransMinimumMet || isRoot || hasLoopAncestor || !hasSeqAncestor)
					continue;

				ProcessTree newPt = new ProcessTreeImpl(ptNew);
				Manual newManual = (Manual) newPt.getNode(id);
				List<Edge> incoming = newManual.getIncomingEdges();
				newPt.removeNode(manual);
				AbstractBlock.XorLoop newBlockNode = new AbstractBlock.XorLoop("xorloop", incoming, new LinkedList<Edge>());
				newPt.addNode(newBlockNode);
				AbstractTask.Manual newTaskNode1 = new AbstractTask.Manual(""+name);
				newPt.addNode(newTaskNode1);
				newBlockNode.addChild(newTaskNode1);
				AbstractTask.Automatic newTaskNode2 = new AbstractTask.Automatic("tau1");
				newPt.addNode(newTaskNode2);
				newBlockNode.addChild(newTaskNode2);
				AbstractTask.Automatic newTaskNode3 = new AbstractTask.Automatic("tau2");
				newPt.addNode(newTaskNode3);
				newBlockNode.addChild(newTaskNode3);
				for(Edge edge : incoming)
					edge.setTarget(newBlockNode);
				if(isRoot)
					newPt.setRoot(newBlockNode);
				if(params.isDuplicateTransitions())
					subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, null, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
				else
					subtasks.add(new MaximalUtilityLPMRecursiveAction(context, params, lpmTreeSet, newPt, recursionDepth+1, transitionNamesForNonDuplicateMode, transitions, petriNetsExplored, topSet, logActivityCountsMap, previousLPM));
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

	private class NodeComparator implements Comparator<Node> {
		public int compare(Node one, Node two) {
			int compareResult = one.getName().compareTo(two.getName());
			boolean oneUnorderedBlock = (one instanceof AbstractBlock.And || one instanceof AbstractBlock.Xor || one instanceof AbstractBlock.Or);
			boolean twoUnorderedBlock = (two instanceof AbstractBlock.And || two instanceof AbstractBlock.Xor || two instanceof AbstractBlock.Or);
			if(!oneUnorderedBlock && !twoUnorderedBlock)
				return compareResult;
			if(!oneUnorderedBlock || !twoUnorderedBlock)
				return oneUnorderedBlock ? 1 : -1;
			EdgesComparator comparator = new EdgesComparator();
			Block oneBlock = (Block) one;
			Block twoBlock = (Block) two;
			return comparator.compare(oneBlock.getOutgoingEdges(), twoBlock.getOutgoingEdges());
		}
	}

	private class EdgeComparator implements Comparator<Edge> {
		public int compare(Edge one, Edge two) {
			NodeComparator comparator = new NodeComparator();
			return comparator.compare(one.getTarget(), two.getTarget());
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
