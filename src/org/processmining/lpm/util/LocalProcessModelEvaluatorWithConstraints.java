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
import java.util.Map.Entry;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
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
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
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
		name = "Score Petri Net Fragment on Log using Alignments (constraints)", 
		parameterLabels = {"Accepting Petri Net", "Input Log"}, 
		returnLabels = {"Scored Accepting Petri Net"}, 
		returnTypes = { LocalProcessModel.class }
		)
public class LocalProcessModelEvaluatorWithConstraints extends LocalProcessModelEvaluator{
	public LocalProcessModelEvaluatorWithConstraints(){
		super();
	}
	
	public LocalProcessModelEvaluatorWithConstraints(LocalProcessModelParameters params) {
		super(params);
	}

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Score Petri Net Fragment on Log using Alignments (constraints)", requiredParameterLabels = {0, 1})
	public LocalProcessModel run(PluginContext context, AcceptingPetriNet apn, XLog log){
		Map<String, Integer> activityCounts = getActivityCounts(log);
		LocalProcessModelParameters params = new LocalProcessModelParameters();
		params.setUseEfficientLog(false);
		params.setEvaluationLog(log);
		params.setDiscoveryLog(log);
		//params.setReturnMurataOrdered(false);
		params.setSmartParameterDefaultsForLog(log);
		LocalProcessModelEvaluator scorer = new LocalProcessModelEvaluatorWithConstraints(params);
		return scorer.evaluateNetOnLog(context, apn, activityCounts, true);
	}

	@Override
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

		Object[] o = projectLogOnNetAlphabet(params.getEvaluationLog(), pn);
		
		Set<String> petriNetAlphabet = new HashSet<String>();
		for(Transition t : pn.getTransitions())
			if(!t.isInvisible())
				petriNetAlphabet.add(t.getLabel());
		
		XLog projectedLog = (XLog) o[0];
		double remainedRatio = (double) o[1];
		int maxTraceLength = (int) o[2];

		double avgFitness = 0;
		int seen = 0;
		double avgNumFirings = 0d;
		int backLoopCount = 0;
		int tracesSeen = 0;
		int avgEnabledCounter = 0;
		double avgEnabledTransitions = 0d;
		
		PNMatchInstancesRepResult alignments = calculateNAlignment(context, projectedLog, apn, maxTraceLength, backLoop, params, verbose);
		Iterator<AllSyncReplayResult> sync = alignments.iterator();
		while(sync.hasNext()){//looping over the alignments
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
			
			int logmovecosts = 0; //to keep track of how many non-model-activities are in between activities of the model per pattern
			int maxlogmovecosts = 0; //states how many logmoves are allowed, now hardcoded at zero, should become input at plugin start
			boolean highlogmovecosts = false; //flag if pattern cost too much
			boolean synchronousSeenSinceLastBackloop = false;
			Integer localcounts; //keeps track of transitions frequency before it is sure that it can be added
			Map<Transition, Integer> localcountsMap = new HashMap<Transition, Integer>(); //keeps track of transitions frequency before it is sure that it can be added
			Integer counts;
			
			for(Integer key : srr.getTraceIndex()){ //looping over each log-trace (non-projected) belonging to the alignment
				XTrace trace = params.getEvaluationLog().get(key);
				int eventIndex = 0;
				for(int i=0; i<nodeInstances.size(); i++){
					boolean goThroughLog = true;
					XEvent event = null;
					String eventLabel = null;
					//Date eventDate = null;
					
					Object nodeInstance = nodeInstances.get(i);
					StepTypes st = stepTypes.get(i);
					
					//Checking if the trace has non-model-activities in between activities of the model per pattern
					while(goThroughLog){
						event = trace.get(eventIndex);
						eventLabel = event.getAttributes().get("concept:name").toString();
						//eventDate = ((XAttributeTimestamp) event.getAttributes().get("time:timestamp")).getValue();
						if(!petriNetAlphabet.contains(eventLabel)){//if eventinlog is NOT in petrinet
							if(synchronousSeenSinceLastBackloop)
								logmovecosts++;
							if(eventIndex<trace.size()-1)
								eventIndex++;
							else
								goThroughLog = false;
							/*
							 * Check NodeInstance (aligment):
							 * If backloop we go through the log for free
							 * If visible transition (sync move) we go through log with cost
							 * If invisible (not backloop) we do not go through the log
							 */
							/*
							if(nodeInstance instanceof Transition){//if NodeInstance is not a logmove
								Transition transition = (Transition) nodeInstance;
								if(!transition.isInvisible() || transition==backLoop){//if sync move or backloop: move through log
									if(eventIndex < trace.size()-1)
										eventIndex++;
									if(transition!=backLoop){ //in case of sync move: add cost
										logmovecosts++; //increase logmovescosts
										if(logmovecosts > maxlogmovecosts){ //flag if some logmovecosts were too high
											highlogmovecosts = true;
										}
									}
								}else{//if invisible but not backloop
									goThroughLog = false;
								}
							}else{ // log move in the alignment
								
							}
							*/
						}else{
							goThroughLog = false;
						}
						
					}
					
					
					if(nodeInstance instanceof Transition){//if NodeInstance is not a logmove
						Transition transition = (Transition) nodeInstance;
						
						
						//arrives here if event IS in petrinet
						//part where SOME COUNT is updated
						if(!transition.isInvisible()){//if sync move
							if(numEnabled>0){
								avgEnabledTransitions = (avgEnabledCounter * avgEnabledTransitions + processVariantCount* numEnabled) / (avgEnabledCounter+processVariantCount);
								avgEnabledCounter+=processVariantCount;
							}
						}
						currentMarking = getMarkingAfterFiring(pn, currentMarking, transition);
						enabled = getEnabledTransitions(pn, currentMarking);
						numEnabled = enabled.size();

						//part where counts for the LPM are updated
						if(!transition.isInvisible()){//if sync move
							synchronousSeenSinceLastBackloop = true;
							languageElem.add(transition.getLabel());
							alignmentLength++;
							localcounts = localcountsMap.get(transition);
							if(localcounts==null)
								localcounts = 0;
							//localcounts+=processVariantCount; //plus 1 for each copy of this trace, processVariantCount is the amount of traces that are the same as the current one
							localcounts++;
							localcountsMap.put(transition, localcounts);
							if(verbose){
								System.out.println("t: "+transition.getLabel()+" __ "+st.name());
								System.out.println("Updated transition "+transition.getLabel()+" with "+processVariantCount+" to "+localcounts);
							}
							if(logmovecosts>maxlogmovecosts)
								highlogmovecosts = true;
							logmovecosts=0;
							if(eventIndex < trace.size()-1)
								eventIndex++;
						}else if(transition.equals(backLoop)){// invisible and backloop	
							if(verbose)
								System.out.println("t: BACKLOOP __ "+st.name());
							synchronousSeenSinceLastBackloop = false;
							boolean fitsMaxLoopConstraint = true;
							for(Integer count : transitionFireCountInInstance.values())
								if(count>maxLoop)
									fitsMaxLoopConstraint = false;
							if(fitsMaxLoopConstraint)		
								languageElementsInLog.add(languageElem);
							languageElem = new LinkedList<String>();
							
							if(highlogmovecosts){
								//no update of countsMap, since this pattern cost too much
								highlogmovecosts = false; //reset flag
							} else{//update of countsMap
								localBackLoopCount+=processVariantCount;
								Iterator<Entry<Transition,Integer>> it = localcountsMap.entrySet().iterator();
								Integer previous = null;
								while (it.hasNext()){
									Entry<Transition,Integer> pair = it.next();
									Transition transit = pair.getKey();
									counts = countsMap.get(transit);
									if(counts==null)
										counts = 0;
									counts+= pair.getValue() * processVariantCount;
									/*
									if(previous!=null && previous!=pair.getValue()){
										System.err.println("Dan is dit de fout");
									}*/
									previous = pair.getValue();
									countsMap.put(transit, counts);

									if(verbose){
										System.out.println("t: "+transit.getLabel()+" __ "+st.name());
										System.out.println("Updated transition "+transit.getLabel()+" with "+processVariantCount+" to "+counts);
									}
								}
								Integer previous2 = null;
								for(Transition t : countsMap.keySet()){
									/*
									if(previous2!=null && countsMap.get(t)!=previous2){
										System.err.println("De fout is al geweest...");
									}*/
									previous2 = countsMap.get(t);
								}
							}
							logmovecosts = 0; //reset of logmovecosts
							localcountsMap.clear();
							
						}else{// invisible but no backloop, should not come here in theory
							if(verbose)
								System.out.println("t: INV __ MINVI");
						}
					}else{ //comes here if NodeInstance contained a log move
						if(verbose){
							XEventClass eventClass = (XEventClass) nodeInstance;
							System.out.println("e: "+eventClass+" __ "+st.name());
						}
						
						//Boolean logmoveloop = true;
						//int nextnode = i + 1;
						//int localcosts = 0;
						
						
						//while(logmoveloop){
							//Object nextnodeInstance; 
							//if(nextnode<nodeInstances.size()){
								//nextnodeInstance = nodeInstances.get(nextnode);
								//if(nextnodeInstance instanceof Transition){//if next NodeInstance is not a logmove
									//Transition nexttransition = (Transition) nextnodeInstance;
									
									//if(nexttransition != backLoop){//if sequential, then add costs. If next would be a backloop, then this activity would not belong to the pattern anymore and no costs should be charged
						if(synchronousSeenSinceLastBackloop)
								logmovecosts++;
						//logmovecosts++;//= localcosts + 1; 
										//if(logmovecosts > maxlogmovecosts){ //flag if some logmovecosts were too high
										//	highlogmovecosts = true;}
									//}
									//logmoveloop = false;
								//}else{//if next NodeInstance is a logmove 
									//localcosts++; 
						if(eventIndex < trace.size()-1)
							eventIndex++;
									//if(nextnode<nodeInstances.size()){
										//nextnode++;
									//}else{//last node instance is seen
										//logmoveloop = false;
										//logmovecosts+= localcosts;
								//	}
								//}
							//}else{
								//logmoveloop = false;//else this is the last activity and thus the activity does not belong to the pattern anymore
							
						//}
						//}
					}
				}
				// add the counts (is not done before since the process does not finish with a backloop)
				if(highlogmovecosts == false){
					if(newInitialAndFinalMarking.containsAll(currentMarking)){
						localBackLoopCount+=processVariantCount;
						Iterator<Entry<Transition,Integer>> it = localcountsMap.entrySet().iterator();
						Integer previous = null;
						while (it.hasNext()){
							Entry<Transition,Integer> pair = it.next();
							Transition transit = pair.getKey();
							counts = countsMap.get(transit);
							if(counts==null)
								counts = 0;
							counts+= pair.getValue() * processVariantCount;
							/*
							if(previous!=null && previous!=pair.getValue()){
								System.err.println("Dan is dit de fout");
							}*/
							countsMap.put(transit, counts);
							previous = pair.getValue();
							if(verbose){
								//System.out.println("t: "+transit.getLabel();//+" __ "+st.name());
								System.out.println("Updated transition "+transit.getLabel()+" with "+processVariantCount+" to "+counts);
							}
						}
						Integer previous2 = null;
						for(Transition t : countsMap.keySet()){
							/*
							if(previous2!=null && countsMap.get(t)!=previous2){
								System.err.println("De fout is al geweest...");
							}*/
							previous2 = countsMap.get(t);
						}
					}
				}
				localcountsMap.clear();
				logmovecosts=0;
			}
			Double c = srr.getInfo().get(PNMatchInstancesRepResult.MAXFITNESSCOST);

			if(verbose){
				System.out.println("cost: "+c);
				System.out.println();
			}
			
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
		LocalProcessModel scoredAPN = new LocalProcessModel(apn, harmonicMean, backLoopCount, avgFitness, 0.0, remainedRatio, avgNumFirings, languageElementsSeenRatio, avgEnabledTransitionScore, petriNetLanguage, languageElementsInLog, countsMap, params);
	
		Map<String, Integer> logActivityCountsMap = getActivityCounts(params.getEvaluationLog());
		scoredAPN.setLogActivityCountMap(logActivityCountsMap);

		if(verbose)
			System.out.println(scoredAPN.toString());
		
		return scoredAPN; 
	}
}