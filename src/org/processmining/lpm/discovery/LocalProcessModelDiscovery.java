package org.processmining.lpm.discovery;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.decodeTransitionLabels;
import static org.processmining.lpm.util.LogUtils.encodeLogUsingScheme;
import static org.processmining.lpm.util.LogUtils.getActivityCounts;
import static org.processmining.lpm.util.LogUtils.getLogDecodingScheme;
import static org.processmining.lpm.util.LogUtils.getLogEncodingScheme;
import static org.processmining.lpm.util.LogUtils.getLpmCountUpperBoundsMap;
import static org.processmining.lpm.util.ProcessTreeUtils.decodeTransitionLabels;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.dialogs.LocalProcessModelParameters.ProjectionMethods;
import org.processmining.lpm.util.LPMXLog;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelEvaluator;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.LocalProcessModelRankingFactory;
import org.processmining.lpm.util.LocalProcessModelTopSet;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;

public class LocalProcessModelDiscovery {
	private LocalProcessModelTopSet topSet;

	private Map<String, Character> encodingScheme;
	private Map<Character, String> decodingScheme;
	private Set<Character> transitions;
	
	private Set<String> lpmTreeSet;
	
	private Map<String, Integer> logActivityCountsMap;
	private Map<Character, Set<Character>> possibleSiblingMap; // containts co-occurrence counts for seq(a,b) and and(a,b) relations, of which the support is upper-bounded by co-occurrence
		
	public LocalProcessModelRanking runHeadlessProjectionless(PluginContext context, LocalProcessModelParameters params){
		encodingScheme = getLogEncodingScheme(params.getDiscoveryLog());
		decodingScheme = getLogDecodingScheme(encodingScheme);
		transitions = decodingScheme.keySet();
		
		XLog evaluationLog = params.getEvaluationLog();
		params.setDiscoveryLog(encodeLogUsingScheme(params.getDiscoveryLog(), encodingScheme, params.getAttributesToKeep()));
		params.setEvaluationLog(params.getDiscoveryLog());
		this.logActivityCountsMap = getActivityCounts(params.getDiscoveryLog());
		this.possibleSiblingMap = upperBoundsMapToPossibleSiblingsMap(params.getFrequencyMinimum(), getLpmCountUpperBoundsMap(params.getDiscoveryLog()));
		
		topSet = new LocalProcessModelTopSet(params.getTop_k());
		
		ProcessTree pt = new ProcessTreeImpl();
		
		lpmTreeSet = Collections.synchronizedSet(new HashSet<String>());
		LocalProcessModelEvaluator scorer = new LocalProcessModelEvaluator(params);

		ForkJoinPool p = new ForkJoinPool(Runtime.getRuntime().availableProcessors()-1);
		LPMRecursiveAction root = new LPMRecursiveAction(context, params, lpmTreeSet, pt, 0, null, transitions, topSet, logActivityCountsMap, possibleSiblingMap, scorer);
		
		p.invoke(root);
		
		params.setEvaluationLog(evaluationLog);
		scorer.refreshEfficientLogComponents();
		
		LocalProcessModelTopSet resultTopSet = new LocalProcessModelTopSet(topSet.getMaxSize());
		if(params.isVerbose())
			System.out.println("Final selection: ");
		Map<String, Integer> logActivityCountsMapNonEncoded = getActivityCounts(evaluationLog);
		for(LocalProcessModel lpm : topSet){
			// decode Petri nets
			AcceptingPetriNet net2 = decodeTransitionLabels(lpm.getAcceptingPetriNet(), decodingScheme);
			LocalProcessModel murataLpm = scorer.evaluateNetOnLog(context, net2, logActivityCountsMapNonEncoded, false);
			if(params.isStoreProcessTree()){
				murataLpm.setProcessTree(decodeTransitionLabels(lpm.getProcessTree(), decodingScheme));
			}
			murataLpm.setLogActivityCountMap(logActivityCountsMapNonEncoded);
			resultTopSet.add(murataLpm); // needed to resort the Murata versions of the nets (determinism score might be slightly different)
		}
		
		LocalProcessModelRanking finalResult = LocalProcessModelRankingFactory.createCountedAcceptingPetriNetArray();
		for(LocalProcessModel lpm : resultTopSet)
			finalResult.addElement(lpm);
		if(params.isVerbose())
			System.out.println(finalResult.getSize()+" Petri nets selected out of "+params.getNumberOfExploredLpms().get()+" candidates");
		params.setDiscoveryLog(evaluationLog);
		return finalResult;
	}

	private static Map<Character, Set<Character>> upperBoundsMapToPossibleSiblingsMap(int frequencyMinimum, Map<List<Character>, Integer> lpmUpperBoundsMap) {
		Map<Character, Set<Character>> possibleSiblingsMap = new HashMap<Character, Set<Character>>();
		for(List<Character> key : lpmUpperBoundsMap.keySet()){
			if(lpmUpperBoundsMap.get(key)>=frequencyMinimum){
				Set<Character> possibleNeighbors = null;
				if(!possibleSiblingsMap.containsKey(key.get(0)))
					possibleNeighbors = new HashSet<Character>();
				else
					possibleNeighbors = possibleSiblingsMap.get(key.get(0));
				possibleNeighbors.add(key.get(1));
				possibleSiblingsMap.put(key.get(0), possibleNeighbors);
			}
		}
		return possibleSiblingsMap;
	}
	
	public LocalProcessModelRanking runHeadless(PluginContext context, LocalProcessModelParameters params){
    	if(params.getProjectionMethod().equals(ProjectionMethods.None))
    		return runHeadlessProjectionless(context, params);
    	else if(params.getProjectionMethod().equals(ProjectionMethods.Markov))
    		return MarkovBasedSearch.runHeadless(context, 0, params, this, 1.5);
    	else if(params.getProjectionMethod().equals(ProjectionMethods.MRIG))
    		return MRIGBasedSearch.runHeadless(context, params, this);
    	else if(params.getProjectionMethod().equals(ProjectionMethods.Entropy))
    		return EntropyBasedSearch.runHeadless(context, params, this);
    	return null;
	}
}