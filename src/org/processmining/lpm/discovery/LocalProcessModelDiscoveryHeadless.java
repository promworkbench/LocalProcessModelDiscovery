package org.processmining.lpm.discovery;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(
		name = "Headless search for Local Process Models", 
		parameterLabels = {"Input Log", "Parameters"}, 
	    returnLabels = {"Local Process Model Ranking"}, 
	    returnTypes = { LocalProcessModelRanking.class },
	    level = PluginLevel.PeerReviewed
		)
public class LocalProcessModelDiscoveryHeadless extends LocalProcessModelDiscovery {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Headless search for Local Process Models", requiredParameterLabels = {0})
	public LocalProcessModelRanking runWithLog(PluginContext context, XLog log) {
		LocalProcessModelParameters params = new LocalProcessModelParameters();
		params.setDiscoveryLog(log);
		params.setEvaluationLog(log);
		params.setSmartParameterDefaultsForDiscoveryLog();
		return runHeadless(context, params); // uses default parameters
	}

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Headless search for Local Process Models with Parameters", requiredParameterLabels = {1})
	public LocalProcessModelRanking runWithParams(PluginContext context, LocalProcessModelParameters params) {
		return runHeadless(context, params); // uses provided parameters
	}
}
