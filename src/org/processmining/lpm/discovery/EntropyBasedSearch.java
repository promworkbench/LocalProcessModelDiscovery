package org.processmining.lpm.discovery;

import static org.processmining.lpm.util.LogUtils.projectLogOnEventNames;

import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.projection.RelativeEntropyBasedProjector;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.LocalProcessModelTopSet;

@Plugin(
		name = "Search Local Process Models using Entropy", 
		parameterLabels = {"Input Log"}, 
	    returnLabels = {"Local Process Model Ranking"}, 
	    returnTypes = { LocalProcessModelRanking.class }
		)
public class EntropyBasedSearch {
	LocalProcessModelParameters params;
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Search Local Process Models using Entropy", requiredParameterLabels = {0})
	public LocalProcessModelRanking bruteForceProcessTreeEpisode(PluginContext context, XLog log) {
		params = params.copy();
		params.setDiscoveryLog(log);
		params.setEvaluationLog(log);
		return runHeadless(context, params, new LocalProcessModelDiscovery()); // uses default parameters
	}

	public static LocalProcessModelRanking runHeadless(PluginContext context, LocalProcessModelParameters params, LocalProcessModelDiscovery processor) {
		RelativeEntropyBasedProjector e = new RelativeEntropyBasedProjector(-0.1);
		Set<Set<String>> projections = e.getProjections(params.getDiscoveryLog());

		LocalProcessModelTopSet container = new LocalProcessModelTopSet(params.getTop_k());
		if(params.isVerbose())
			System.out.println("Projections: ");
		for(Set<String> projection : projections){
			if(projection.size()==1)
				continue;
			System.out.println(projection);
			XLog discoveryLog = projectLogOnEventNames(params.getEvaluationLog(), projection);
			params.setDiscoveryLog(discoveryLog);
			LocalProcessModelRanking result = processor.runHeadlessProjectionless(context, params);
			for(int i=0; i<result.getSize(); i++){
				container.add(result.getNet(i));
			}
		}
		LocalProcessModelRanking array = new LocalProcessModelRanking();
		for(LocalProcessModel net : container)
			array.addNet(net);
		return array;
	}
}