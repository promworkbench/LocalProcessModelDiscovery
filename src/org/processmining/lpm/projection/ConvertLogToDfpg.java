package org.processmining.lpm.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLogImpl;
import org.processmining.plugins.InductiveMiner.mining.logs.IMTrace;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ConvertLogToDfpg {

	@Plugin(name = "Convert log to directly-follows-precedes graph", returnLabels = { "Directly-follows-precedes graph" }, returnTypes = { Dfpg.class }, parameterLabels = { "Log" }, userAccessible = true, help = "Convert a log into a directly-follows-precedes graph.")
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N.Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	public Dfpg createDfpg(PluginContext context, XLog log) {
		return log2Dfpg(log);
	}

	public static Dfpg log2Dfpg(XLog log) {
		//initialise, read the log
		Dfpg dfpg = new Dfpg();
		XEventNameClassifier classifier = new XEventNameClassifier();
		IMLog imLog = new IMLogImpl(log, classifier, MiningParameters.getDefaultLifeCycleClassifier());

		TObjectIntHashMap<XEventClass> minimumSelfDistances = new TObjectIntHashMap<>();
		THashMap<XEventClass, MultiSet<XEventClass>> minimumSelfDistancesBetween = new THashMap<XEventClass, MultiSet<XEventClass>>();
		long numberOfEpsilonTraces = 0;

		XEventClass fromEventClass;
		XEventClass toEventClass;

		//walk trough the log
		Map<XEventClass, Integer> eventSeenAt;
		List<XEventClass> readTrace;

		for (IMTrace trace : imLog) {
			toEventClass = null;
			fromEventClass = null;

			int traceSize = 0;
			eventSeenAt = new THashMap<XEventClass, Integer>();
			readTrace = new ArrayList<XEventClass>();

			for (XEvent e : trace) {
				XEventClass ec = imLog.classify(trace, e);
				dfpg.addActivity(ec);

				fromEventClass = toEventClass;
				toEventClass = ec;

				readTrace.add(toEventClass);

				if (eventSeenAt.containsKey(toEventClass)) {
					//we have detected an activity for the second time
					//check whether this is shorter than what we had already seen
					int oldDistance = Integer.MAX_VALUE;
					if (minimumSelfDistances.containsKey(toEventClass)) {
						oldDistance = minimumSelfDistances.get(toEventClass);
					}

					if (!minimumSelfDistances.containsKey(toEventClass)
							|| traceSize - eventSeenAt.get(toEventClass) <= oldDistance) {
						//keep the new minimum self distance
						int newDistance = traceSize - eventSeenAt.get(toEventClass);
						if (oldDistance > newDistance) {
							//we found a shorter minimum self distance, record and restart with a new multiset
							minimumSelfDistances.put(toEventClass, newDistance);

							minimumSelfDistancesBetween.put(toEventClass, new MultiSet<XEventClass>());
						}

						//store the minimum self-distance activities
						MultiSet<XEventClass> mb = minimumSelfDistancesBetween.get(toEventClass);
						mb.addAll(readTrace.subList(eventSeenAt.get(toEventClass) + 1, traceSize));
					}
				}
				eventSeenAt.put(toEventClass, traceSize);
				{
					if (fromEventClass != null) {
						//add edge to directly-follows graph
						dfpg.addEdge(fromEventClass, toEventClass, 1);
					} else {
						//add edge to start activities
						dfpg.addStartActivity(toEventClass, 1);
					}
				}

				traceSize += 1;
			}

			if (toEventClass != null) {
				dfpg.addEndActivity(toEventClass, 1);
			}

			if (traceSize == 0) {
				numberOfEpsilonTraces = numberOfEpsilonTraces + 1;
			}
		}
		
		return dfpg;
	}
}