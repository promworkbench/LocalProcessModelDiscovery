package org.processmining.lpm.discovery;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.decodeTransitionLabels;
import static org.processmining.lpm.util.LogUtils.encodeLogUsingSchemeWithAttributes;
import static org.processmining.lpm.util.LogUtils.getActivityCounts;
import static org.processmining.lpm.util.LogUtils.getLogDecodingScheme;
import static org.processmining.lpm.util.LogUtils.getLogEncodingScheme;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.dialogs.LocalProcessModelParameters.ProjectionMethods;
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.LocalProcessModelRankingFactory;
import org.processmining.lpm.util.UtilityAlignmentAcceptingPetriNetScorer;
import org.processmining.lpm.util.UtilityAlignmentScoredAcceptingPetrinetContainer;
import org.processmining.lpm.util.UtilityLocalProcessModel;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;

@Plugin(
		name = "Search for Utility-Closed Local Process Models", 
		parameterLabels = {"Input Log"}, 
		returnLabels = {"Utility-Closed Local Process Model Ranking"}, 
		returnTypes = { LocalProcessModelRanking.class }
		)
public class ClosedUtilityLocalProcessModelDiscovery extends LocalProcessModelDiscovery{
	private AtomicInteger petriNetsExplored;

	private UtilityLocalProcessModelParameters params;
	private UtilityAlignmentScoredAcceptingPetrinetContainer topSet;

	private Map<String, Character> encodingScheme;
	private Map<Character, String> decodingScheme;
	private Set<Character> transitions;

	private Set<String> lpmTreeSet;

	private Map<String, Integer> logActivityCountsMap;

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Search for Utility-Closed Local Process Models", requiredParameterLabels = {0})
	public LocalProcessModelRanking run2(PluginContext context, XLog log) {
		params = new UtilityLocalProcessModelParameters();
		params.setSmartParameterDefaultsForDiscoveryLog();
		params.setUtilityMinimum(0);
		params.setUtilityAttributeName("Costs");
		params.setDiscoveryLog(log);
		params.setEvaluationLog(log);
		return runHeadless(context, params); // uses default parameters
	}

	@Override
	public LocalProcessModelRanking runHeadlessProjectionless(PluginContext context, LocalProcessModelParameters params){
		UtilityLocalProcessModelParameters uParams = (UtilityLocalProcessModelParameters) params;
		this.params = uParams;
		
		encodingScheme = getLogEncodingScheme(params.getDiscoveryLog());
		decodingScheme = getLogDecodingScheme(encodingScheme);
		transitions = decodingScheme.keySet();
		params.setEncodingScheme(encodingScheme);
		params.setDecodingScheme(decodingScheme);

		XLog evaluationLog = params.getEvaluationLog();
		params.setDiscoveryLog(encodeLogUsingSchemeWithAttributes(params.getDiscoveryLog(), encodingScheme, new String[]{"Costs"}));
		params.setEvaluationLog(encodeLogUsingSchemeWithAttributes(params.getEvaluationLog(), encodingScheme, new String[]{"Costs"}));

		this.logActivityCountsMap = getActivityCounts(params.getDiscoveryLog());

		topSet = new UtilityAlignmentScoredAcceptingPetrinetContainer(params.getTop_k());
		petriNetsExplored = new AtomicInteger();

		ProcessTree pt = new ProcessTreeImpl();

		lpmTreeSet = Collections.synchronizedSet(new HashSet<String>());

		ForkJoinPool p = new ForkJoinPool(Runtime.getRuntime().availableProcessors()-1);
				
		ClosedUtilityLPMRecursiveAction root = new ClosedUtilityLPMRecursiveAction(context, uParams, lpmTreeSet, pt, 0, null, transitions, petriNetsExplored, topSet, logActivityCountsMap, null);

		p.invoke(root);

		params.setEvaluationLog(evaluationLog);
		UtilityAlignmentScoredAcceptingPetrinetContainer petriContainer = new UtilityAlignmentScoredAcceptingPetrinetContainer(topSet.getMaxSize());
		if(params.isVerbose())
			System.out.println("Final selection: ");
		Map<String, Integer> logActivityCountsMapNonEncoded = getActivityCounts(evaluationLog);
		for(UtilityLocalProcessModel sapn : topSet){
			// decode Petri nets
			AcceptingPetriNet net2 = decodeTransitionLabels(sapn.getAcceptingPetriNet(), decodingScheme);
			UtilityLocalProcessModel murataSapn = UtilityAlignmentAcceptingPetriNetScorer.evaluateNetOnLog(context, net2, logActivityCountsMapNonEncoded, uParams.isReturnMurataOrdered(), uParams, false);
			
			murataSapn.setLogActivityCountMap(logActivityCountsMapNonEncoded);
			murataSapn.copyUtilityList(sapn.getUtilityList());
			petriContainer.add(murataSapn); // needed to resort the Murata versions of the nets (determinism score might be slightly different)
		}

		LocalProcessModelRanking petriList = LocalProcessModelRankingFactory.createCountedAcceptingPetriNetArray();
		
		for(UtilityLocalProcessModel sapn : petriContainer)
			petriList.addElement(sapn);
		if(params.isVerbose())
			System.out.println(petriList.getSize()+" Petri nets selected out of "+petriNetsExplored+" candidates");

		return petriList;
	}

	@Override
	public LocalProcessModelRanking runHeadless(PluginContext context, LocalProcessModelParameters params){
		if(params.getProjectionMethod().equals(ProjectionMethods.None))
			return runHeadlessProjectionless(context, params);
		else if(params.getProjectionMethod().equals(ProjectionMethods.Markov))
			return MarkovBasedSearch.runHeadless(context, 0,  params, this, 1.5);
		else if(params.getProjectionMethod().equals(ProjectionMethods.MRIG))
			return MRIGBasedSearch.runHeadless(context, params, this);
		else if(params.getProjectionMethod().equals(ProjectionMethods.Entropy))
			return EntropyBasedSearch.runHeadless(context, params, this);
		return null;
	}
}