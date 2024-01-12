package org.processmining.lpm.discovery;

import java.util.Arrays;
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
import java.util.concurrent.atomic.AtomicInteger;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNetArray;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetArrayImpl;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.plugins.ProcessTreeArray;
import org.processmining.lpm.util.AcceptingPetrinetUtils;
import org.processmining.lpm.util.LocalProcessModelTopSet;
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

@Plugin(
		name = "Detect redundancy in pragmatic LPM search (node order fix)", 
		parameterLabels = {}, 
	    returnLabels = {"Local Process Model Ranking"}, 
	    returnTypes = { ProcessTreeArray.class }
		)
public class PragmaticSearchRedundancyDetectorOperatorNodeOrder{
	private enum OPERATOR {SEQ, XOR, XORLOOP, OR, AND, LOOP, ROOT};
	private AtomicInteger petriNetsExplored;
	
	private LocalProcessModelTopSet topSet;

	private Set<Character> transitions;
	
	private Set<String> treeSet;
	
	private Map<Set<List<String>>, AcceptingPetriNetArrayImpl> netsByLanguage;
	private Map<Set<List<String>>, ProcessTreeArray> treesByLanguage;

	private LocalProcessModelParameters params;
		
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Detect redundancy in pragmatic LPM search (node order fix)", requiredParameterLabels = {})
	public ProcessTreeArray runHeadless(PluginContext context){
		petriNetsExplored.set(0);
		ProcessTree pt = new ProcessTreeImpl();
		treeSet = new HashSet<String>();
		params = new LocalProcessModelParameters();
		params.setConfidenceMinimum(0);
		params.setCoverageMinimum(0);
		params.setDeterminismMinimum(0);
		params.setFrequencyMinimum(0);
		params.setLanguageFitMinimum(0);
		params.setNumTransitions(4);
		params.setDuplicateTransitions(false);
		params.setUseAnd(true);
		params.setUseOr(false);
		params.setUseSeq(true);
		params.setUseXor(true);
		params.setUseXorloop(true);
		
		transitions = new HashSet<Character>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'));
		netsByLanguage = new HashMap<Set<List<String>>, AcceptingPetriNetArrayImpl>();
		treesByLanguage = new HashMap<Set<List<String>>, ProcessTreeArray>();
		
		// DO THE ACTUAL LABOUR
		recurseProcessTrees(context, pt, 0, null);
		
		for(Set<List<String>> language : netsByLanguage.keySet()){
			AcceptingPetriNetArray tempNets = netsByLanguage.get(language);
			ProcessTreeArray tempTrees = treesByLanguage.get(language);
			ProvidedObjectManager pom = context.getProvidedObjectManager();
			if(tempNets.getSize()>1){
				pom.createProvidedObject("nets "+language, tempNets, AcceptingPetriNetArray.class, context);
				pom.createProvidedObject("trees "+language, tempTrees, ProcessTreeArray.class, context);
			}
		}
		System.out.println("Total explored: "+petriNetsExplored);
		return new ProcessTreeArray();
	}
	
	private void recurseProcessTrees(PluginContext context, ProcessTree pt, int recursionDepth, Set<Character> transitionNamesForNonDuplicateMode){
		// normalize tree
		ProcessTree ptNew = normalizeTree(pt);
		String treeAsString = ptNew.toString();
		if(treeSet.contains(treeAsString))
			return;
		treeSet.add(treeAsString);
		if(petriNetsExplored.getAndIncrement()%1000==0){
			System.out.println(petriNetsExplored+" Petri nets explored");
			params.setNumberOfExploredLpms(petriNetsExplored);
		}
		
		boolean frequencyMinimumMet = true;
		boolean avgEnabledTransMinimumMet = true;
		boolean languageRatioMinimumMet = true;
		boolean confidenceMinimumMet = true;
		
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
				
				//AlignmentScoredAcceptingPetriNet scoredNet = AlignmentAcceptingPetriNetScorer.evaluateNetOnLog(context, apn, logActivityCountsMap, false, params, false);
				frequencyMinimumMet = true;// scoredNet.getAlignmentCountRatio() > params.getFrequencyMinimum();
				avgEnabledTransMinimumMet = true;// scoredNet.getAvgEnabledTransitions() > params.getDeterminismMinimum();
				languageRatioMinimumMet = true;//scoredNet.getLanguageRatio() > params.getLanguageRatioMinimum();
				confidenceMinimumMet = true;//scoredNet.getHarmonicConfidence() > params.getConfidenceMinimum();
				if(!frequencyMinimumMet || !avgEnabledTransMinimumMet || !languageRatioMinimumMet || !confidenceMinimumMet){
					pwm = null;
					apn = null;
					nameToTransitionMap = null;
				}else{
					// not garbage collected automatically
					pwm = null;
					apn = null;
					nameToTransitionMap = null;
				}
			}
		}else{
			for(Character other : transitions){
				ProcessTree newpt = new ProcessTreeImpl(ptNew);
				AbstractTask.Manual manual = new AbstractTask.Manual(""+other);
				newpt.addNode(manual);
				newpt.setRoot(manual);
				if(params.isDuplicateTransitions())
					recurseProcessTrees(context, newpt, recursionDepth+1, null);
				else{
					Set<Character> newTransitionNames = new HashSet<Character>(transitions);
					newTransitionNames.remove(other);
					recurseProcessTrees(context, newpt, recursionDepth+1, newTransitionNames);
				}
			}
			return;
		}
		
		if(recursionDepth>=params.getNumTransitions() || (!params.isDuplicateTransitions() && transitionNamesForNonDuplicateMode.isEmpty()))
			return;
		else{
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
				Set<List<String>> language = AcceptingPetrinetUtils.calculateLanguage(apn, 5, true);
				AcceptingPetriNetArrayImpl currentPNA = null; 
				ProcessTreeArray currentPTA = null; 
				if(!netsByLanguage.containsKey(language)){
					currentPNA = new AcceptingPetriNetArrayImpl();
					currentPTA = new ProcessTreeArray();
				}else{
					currentPNA = netsByLanguage.get(language);
					currentPTA = treesByLanguage.get(language);
				}
				currentPNA.addNet(apn);
				currentPTA.addTree(ptNew);
				netsByLanguage.put(language, currentPNA);
				treesByLanguage.put(language, currentPTA);
			}
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
									recurseProcessTrees(context, newPt, recursionDepth+1, null);
								else
									recurseProcessTrees(context, newPt, recursionDepth+1, newTransitionNames);
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
									recurseProcessTrees(context, newPt, recursionDepth+1, null);
								else
									recurseProcessTrees(context, newPt, recursionDepth+1, newTransitionNames);
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
									recurseProcessTrees(context, newPt, recursionDepth+1, null);
								else
									recurseProcessTrees(context, newPt, recursionDepth+1, newTransitionNames);
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
									recurseProcessTrees(context, newPt, recursionDepth+1, null);
								else
									recurseProcessTrees(context, newPt, recursionDepth+1, newTransitionNames);
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
					recurseProcessTrees(context, newPt, recursionDepth+1, null);
				else
					recurseProcessTrees(context, newPt, recursionDepth+1, transitionNamesForNonDuplicateMode);
			}
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
		//if(i>2)
		//	System.out.println(pt+" changed into "+newPt);
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