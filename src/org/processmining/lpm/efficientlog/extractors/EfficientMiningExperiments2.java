package org.processmining.lpm.efficientlog.extractors;

import java.util.concurrent.atomic.AtomicInteger;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.discovery.LocalProcessModelDiscovery;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(
		name = "Run Efficient LPM Mining Experiments (Events)", 
		parameterLabels = {"Input Log"}, 
	    returnLabels = {"Local Process Model Ranking"}, 
	    returnTypes = { LocalProcessModelRanking.class }
		)
public class EfficientMiningExperiments2 {
	LocalProcessModelParameters params;
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Run Efficient LPM Mining Experiments (Events)", requiredParameterLabels = {0})
	public LocalProcessModelRanking bruteForceProcessTreeEpisode(PluginContext context, XLog log) {
		params = new LocalProcessModelParameters();
		params.setDiscoveryLog(log);
		params.setEvaluationLog(log);
		params.setVerbose(false);
		params.setSmartParameterDefaultsForDiscoveryLog();
		//params.setProjectionMethod(ProjectionMethods.None);
		LocalProcessModelDiscovery lpmd_temp = new LocalProcessModelDiscovery();
		// do the run without constraints
		long startTime = System.currentTimeMillis();
		lpmd_temp.runHeadless(context, params);
		long endTime = System.currentTimeMillis();
		long duration = endTime-startTime;
		System.out.println("None, -, "+duration+", "+params.getNumberOfExploredLpms().get());
		for(int consecutiveNonFitting = 0; consecutiveNonFitting <= 10; consecutiveNonFitting++){	
			// do the run using algorithm 3
			lpmd_temp = new LocalProcessModelDiscovery();
			params.setUseEfficientLog(true);
			params.setEfficientLogExtractor(new EventGapExtractor(consecutiveNonFitting, -1));
			params.setEfficientLogCacheSize(0);
			params.setNumberOfExploredLpms(new AtomicInteger(0));
			startTime = System.currentTimeMillis();
			lpmd_temp.runHeadless(context, params);
			endTime = System.currentTimeMillis();
			duration = endTime-startTime;
			System.out.println("Alg. 3, "+consecutiveNonFitting+", "+duration+", "+params.getNumberOfExploredLpms().get());
			
			// do the run using algorithm 3 + caching
			lpmd_temp = new LocalProcessModelDiscovery();
			params.setUseEfficientLog(true);
			params.setEfficientLogExtractor(new EventGapExtractor(consecutiveNonFitting, -1));
			params.setEfficientLogCacheSize(1000000);
			params.setNumberOfExploredLpms(new AtomicInteger(0));
			startTime = System.currentTimeMillis();
			lpmd_temp.runHeadless(context, params);
			endTime = System.currentTimeMillis();
			duration = endTime-startTime;
			System.out.println("Alg. 3 + caching, "+consecutiveNonFitting+", "+duration+", "+params.getNumberOfExploredLpms().get());
		}
		return null;
	}
}