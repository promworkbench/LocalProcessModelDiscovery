package org.processmining.lpm.discovery;

import static org.processmining.lpm.util.PetrinetUtils.DUMMY;
import static org.processmining.lpm.util.PetrinetUtils.transitionToEventClassMapperByLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
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
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayer.matchinstances.PNLogMatchInstancesReplayer;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;

import nl.tue.astar.AStarException;

@Plugin(
		name = "Calculate Log Episodes of Local Process Model", 
		parameterLabels = {"Local Process Model", "Event Log"}, 
		returnLabels = {"Event Log with Event Attributes"}, 
		returnTypes = { XLog.class }
		)
public class CalculateLogEpisodes {
	protected Map<String,XLog> efficientLogCache;
	protected EfficientEventSet ees;
	protected LocalProcessModelParameters params;
	
	public CalculateLogEpisodes(){}
	
	public CalculateLogEpisodes(LocalProcessModelParameters params){
		this.efficientLogCache = java.util.Collections.synchronizedMap(new FifoHashMap<String,XLog>(params.getEfficientLogCacheSize()));
		this.params = params;
		this.ees = new EfficientEventSet(params.getEvaluationLog());
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Calculate Log Episodes of Local Process Model", requiredParameterLabels = {0, 1})
	public XLog run(PluginContext context, AcceptingPetriNet apn, XLog log){
		LocalProcessModelParameters params = new LocalProcessModelParameters();
		params.setEvaluationLog(log);
		params.setUseEfficientLog(false);
		params.setReturnMurataOrdered(false);
		CalculateLogEpisodes scorer = new CalculateLogEpisodes(params);
		return scorer.evaluateNetOnLog(context, apn);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Calculate Log Episodes of Local Process Model", requiredParameterLabels = {0, 1})
	public XLog run(PluginContext context, LocalProcessModel lpm, XLog log){
		LocalProcessModelParameters params = new LocalProcessModelParameters();
		params.setEvaluationLog(log);
		params.setUseEfficientLog(false);
		params.setReturnMurataOrdered(false);
		CalculateLogEpisodes scorer = new CalculateLogEpisodes(params);
		return scorer.evaluateNetOnLog(context, lpm.getAcceptingPetriNet());
	}

	public XLog evaluateNetOnLog(PluginContext context, AcceptingPetriNet apn){
		AcceptingPetriNet apnClone = cloneAcceptingPetriNet(apn);

		if(params.isReturnMurataOrdered()){
			ReduceUsingMurataRulesPlugin murata = new ReduceUsingMurataRulesPlugin();
			ReduceUsingMurataRulesParameters murataParams = new ReduceUsingMurataRulesParameters();
			murataParams.setRetainBehavior(true);
			murataParams.setTryConnections(false);
			apnClone =  murata.run(context, apn, murataParams);
		}
		
		Petrinet pn = apnClone.getNet();
		Transition backLoop = pn.addTransition("backloop");
		backLoop.setInvisible(true);
		Place endPlace = new ArrayList<Marking>(apnClone.getFinalMarkings()).get(0).toList().get(0);
		Place startPlace = new ArrayList<Place>(apnClone.getInitialMarking().baseSet()).get(0);
		pn.addArc(endPlace, backLoop);
		pn.addArc(backLoop, startPlace);
		Set<Marking> newInitialAndFinalMarkings = new HashSet<Marking>();
		Marking newInitialAndFinalMarking = new Marking();
		newInitialAndFinalMarking.add(endPlace);
		newInitialAndFinalMarkings.add(newInitialAndFinalMarking);
		apnClone.setInitialMarking(newInitialAndFinalMarking);
		apnClone.setFinalMarkings(newInitialAndFinalMarkings);
		
		int maxTraceLength = 0;
		XLog projectedLog = (XLog) params.getEvaluationLog().clone();
		PNMatchInstancesRepResult alignments = null;

		XLog returnLog = newEmptyLog();
		try{
			alignments = calculateNAlignment(context, projectedLog, apnClone, maxTraceLength, backLoop, params);
		}catch(NullPointerException e){}
		if(alignments!=null){
			Iterator<AllSyncReplayResult> sync = alignments.iterator();
			while(sync.hasNext()){
				AllSyncReplayResult srr = sync.next();
				Set<XTrace> traces = new HashSet<XTrace>();
				for(Integer i : srr.getTraceIndex()){
					traces.add(params.getEvaluationLog().get(i));
				}
				List<Object> nodeInstances = srr.getNodeInstanceLst().get(0);
	
				int eventID = 0;
				Map<XTrace, XEvent> lastEvent = new HashMap<XTrace, XEvent>();
				for(int i=0; i<nodeInstances.size(); i++){
					Object nodeInstance = nodeInstances.get(i);
					if(nodeInstance instanceof Transition){
						Transition transition = (Transition) nodeInstance;
						if(!transition.isInvisible()){
							for(XTrace trace : traces){
								lastEvent.put(trace, trace.get(eventID));
								trace.get(eventID).getAttributes().put("part_of_LPM_instance", new XAttributeBooleanImpl("part_of_LPM_instance", true));
							}
							eventID++;
						}else{
							if(transition==backLoop){
								for(XTrace trace : traces){
									if(lastEvent.containsKey(trace)){
										lastEvent.get(trace).getAttributes().put("last_of_LPM_instance", new XAttributeBooleanImpl("last_of_LPM_instance", true));
									}
								}
							}
						}
					}else{
						for(XTrace trace : traces){
							trace.get(eventID).getAttributes().put("part_of_LPM_instance", new XAttributeBooleanImpl("part_of_LPM_instance", false));
						}
						eventID++;
					}
				}
				for(XTrace trace : traces){
					if(lastEvent.containsKey(trace)){
						lastEvent.get(trace).getAttributes().put("last_of_LPM_instance", new XAttributeBooleanImpl("last_of_LPM_instance", true));
					}
					returnLog.add(trace);
				}
			}
		}

		return returnLog; 
	}

	public static PNMatchInstancesRepResult calculateNAlignment(PluginContext context, XLog log, AcceptingPetriNet apn, int maxTraceLength, Transition backloop, LocalProcessModelParameters params){
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
		//parameter.setEpsilon(0.1);

		parameter.setCreateConn(false);

		Marking initialMarking = apn.getInitialMarking();
		Marking finalMarking = new ArrayList<Marking>(apn.getFinalMarkings()).get(0);

		parameter.setMaxNumOfStates(params.getAlignmentMaxNumStatesPerTransition() * apn.getNet().getTransitions().size());
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
		logInfo = null;
		return pnRepResult;
	}
	
	public static AcceptingPetriNet cloneAcceptingPetriNet(AcceptingPetriNet apn){
		Petrinet pn = apn.getNet();
		Petrinet clone = new PetrinetImpl("Clone of "+pn.getLabel());
		
		// Clone transitions
		Map<Transition, Transition> tMap = new HashMap<Transition, Transition>();
		for(Transition tOld: pn.getTransitions()){
			Transition tNew = clone.addTransition(tOld.getLabel());
			if(tOld.isInvisible())
				tNew.setInvisible(true);
			tMap.put(tOld, tNew);
		}
		
		// Clone places
		Map<Place, Place> pMap = new HashMap<Place, Place>();
		for(Place pOld: pn.getPlaces()){
			Place pNew = clone.addPlace(pOld.getLabel());
			pMap.put(pOld, pNew);
		}
		
		for(Transition tOld: pn.getTransitions()){
			for(PetrinetEdge eOld: pn.getInEdges(tOld)){
				Place pOld = (Place) eOld.getSource();
				clone.addArc(pMap.get(pOld), tMap.get(tOld));
			}
			for(PetrinetEdge eOld: pn.getOutEdges(tOld)){
				Place pOld = (Place) eOld.getTarget();
				clone.addArc(tMap.get(tOld), pMap.get(pOld));
			}
		}
		
		// Clone Initial Marking
		Marking initialMarkingOld = apn.getInitialMarking();
		Set<Place> initialMarkingNetSet = new HashSet<Place>();
		for(Place pOld: initialMarkingOld)
			initialMarkingNetSet.add(pMap.get(pOld));
		Marking initialMarkingNew = new Marking(initialMarkingNetSet);
		
		// Clone Final Markings
		Set<Marking> finalMarkingsOld = apn.getFinalMarkings();
		Set<Marking> finalMarkingsNew = new HashSet<Marking>();
		for(Marking finalMarkingOld : finalMarkingsOld){
			Set<Place> finalMarkingNetSet = new HashSet<Place>();
			for(Place pOld: finalMarkingOld)
				finalMarkingNetSet.add(pMap.get(pOld));
			finalMarkingsNew.add(new Marking(finalMarkingNetSet));
		}
		
		return new AcceptingPetriNetImpl(clone, initialMarkingNew, finalMarkingsNew);
	}
	
	public static Petrinet clonePetriNet(Petrinet pn){
		Petrinet clone = new PetrinetImpl("Clone of "+pn.getLabel());
		
		// Clone transitions
		Map<Transition, Transition> tMap = new HashMap<Transition, Transition>();
		for(Transition tOld: pn.getTransitions()){
			Transition tNew = clone.addTransition(tOld.getLabel());
			tMap.put(tOld, tNew);
		}
		
		// Clone places
		Map<Place, Place> pMap = new HashMap<Place, Place>();
		for(Transition pOld: pn.getTransitions()){
			Transition pNew = clone.addTransition(pOld.getLabel());
			tMap.put(pOld, pNew);
		}
		
		for(Transition tOld: pn.getTransitions()){
			for(PetrinetEdge eOld: pn.getInEdges(tOld)){
				Place pOld = (Place) eOld.getSource();
				clone.addArc(pMap.get(pOld), tMap.get(tOld));
			}
			for(PetrinetEdge eOld: pn.getOutEdges(tOld)){
				Place pOld = (Place) eOld.getTarget();
				clone.addArc(tMap.get(tOld), pMap.get(pOld));
			}
		}
		return clone;
	}
	
	public static XLog newEmptyLog(){
		return XFactoryRegistry.instance().currentDefault().createLog();
	}
}