/**
 * 
 */
package org.processmining.lpm.adjustedalignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.lpm.adjustedalignments.AStarThread.Canceller;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.matchinstances.InfoObjectConst;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.express.NBestAlignmentsAlg;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import nl.tue.astar.AStarException;
import nl.tue.astar.Record;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.DijkstraTail;

/**
 * @author aadrians
 * Mar 15, 2013
 *
 */
public class NBestOptAlignmentsNoModelMoveGraphSamplingAlg extends AllOptAlignmentsGraphSamplingAlg implements NBestAlignmentsAlg {
	// additional attribute: the number of expected best alignments
	protected int expectedAlignments = 1;
	
	public String toString() {
		return "Graph-based state space replay to obtain N-best representative optimal alignments (not necessarily optimal)";
	}

	public String getHTMLInfo() {
		return "<html>Returns N-best representatives of optimal alignments using graph-based state space (with state sorting). <br/>" +
				"Assuming that the model does not allow loop/infinite firing sequences of cost 0.</html>";
	};
	
	@SuppressWarnings("unchecked")
	public PNMatchInstancesRepResult replayLog(final PluginContext context, PetrinetGraph net, Marking initMarking, Marking finalMarking, final XLog log, final TransEvClassMapping mapping, Object[] parameters) throws AStarException {
		this.initMarking = initMarking;
		this.finalMarkings = new Marking[] { finalMarking };

		mapTrans2Cost = (Map<Transition, Integer>) parameters[MAPTRANSTOCOST];
		mapEvClass2Cost = (Map<XEventClass, Integer>) parameters[MAPXEVENTCLASSTOCOST];
		maxNumOfStates = (Integer) parameters[MAXEXPLOREDINSTANCES];
		expectedAlignments = (Integer) parameters[3];

		classifier = mapping.getEventClassifier();

		XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		final XEventClasses classes = summary.getEventClasses();

		final int delta = 1000; // TODO: check effect van dit tunen

		// for sake of compatibility, insert cost of dummy xevent class
		mapEvClass2Cost.put(mapping.getDummyEventClass(), 0);

		final PDelegate delegate = getDelegate(net, log, classes, mapping, mapTrans2Cost, mapEvClass2Cost, delta, false, finalMarkings);

		final MemoryEfficientAStarAlgorithm<PHead, DijkstraTail> aStar = new MemoryEfficientAStarAlgorithm<PHead, DijkstraTail>(delegate);

		final List<MatchInstancesGraphRes> results = new ArrayList<MatchInstancesGraphRes>();

		final TIntIntMap doneMap = new TIntIntHashMap();

		TObjectIntMap<Trace> traces = new TObjectIntHashMap<Trace>(log.size() / 2, 0.5f, -1);

		final List<AllSyncReplayResult> col = new ArrayList<AllSyncReplayResult>();

		for (int i = 0; i < log.size(); i++) {
			if (context != null) {
				if (context.getProgress().isCancelled()) {
					break;
				}
			}
			PHead initial = new PHead(delegate, initMarking, log.get(i));
			final Trace trace = getLinearTrace(log, i, delegate);
			int first = traces.get(trace);
			if (first >= 0) {
				doneMap.put(i, first);
				continue;
			} else {
				traces.put(trace, i);
			}
			final AllSamplingOptAlignmentsGraphThread<PHead, DijkstraTail> thread = getThread(aStar, initial, trace, maxNumOfStates);

			final int j = i;
			
			MatchInstancesGraphRes result = new MatchInstancesGraphRes();
			result.trace = j;
			result.filteredTrace = trace;

			Canceller c = new Canceller() {
				public boolean isCancelled() {
					if (context != null) {
						return context.getProgress().isCancelled();
					}
					return false;
				}
			};

			PRecord record = (PRecord) thread.getOptimalRecord(c);
			result.reliable = thread.wasReliable();
			result.addRecord(record);
			if (result.reliable) {
				int counter = 1;
				
				// check if there are other optimal alignments that only shares final marking
				Map<Record, List<Record>> mapSuffix = thread.getMapToStatesWSameSuffix();
				List<Record> listRecords = mapSuffix.get(record);
				if (listRecords != null){
					counter += listRecords.size();
				}
				
				while (counter < expectedAlignments) {
					record = (PRecord) thread.getOptimalRecord(c);
					if (thread.wasReliable()) {
						result.addRecord(record);
						
						// check if there are other optimal alignments that only shares final marking
						mapSuffix = thread.getMapToStatesWSameSuffix();
						listRecords = mapSuffix.get(record);
						if (listRecords != null){
							counter += listRecords.size();
						}
						counter++;
					} else {
						break;
					}
				}
				result.mapRecordToSameSuffix = thread.getMapToStatesWSameSuffix();
				result.reliable = thread.getVisitedStateCount() < maxNumOfStates;
			}

			visitedStates += thread.getVisitedStateCount();
			queuedStates += thread.getQueuedStateCount();

			result.queuedStates = thread.getQueuedStateCount();
			result.states = thread.getVisitedStateCount();

			results.add(result);
		}
		

		long maxStateCount = 0;
		for (MatchInstancesGraphRes r : results) {
			XTrace trace = log.get(r.trace);
			int states = addReplayResults(delegate, trace, r, doneMap, log, col, r.trace, null,
					new LinkedList<Object>(), new LinkedList<StepTypes>());
			maxStateCount = Math.max(maxStateCount, states);
		}
		maxStateCount *= 56;
		
		summary = null;

		synchronized (col) {
			PNMatchInstancesRepResult res = new PNMatchInstancesRepResult(col);
			return res;
		}
	}
	/**
	 * This method needs to be overridden because there is no need to compute the number of all optimal alignments
	 * as it is computed in the parent class
	 */
	@Override
	protected AllSyncReplayResult recordToResult(PDelegate d, XTrace trace, Trace filteredTrace, Collection<PRecord> records, int traceIndex, int states, int queuedStates, boolean isReliable, long milliseconds, final List<Object> suffixNodeInstance, final List<StepTypes> suffixStepTypes, AllSyncReplayResult prevResult, Map<Record, List<Record>> mapToStatesWSameSuffix) {
		List<List<Object>> lstNodeInstanceLst = new ArrayList<List<Object>>(records.size());
		List<List<StepTypes>> lstStepTypesLst = new ArrayList<List<StepTypes>>(records.size());

		double minCost = Double.NaN;
		double maxCost = Double.NaN;
		double cost = 0;
		int counter = 0;
		
		List<Integer> numRepresented = new LinkedList<Integer>(); // record of number alignments represented

		Iterator<PRecord> allIt = records.iterator();
		while ((allIt.hasNext())&&(counter < expectedAlignments)){
			PRecord r = allIt.next(); 

			// uncomment this to get more optimal alignments
			// start block -----------------------------------
			// compute the number of represented alignments
			//PRecord pred = r.getPredecessor(); // the main state
			int numRepresentedBySample = 1 + 1;//countOptimalAlignments(pred, mapToStatesWSameSuffix);
			numRepresented.add(numRepresentedBySample); // add number of alignment the sample represents
			cost = constructResult(r, d, trace, filteredTrace, true, lstNodeInstanceLst, lstStepTypesLst);
			counter++;
			if (Double.compare(Double.NaN, minCost) == 0) {
				minCost = cost;
			} else if (Double.compare(minCost, cost) > 0) {
				minCost = cost;
			}

			if (Double.compare(Double.NaN, maxCost) == 0) {
				maxCost = cost;
			} else if (Double.compare(maxCost, cost) < 0) {
				maxCost = cost;
			}
			
			// since we must get samples, get all records that directly visit the final state
			List<Record> otherPaths = mapToStatesWSameSuffix.get(r);
			if (otherPaths != null) {
				Iterator<Record> it = otherPaths.iterator();
				while ((counter < expectedAlignments)&&(it.hasNext())){
					PRecord rec = (PRecord) it.next();
					numRepresentedBySample = 1 + 1;//countOptimalAlignments(rec, mapToStatesWSameSuffix);
					numRepresented.add(numRepresentedBySample); // add number of alignment the sample represents
					cost = constructResult(rec, d, trace, filteredTrace, false, lstNodeInstanceLst,
							lstStepTypesLst);
					counter++;
					if (Double.compare(Double.NaN, minCost) == 0) {
						minCost = cost;
					} else if (Double.compare(minCost, cost) > 0) {
						minCost = cost;
					}

					if (Double.compare(Double.NaN, maxCost) == 0) {
						maxCost = cost;
					} else if (Double.compare(maxCost, cost) < 0) {
						maxCost = cost;
					}
				}
			}
		}
		
		AllSyncReplayResult res = new AllSyncReplayResult(lstNodeInstanceLst, lstStepTypesLst, traceIndex, isReliable);
		res.addInfoObject(InfoObjectConst.NUMREPRESENTEDALIGNMENT, numRepresented);
		
		// set infos
		res.addInfo(PNMatchInstancesRepResult.MINFITNESSCOST, minCost);
		res.addInfo(PNMatchInstancesRepResult.MAXFITNESSCOST, maxCost);
		res.addInfo(PNMatchInstancesRepResult.NUMSTATES, (double) states);
		res.addInfo(PNMatchInstancesRepResult.QUEUEDSTATE, (double) queuedStates);
		res.addInfo(PNMatchInstancesRepResult.ORIGTRACELENGTH, (double) trace.size());
		
		return res;
	}
	
}
