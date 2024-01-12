/**
 * 
 */
package org.processmining.lpm.adjustedalignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.lpm.adjustedalignments.AStarThread.ASynchronousMoveSorting;
import org.processmining.models.graphbased.directed.petrinet.InhibitorNet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.ResetInhibitorNet;
import org.processmining.models.graphbased.directed.petrinet.ResetNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.annotations.PNReplayMultipleAlignmentAlgorithm;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;

import nl.tue.astar.AStarException;
import nl.tue.astar.Record;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.DijkstraTail;

/**
 * @author aadrians Mar 3, 2013
 * 
 */
@PNReplayMultipleAlignmentAlgorithm
public abstract class AllOptAlignmentsGraphSamplingAlg extends AbstractAllOptAlignmentsGraphAlg<PDelegate, DijkstraTail> {
	public abstract String toString();

	@Override
	public abstract String getHTMLInfo();

	/**
	 * Since we only need samples, sorting may take place
	 * 
	 * @throws AStarException
	 */
	protected AllSamplingOptAlignmentsGraphThread<PHead, DijkstraTail> getThread(MemoryEfficientAStarAlgorithm<PHead, DijkstraTail> aStar, PHead initial, Trace trace, int maxNumOfStates) throws AStarException {
		AllSamplingOptAlignmentsGraphThread<PHead, DijkstraTail> thread = new AllSamplingOptAlignmentsGraphThread.MemoryEfficient<PHead, DijkstraTail>(aStar, initial, trace, maxNumOfStates);
		thread.setASynchronousMoveSorting(ASynchronousMoveSorting.LOGMOVEFIRST);
		return thread;
	}

	public abstract PNMatchInstancesRepResult replayLog(final PluginContext context, PetrinetGraph net, Marking initMarking, Marking finalMarking, final XLog log, final TransEvClassMapping mapping, Object[] parameters) throws AStarException;
	
	protected abstract AllSyncReplayResult recordToResult(PDelegate d, XTrace trace, Trace filteredTrace, Collection<PRecord> records, int traceIndex, int states, int queuedStates, boolean isReliable, long milliseconds, final List<Object> suffixNodeInstance, final List<StepTypes> suffixStepTypes, AllSyncReplayResult prevResult, Map<Record, List<Record>> mapToStatesWSameSuffix);
	
	protected double constructResult(PRecord r, PDelegate d, XTrace trace, Trace filteredTrace, boolean isFirst, List<List<Object>> lstNodeInstanceLst, List<List<StepTypes>> lstStepTypesLst) {
		double cost = 0.00;
		List<PRecord> history = PRecord.getHistory(r); // this is only a single history
		int eventInTrace = -1;
		List<StepTypes> stepTypes = new ArrayList<StepTypes>(history.size());
		List<Object> nodeInstance = new ArrayList<Object>();
		for (PRecord rec : history) {
			if (rec.getMovedEvent() == AStarThread.NOMOVE) {
				// move model only
				Transition t = d.getTransition((short) rec.getModelMove());
				if (t.isInvisible()) {
					stepTypes.add(StepTypes.MINVI);
				} else {
					stepTypes.add(StepTypes.MREAL);
				}
				nodeInstance.add(t);
				if (isFirst) {
					cost += (d.getCostForMoveModel((short) rec.getModelMove()) - d.getEpsilon()) / d.getDelta();
				}
			} else {
				// a move occurred in the log. Check if class aligns with class in trace
				short a = (short) filteredTrace.get(rec.getMovedEvent());
				eventInTrace++;
				XEventClass clsInTrace = d.getClassOf(trace.get(eventInTrace));
				while (d.getIndexOf(clsInTrace) != a) {
					// The next event in the trace is not of the same class as the next event in the A-star result.
					// This is caused by the class in the trace not being mapped to any transition.
					// move log only
					stepTypes.add(StepTypes.L);
					nodeInstance.add(clsInTrace);
					if (isFirst) {
						cost += mapEvClass2Cost.get(clsInTrace);
					}
					eventInTrace++;
					clsInTrace = d.getClassOf(trace.get(eventInTrace));
				}
				if (rec.getModelMove() == AStarThread.NOMOVE) {
					// move log only
					stepTypes.add(StepTypes.L);
					nodeInstance.add(d.getEventClass(a));
					if (isFirst) {
						cost += (d.getCostForMoveLog(a) - d.getEpsilon()) / d.getDelta();
					}
				} else {
					// sync move
					stepTypes.add(StepTypes.LMGOOD);
					nodeInstance.add(d.getTransition((short) rec.getModelMove()));
					if (isFirst) {
						cost += (d.getCostForMoveSync((short) rec.getModelMove()) - d.getEpsilon()) / d.getDelta();
					}
				}
			}
		}

		// add the rest of the trace
		eventInTrace++;
		while (eventInTrace < trace.size()) {
			// move log only
			XEventClass a = d.getClassOf(trace.get(eventInTrace++));
			stepTypes.add(StepTypes.L);
			nodeInstance.add(a);
			if (isFirst) {
				cost += mapEvClass2Cost.get(a);
			}
		}

		lstNodeInstanceLst.add(nodeInstance);
		lstStepTypesLst.add(stepTypes);
		return cost;
	}

	protected int countOptimalAlignments(Record r, Map<Record, List<Record>> mapToStatesWSameSuffix) {
		if (r == null) {
			return 0;
		} else {
			int result = 0;
			List<Record> otherStates = mapToStatesWSameSuffix.get(r);
			if (otherStates != null) {
				for (Record rec : otherStates) {
					result += 1 + countOptimalAlignments(rec.getPredecessor(), mapToStatesWSameSuffix);
				}
			}
			result += countOptimalAlignments(r.getPredecessor(), mapToStatesWSameSuffix);
			return result;
		}
	}

	protected PDelegate getDelegate(PetrinetGraph net, XLog log, XEventClasses classes, TransEvClassMapping map,
			Map<Transition, Integer> mapTrans2Cost, Map<XEventClass, Integer> mapEvClass2Cost, int delta,
			boolean allMarkingsAreFinal, Marking[] finalMarkings) {
		PDelegate d = null;
		if (net instanceof ResetInhibitorNet) {
			d = new PDelegate((ResetInhibitorNet) net, log, classes, map, mapTrans2Cost, mapEvClass2Cost, delta,
					allMarkingsAreFinal, finalMarkings);
		} else if (net instanceof ResetNet) {
			d = new PDelegate((ResetNet) net, log, classes, map, mapTrans2Cost, mapEvClass2Cost, delta,
					allMarkingsAreFinal, finalMarkings);
		} else if (net instanceof InhibitorNet) {
			d = new PDelegate((InhibitorNet) net, log, classes, map, mapTrans2Cost, mapEvClass2Cost, delta,
					allMarkingsAreFinal, finalMarkings);
		} else if (net instanceof Petrinet) {
			d = new PDelegate((Petrinet) net, log, classes, map, mapTrans2Cost, mapEvClass2Cost, delta,
					allMarkingsAreFinal, finalMarkings);
		}
		if (d != null) {
			d.setEpsilon(0);
		}
		return d;
	}
}
