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
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.dialogs.LocalProcessModelParameters.ProjectionMethods;
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters;
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters.PruningStrategies;
import org.processmining.lpm.util.LPMXLog;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.LocalProcessModelRankingFactory;
import org.processmining.lpm.util.ParameterizedLPMXLog;
import org.processmining.lpm.util.UtilityAlignmentAcceptingPetriNetScorer;
import org.processmining.lpm.util.UtilityAlignmentScoredAcceptingPetrinetContainer;
import org.processmining.lpm.util.UtilityLocalProcessModel;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;

@Plugin(
		name = "Search for High Utility Local Process Models", 
		parameterLabels = {"Input Log"}, 
		returnLabels = {"High-Utility Local Process Model Ranking"}, 
		returnTypes = { ParameterizedLPMXLog.class }
		)
public class HighUtilityLocalProcessModelDiscovery extends LocalProcessModelDiscovery{
	private AtomicInteger petriNetsExplored;

	private UtilityLocalProcessModelParameters params;
	private UtilityAlignmentScoredAcceptingPetrinetContainer topSet;

	private Map<String, Character> encodingScheme;
	private Map<Character, String> decodingScheme;
	private Set<Character> transitions;

	private Set<String> lpmTreeSet;

	private Map<String, Integer> logActivityCountsMap;

	@Override
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Search for High Utility Local Process Models", requiredParameterLabels = {0})
	public LPMXLog run(UIPluginContext context, XLog log) {
		params = new UtilityLocalProcessModelParameters();
		params.setSmartParameterDefaultsForDiscoveryLog();
		params.setUtilityAttributeName("Costs");
		params.setUtilityMinimum(1);
		params.setDiscoveryLog(log);
		params.setEvaluationLog(log);
		params.setPruningStrategy(PruningStrategies.BEST_L);
		params.setUseEfficientLog(false);
		//params.setMax_consecutive_nonfitting(100);
		//params.setMax_consecutive_timedif_millis(Integer.MAX_VALUE);
		//params.setMax_total_nonfitting(100);
		//params.setMax_total_timedif_millis(Integer.MAX_VALUE);
		params.setPruningK(1);
		params.setProjectionMethod(ProjectionMethods.Markov);
		params.setUtilityMinimum(400);
		params.setTop_k(100);
		params.setLanguageFitMinimum(0.00);
		params.setDeterminismMinimum(0.66);
		params.setFrequencyMinimum(3);
		params.setMaxActivityFrequencyInLog(20);
		return new ParameterizedLPMXLog(log, params); // uses default parameters
	}
	
	@Override
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Search for High Utility Local Process Models", requiredParameterLabels = {0})
	public ParameterizedLPMXLog run(PluginContext context, XLog log) {
		params = new UtilityLocalProcessModelParameters();
		params.setSmartParameterDefaultsForDiscoveryLog();
		params.setUtilityAttributeName("Costs");
		params.setUtilityMinimum(1);
		params.setDiscoveryLog(log);
		params.setEvaluationLog(log);
		params.setPruningStrategy(PruningStrategies.BEST_L);
		params.setUseEfficientLog(false);
		//params.setMax_consecutive_nonfitting(100);
		//params.setMax_consecutive_timedif_millis(Integer.MAX_VALUE);
		//params.setMax_total_nonfitting(100);
		//params.setMax_total_timedif_millis(Integer.MAX_VALUE);
		params.setPruningK(3);
		params.setProjectionMethod(ProjectionMethods.Markov);
		params.setUtilityMinimum(400);
		params.setTop_k(100);
		params.setLanguageFitMinimum(0.00);
		params.setDeterminismMinimum(0.66);
		params.setFrequencyMinimum(3);
		params.setMaxActivityFrequencyInLog(20);
		return new ParameterizedLPMXLog(log, params); // uses default parameters
	}

	public LocalProcessModelRanking runHeadlessProjectionless(PluginContext context, LocalProcessModelParameters params){
		UtilityLocalProcessModelParameters uParams = (UtilityLocalProcessModelParameters) params;
		this.params = uParams;
		
		encodingScheme = getLogEncodingScheme(uParams.getDiscoveryLog());
		decodingScheme = getLogDecodingScheme(encodingScheme);
		transitions = decodingScheme.keySet();
		uParams.setEncodingScheme(encodingScheme);
		uParams.setDecodingScheme(decodingScheme);

		XLog evaluationLog = uParams.getEvaluationLog();
		uParams.setDiscoveryLog(encodeLogUsingSchemeWithAttributes(uParams.getDiscoveryLog(), encodingScheme, new String[]{"Costs"}));
		uParams.setEvaluationLog(encodeLogUsingSchemeWithAttributes(uParams.getEvaluationLog(), encodingScheme, new String[]{"Costs"}));

		this.logActivityCountsMap = getActivityCounts(uParams.getDiscoveryLog());

		topSet = new UtilityAlignmentScoredAcceptingPetrinetContainer(uParams.getTop_k());
		petriNetsExplored = new AtomicInteger();

		ProcessTree pt = new ProcessTreeImpl();

		lpmTreeSet = Collections.synchronizedSet(new HashSet<String>());

		ForkJoinPool p = new ForkJoinPool(Runtime.getRuntime().availableProcessors()-1);
		
		HighUtilityLPMRecursiveAction root = new HighUtilityLPMRecursiveAction(context, uParams, lpmTreeSet, pt, 0, null, transitions, topSet, logActivityCountsMap, null);

		p.invoke(root);

		uParams.setEvaluationLog(evaluationLog);
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