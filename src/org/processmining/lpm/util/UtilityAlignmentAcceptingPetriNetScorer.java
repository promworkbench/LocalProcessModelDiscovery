package org.processmining.lpm.util;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.calculateLanguage;
import static org.processmining.lpm.util.LogUtils.getActivityCounts;
import static org.processmining.lpm.util.PetrinetUtils.getEnabledTransitions;
import static org.processmining.lpm.util.PetrinetUtils.getMarkingAfterFiring;
import static org.processmining.lpm.util.PetrinetUtils.projectLogOnNetAlphabet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
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
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Doubles;

import flanagan.analysis.Stat;

@Plugin(
		name = "Score Petri Net Fragment with Utility on Log using Alignments", 
		parameterLabels = {"Accepting Petri Net", "Input Log"}, 
		returnLabels = {"Scored Accepting Petri Net"}, 
		returnTypes = { UtilityLocalProcessModel.class }
		)
public class UtilityAlignmentAcceptingPetriNetScorer {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Score Petri Net Fragment with Utility on Log using Alignments", requiredParameterLabels = {0, 1})
	public UtilityLocalProcessModel run(PluginContext context, AcceptingPetriNet apn, XLog log){
		Map<String, Integer> activityCounts = getActivityCounts(log);
		UtilityLocalProcessModelParameters params = new UtilityLocalProcessModelParameters();
		params.setEvaluationLog(log);
		return evaluateNetOnLog(context, apn, activityCounts, false, params, true);
	}

	public static UtilityLocalProcessModel evaluateNetOnLog(PluginContext context, AcceptingPetriNet apn, Map<String, Integer> activityCountsMap, boolean useMurata, UtilityLocalProcessModelParameters params, boolean verbose){
		int maxLoop = 3;
		Map<Transition, Integer> countsMap = new HashMap<Transition, Integer>();
		Set<List<String>> languageElementsInLog = new HashSet<List<String>>();
		if(useMurata){
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

		Object[] o = projectLogOnNetAlphabet(params.getEvaluationLog(), pn);
		XLog projectedLog = (XLog) o[0];
		double remainedRatio = (double) o[1];
		int maxTraceLength = (int) o[2];
		o = null;
		double avgFitness = 0;
		int seen = 0;
		double avgNumFirings = 0d;
		int backLoopCount = 0;
		int tracesSeen = 0;
		int avgEnabledCounter = 0;
		double avgEnabledTransitions = 0d;
		
		PNMatchInstancesRepResult alignments = LocalProcessModelEvaluator.calculateNAlignment(context, projectedLog, apn, maxTraceLength, backLoop, params, verbose);
		Iterator<AllSyncReplayResult> sync = alignments.iterator();
		double utility = 0d;
		Map<Transition, Double> achievedUtilityMap = new HashMap<Transition, Double>();
		Map<String, Double> potentialUtilityMap = new HashMap<String, Double>();
		while(sync.hasNext()){
			AllSyncReplayResult srr = sync.next();
			int processVariantCount = srr.getTraceIndex().size();
			seen++;
			List<StepTypes> stepTypes = srr.getStepTypesLst().get(0);
			for(Integer traceId : srr.getTraceIndex()){
				XTrace trace = projectedLog.get(traceId);
				int eventId = 0;
				int stepId = 0;
				for(StepTypes step : stepTypes){
					if(step==StepTypes.L || step==StepTypes.LMGOOD){
						double eventUtility = 0d;
						XEvent event = trace.get(eventId);
						XAttribute xa = event.getAttributes().get(params.getUtilityAttributeName());
						if(xa instanceof XAttributeContinuous){
							eventUtility = ((XAttributeContinuous) xa).getValue();
						}else if (xa instanceof XAttributeDiscrete){
							eventUtility = ((XAttributeDiscrete) xa).getValue();								
						}
						
						if(step==StepTypes.LMGOOD){
							utility += eventUtility;
							Object t = srr.getNodeInstanceLst().get(0).get(stepId);
							if(!(t instanceof Transition))
								System.err.println("Something goes wrong here, it is not a transition");
							Double achievedUtility = achievedUtilityMap.get((Transition) t);
							if(achievedUtility==null)
								achievedUtility = 0d;
							achievedUtility += eventUtility;
							achievedUtilityMap.put((Transition) t, achievedUtility);
						}
						Double potentialUtility = potentialUtilityMap.get(event.getAttributes().get("concept:name").toString());
						if(potentialUtility==null)
							potentialUtility = 0d;
						potentialUtility += eventUtility;
						potentialUtilityMap.put(event.getAttributes().get("concept:name").toString(), potentialUtility);
							
						eventId++;
					}
					stepId++;
				}
			}
			List<Object> nodeInstances = srr.getNodeInstanceLst().get(0);
			int alignmentLength = 0;
			int localBackLoopCount = 0;
			List<String> languageElem = new LinkedList<String>();
			Map<Transition, Integer> transitionFireCountInInstance = new HashMap<Transition, Integer>();
			Multiset<Place> currentMarking = HashMultiset.create();
			currentMarking.add(endPlace);

			Set<Transition> enabled = getEnabledTransitions(pn, currentMarking);
			//enabled.remove(backLoop);
			int numEnabled = enabled.size();

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
			//protection against integer overflow of alignments
			if(c<0) // protection against integer overflow in alignments
				return new UtilityLocalProcessModel(apn, languageElementsInLog, languageElementsInLog, countsMap, params);
			double originalLength = srr.getInfo().get(PNMatchInstancesRepResult.ORIGTRACELENGTH);
			double fitness = 1 - (c / Math.max(originalLength, alignmentLength));
			avgFitness = (seen*avgFitness + fitness)/(seen+1);
			if(avgFitness<0) // protection against integer overflow in alignments
				return new UtilityLocalProcessModel(apn, languageElementsInLog, languageElementsInLog, countsMap, params);
			if(alignmentLength>0&&localBackLoopCount>0){
				avgNumFirings = (avgNumFirings * tracesSeen + ((double) alignmentLength)/localBackLoopCount)/(tracesSeen+1);
				tracesSeen++;
			}
			backLoopCount+=localBackLoopCount;
		}

		pn.removeEdge(aIn);
		pn.removeEdge(aOut);
		pn.removeTransition(backLoop);
		apn.setFinalMarkings(finalMarkings);
		apn.setInitialMarking(initialMarking);

		Set<List<String>> petriNetLanguage = calculateLanguage(apn, maxLoop, true);
		languageElementsInLog.retainAll(petriNetLanguage);
		double languageElementsSeenRatio = ((double) languageElementsInLog.size()) / petriNetLanguage.size();
		double avgEnabledTransitionScore = 1d/avgEnabledTransitions;

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

		double harmonicMean = ratios.size()==0 ? 0 : Stat.harmonicMean(Doubles.toArray(ratios));
		UtilityLocalProcessModel scoredAPN = new UtilityLocalProcessModel(apn, harmonicMean, backLoopCount, avgFitness, 0.0, remainedRatio, avgNumFirings, languageElementsSeenRatio, avgEnabledTransitionScore, petriNetLanguage, languageElementsInLog, countsMap, utility, achievedUtilityMap, potentialUtilityMap, params);
		
		Map<String, Integer> logActivityCountsMap = getActivityCounts(params.getEvaluationLog());
		scoredAPN.setLogActivityCountMap(logActivityCountsMap);

		if(verbose)
			System.out.println(scoredAPN.toString());
		
		return scoredAPN; 
	}
}