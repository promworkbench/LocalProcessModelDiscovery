package org.processmining.lpm.util;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.calculateLanguage;
import static org.processmining.lpm.util.LogUtils.getActivityCounts;
import static org.processmining.lpm.util.PetrinetUtils.DUMMY;
import static org.processmining.lpm.util.PetrinetUtils.getEnabledTransitions;
import static org.processmining.lpm.util.PetrinetUtils.getMarkingAfterFiring;
import static org.processmining.lpm.util.PetrinetUtils.projectLogOnNetAlphabet;
import static org.processmining.lpm.util.PetrinetUtils.transitionToEventClassMapperByLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinetclassicalreductor.parameters.ReduceUsingMurataRulesParameters;
import org.processmining.acceptingpetrinetclassicalreductor.plugins.ReduceUsingMurataRulesPlugin;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.adjustedalignments.NBestOptAlignmentsNoModelMoveGraphSamplingAlg;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.efficientlog.EfficientEventSet;
import org.processmining.lpm.efficientlog.FifoHashMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayer.matchinstances.PNLogMatchInstancesReplayer;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Doubles;

import nl.tue.astar.AStarException;

@Plugin(
		name = "Score Petri Net Fragment on Log using Alignments", 
		parameterLabels = {"Accepting Petri Net", "Input Log"}, 
		returnLabels = {"Scored Accepting Petri Net"}, 
		returnTypes = { LocalProcessModel.class }
		)
public class LocalProcessModelEvaluator {
	protected Map<String,XLog> efficientLogCache;
	protected EfficientEventSet ees;
	protected LocalProcessModelParameters params;
	
	public LocalProcessModelEvaluator(){}
	
	public LocalProcessModelEvaluator(LocalProcessModelParameters params){
		this.efficientLogCache = java.util.Collections.synchronizedMap(new FifoHashMap<String,XLog>(params.getEfficientLogCacheSize()));
		this.params = params;
		this.ees = new EfficientEventSet(params.getEvaluationLog());
	}
	
	public void refreshEfficientLogComponents(){
		this.efficientLogCache = java.util.Collections.synchronizedMap(new FifoHashMap<String,XLog>(params.getEfficientLogCacheSize()));
		this.ees = new EfficientEventSet(params.getEvaluationLog());
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Score Petri Net Fragment on Log using Alignments", requiredParameterLabels = {0, 1})
	public LocalProcessModel run(PluginContext context, AcceptingPetriNet apn, XLog log){
		Map<String, Integer> activityCounts = getActivityCounts(log);
		LocalProcessModelParameters params = new LocalProcessModelParameters();
		params.setUseEfficientLog(false);
		params.setEvaluationLog(log);
		params.setReturnMurataOrdered(false);
		LocalProcessModelEvaluator scorer = new LocalProcessModelEvaluator(params);
		return scorer.evaluateNetOnLog(context, apn, activityCounts, true);
	}

	public LocalProcessModel evaluateNetOnLog(PluginContext context, AcceptingPetriNet apn, Map<String, Integer> activityCountsMap, boolean verbose){
		int maxLoop = 3;
		Map<Transition, Integer> countsMap = new HashMap<Transition, Integer>();
		Set<List<String>> languageElementsInLog = new HashSet<List<String>>();
		if(params.isReturnMurataOrdered()){
			ReduceUsingMurataRulesPlugin murata = new ReduceUsingMurataRulesPlugin();
			ReduceUsingMurataRulesParameters murataParams = new ReduceUsingMurataRulesParameters();
			murataParams.setRetainBehavior(true);
			murataParams.setTryConnections(false);
			apn =  murata.run(context, apn, murataParams);
		}
		Petrinet pn = apn.getNet();
		Transition backLoop = pn.addTransition("backloop");
		backLoop.setInvisible(true);
		Place endPlace = new ArrayList<Marking>(apn.getFinalMarkings()).get(0).toList().get(0);
		Arc aIn = pn.addArc(endPlace, backLoop);
		Marking initialMarking = apn.getInitialMarking();
		Place startPlace = initialMarking.toList().get(0);
		Arc aOut = pn.addArc(backLoop, startPlace);
		Set<Marking> finalMarkings = apn.getFinalMarkings();
		Set<Marking> newInitialAndFinalMarkings = new HashSet<Marking>();
		Marking newInitialAndFinalMarking = new Marking();
		newInitialAndFinalMarking.add(endPlace);
		newInitialAndFinalMarkings.add(newInitialAndFinalMarking);
		apn.setInitialMarking(newInitialAndFinalMarking);
		apn.setFinalMarkings(newInitialAndFinalMarkings);
		
		
		XLog projectedLog = null;
		double remainedRatio = 0d;
		int maxTraceLength = 0;
		if(!params.isUseEfficientLog()){
			Object[] o = projectLogOnNetAlphabet(params.getEvaluationLog(), pn);
			projectedLog = (XLog) o[0];
			remainedRatio = (double) o[1];
			maxTraceLength = (int) o[2];
			o = null;
		}else{
			String hash = params.getEfficientLogExtractor().hashEquals(apn);
			if(efficientLogCache.containsKey(hash)){
				projectedLog = efficientLogCache.get(hash);
			}else{
				projectedLog = ees.extractXLog(apn, params.getEfficientLogExtractor());
				efficientLogCache.put(hash, projectedLog);
			}
			//Object[] o = projectLogOnActivitySet(log, alphabet);
			int size = 0;
			for(XTrace trace : projectedLog){
				size += trace.size();
				if(trace.size()>maxTraceLength)
					maxTraceLength = trace.size();
			}
			//projectedLog = (XLog) o[0];
			remainedRatio = ((double)size)/ees.getSize();
			//maxTraceLength = (int) o[2];
		}

		double avgFitness = 0;
		int seen = 0;
		double avgNumFirings = 0d;
		int backLoopCount = 0;
		int tracesSeen = 0;
		int avgEnabledCounter = 0;
		double avgEnabledTransitions = 0d;
		
		PNMatchInstancesRepResult alignments = null;
		try{
			alignments = calculateNAlignment(context, projectedLog, apn, maxTraceLength, backLoop, params, verbose);
		}catch(NullPointerException e){
			// Bizar dat dit gebeurd... De EventClasses veranderen tussen de aanroep vanuit calculateNAlignment (line 316) en AbstractPDelegate.initEventClasses (line 207)
		}
		if(alignments!=null){
			Iterator<AllSyncReplayResult> sync = alignments.iterator();
			while(sync.hasNext()){
				AllSyncReplayResult srr = sync.next();
				int processVariantCount = srr.getTraceIndex().size();
				seen++;
				List<Object> nodeInstances = srr.getNodeInstanceLst().get(0);
				List<StepTypes> stepTypes = srr.getStepTypesLst().get(0);
				int alignmentLength = 0;
				int localBackLoopCount = 0;
				List<String> languageElem = new LinkedList<String>();
				Map<Transition, Integer> transitionFireCountInInstance = new HashMap<Transition, Integer>();
				Multiset<Place> currentMarking = HashMultiset.create();
				currentMarking.add(endPlace);
	
				Set<Transition> enabled = getEnabledTransitions(pn, currentMarking);
				//enabled.remove(backLoop);
				int numEnabled = enabled.size();
				/*
				if(numEnabled>0){
					avgEnabledTransitions = (avgEnabledCounter * avgEnabledTransitions + processVariantCount*numEnabled) / (avgEnabledCounter+processVariantCount);
					avgEnabledCounter+=processVariantCount;
				}
				 */
				for(int i=0; i<nodeInstances.size(); i++){
					Object nodeInstance = nodeInstances.get(i);
					StepTypes st = stepTypes.get(i);
					if(nodeInstance instanceof Transition){
						Transition transition = (Transition) nodeInstance;
						if(!transition.isInvisible()){
							if(numEnabled>0){
								avgEnabledTransitions = (avgEnabledCounter * avgEnabledTransitions + processVariantCount* numEnabled) / (avgEnabledCounter+processVariantCount);
								avgEnabledCounter+=processVariantCount;
							}
						}
						currentMarking = getMarkingAfterFiring(pn, currentMarking, transition);
						enabled = getEnabledTransitions(pn, currentMarking);
						numEnabled = enabled.size();
						if(!transition.isInvisible()){
							languageElem.add(transition.getLabel());
							alignmentLength++;
							Integer counts = countsMap.get(transition);
							if(counts==null)
								counts = 0;
							counts+=processVariantCount;
							countsMap.put(transition, counts);
							if(verbose){
								System.out.println("t: "+transition.getLabel()+" __ "+st.name());
								System.out.println("Updated transition "+transition.getLabel()+" with "+processVariantCount+" to "+counts);
							}
						}else if(transition.equals(backLoop)){// invisible and backloop
							if(verbose)
								System.out.println("t: BACKLOOP __ "+st.name());
							localBackLoopCount+=processVariantCount;
							boolean fitsMaxLoopConstraint = true;
							for(Integer count : transitionFireCountInInstance.values())
								if(count>maxLoop)
									fitsMaxLoopConstraint = false;
							if(fitsMaxLoopConstraint)		
								languageElementsInLog.add(languageElem);
							languageElem = new LinkedList<String>();
						}else{// invisible but no backloop
							if(verbose)
								System.out.println("t: INV __ MINVI");
						}
					}else{
						if(verbose){
							XEventClass eventClass = (XEventClass) nodeInstance;
							System.out.println("e: "+eventClass+" __ "+st.name());
						}
					}
				}
				Double c = srr.getInfo().get(PNMatchInstancesRepResult.MAXFITNESSCOST);
	
				if(verbose){
					System.out.println("cost: "+c);
					System.out.println();
				}
				languageElementsInLog.add(languageElem);
				//protection against integer overflow of alignments
				if(c<0) // protection against integer overflow in alignments
					return new LocalProcessModel(apn, 0, 0, 0, 0, 0, 0, 0, 0, languageElementsInLog, languageElementsInLog, countsMap, params);
				double originalLength = srr.getInfo().get(PNMatchInstancesRepResult.ORIGTRACELENGTH);
				double fitness = 1 - (c / Math.max(originalLength, alignmentLength));
				avgFitness = (seen*avgFitness + fitness)/(seen+1);
				if(avgFitness<0) // protection against integer overflow in alignments
					return new LocalProcessModel(apn, 0, 0, 0, 0, 0, 0, 0, 0, languageElementsInLog, languageElementsInLog, countsMap, params);
				if(alignmentLength>0&&localBackLoopCount>0){
					avgNumFirings = (avgNumFirings * tracesSeen + ((double) alignmentLength)/localBackLoopCount)/(tracesSeen+1);
					tracesSeen++;
				}
				backLoopCount+=localBackLoopCount;
			}
		}
		
		pn.removeEdge(aIn);
		pn.removeEdge(aOut);
		pn.removeTransition(backLoop);
		apn.setFinalMarkings(finalMarkings);
		apn.setInitialMarking(initialMarking);

		Set<List<String>> petriNetLanguage = calculateLanguage(apn, maxLoop, true);
		languageElementsInLog.retainAll(petriNetLanguage);
		double languageElementsSeenRatio = ((double) languageElementsInLog.size()) / petriNetLanguage.size();
		double determinism = 1d/avgEnabledTransitions;

		List<Double> ratios = new LinkedList<Double>();
		for(Transition t : apn.getNet().getTransitions()){
			if(!t.isInvisible()){
				if(countsMap.containsKey(t)){
					double c = countsMap.get(t);
					ratios.add(c/activityCountsMap.get(t.getLabel()));
				}else
					ratios.add(0d);
			}
		}

		double harmonicConfidence = ratios.size()==0 ? 0 : harmonicMean(Doubles.toArray(ratios));
		Map<String, Integer> logActivityCountsMap = getActivityCounts(params.getEvaluationLog());

		LocalProcessModel scoredAPN = new LocalProcessModel(apn);
		scoredAPN.setHarmonicConfidence(harmonicConfidence);
		scoredAPN.setFrequency(backLoopCount);
		scoredAPN.setAlignmentCost(avgFitness);
		scoredAPN.setRemainedRatio(remainedRatio);
		scoredAPN.setAvgNumFirings(avgNumFirings);
		scoredAPN.setLanguageRatio(languageElementsSeenRatio);
		scoredAPN.setDeterminism(determinism);
		scoredAPN.setLanguage(petriNetLanguage);
		scoredAPN.setLanguageSeen(languageElementsInLog);
		scoredAPN.setCountsMap(countsMap);
		scoredAPN.setParams(params);
		scoredAPN.setLogActivityCountMap(logActivityCountsMap);
		scoredAPN.setAlignments(alignments);

		if(verbose)
			System.out.println(scoredAPN.toString());
		
		return scoredAPN; 
	}

	public static PNMatchInstancesRepResult calculateNAlignment(PluginContext context, XLog log, AcceptingPetriNet apn, int maxTraceLength, Transition backloop, LocalProcessModelParameters params, boolean verbose){
		Petrinet ptnet = apn.getNet();
		TransEvClassMapping oldMap = transitionToEventClassMapperByLabel(log, ptnet);
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, oldMap.getEventClassifier());

		CostBasedCompleteParam parameter = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(), DUMMY, ptnet.getTransitions(), Integer.MAX_VALUE, 1);

		//parameter.setInitialMarking(apn.getInitialMarking());
		parameter.getMapEvClass2Cost().remove(DUMMY);
		parameter.getMapEvClass2Cost().put(DUMMY, 1);
		parameter.setGUIMode(false);

		for(Transition t : apn.getNet().getTransitions()){
			if(!t.isInvisible())
				parameter.getMapTrans2Cost().put(t, maxTraceLength);
			else
				parameter.getMapTrans2Cost().put(t, 0);
		}

		parameter.setCreateConn(false);

		Marking initialMarking = apn.getInitialMarking();
		Marking finalMarking = new ArrayList<Marking>(apn.getFinalMarkings()).get(0);

		parameter.setMaxNumOfStates(params.getAlignmentMaxNumStatesPerTransition() * apn.getNet().getTransitions().size());
		if(verbose){
			System.out.println("log: ");
			for(XTrace trace : log){
				for(XEvent event : trace){
					System.out.print(event.getAttributes().get("concept:name")+", ");
				}
				System.out.println();
			}

			System.out.println();
			System.out.println("mapping:                        "+oldMap);
			System.out.println("parameter.getMapTrans2Cost():   "+parameter.getMapTrans2Cost());
			System.out.println("parameter.getMapEvClass2Cost(): "+parameter.getMapEvClass2Cost());
		}
		Object[] parameters = new Object[]{parameter.getMapTrans2Cost(), parameter.getMaxNumOfStates(), parameter.getMapEvClass2Cost(), 1};
		
		// select algorithm without ILP
		PNLogMatchInstancesReplayer replayer = new PNLogMatchInstancesReplayer();
		NBestOptAlignmentsNoModelMoveGraphSamplingAlg alg = new NBestOptAlignmentsNoModelMoveGraphSamplingAlg();

		PNMatchInstancesRepResult pnRepResult = null;
		try {
			pnRepResult = replayer.replayLog(context, ptnet, log, oldMap, initialMarking, finalMarking, alg, parameters);
		} catch (AStarException e) {
			e.printStackTrace();
		} catch(Throwable e){
			e.printStackTrace();
		}
		if(verbose){
			System.out.println("result1: "+pnRepResult.first().getInfo().get(PNMatchInstancesRepResult.MAXFITNESSCOST));
			System.out.println("result2: "+pnRepResult.first().getInfo().get(PNMatchInstancesRepResult.MINFITNESSCOST));
		}
		logInfo = null;
		return pnRepResult;
	}
	
	public static double harmonicMean(double[] data){  
		double sum = 0.0;

		for (int i = 0; i < data.length; i++) { 
			sum += 1.0 / data[i]; 
		} 
		return data.length / sum; 
	}
}