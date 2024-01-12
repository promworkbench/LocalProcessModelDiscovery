package org.processmining.lpm.discovery;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LPMXLog;

@Plugin(
		name = "Search for Local Process Models", 
		parameterLabels = {"Input Log"}, 
	    returnLabels = {"Local Process Model Ranking"}, 
	    returnTypes = { LPMXLog.class },
	    level = PluginLevel.PeerReviewed
		)
public class LocalProcessModelDiscoveryUI extends LocalProcessModelDiscovery {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Search for Local Process Models", requiredParameterLabels = {0})
	public LPMXLog run(UIPluginContext context, XLog log) {
		//params = new LocalProcessModelParameters();
		//params.setDiscoveryLog(log);
		//params.setEvaluationLog(log);
		//params.setSmartParameterDefaultsForDiscoveryLog();
		//LocalProcessModelDialog dialog = new LocalProcessModelDialog(params);
	    // Show the dialog. User can now change the configuration.
	    //InteractionResult result = context.showWizard("Configure parameters for Local Process Model Discovery", true, true, dialog);
	    // User has close the dialog.
	    //if (result == InteractionResult.FINISHED) 
	    //	return runHeadless(context, params);
		return new LPMXLog(log);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Search for Local Process Models", requiredParameterLabels = {0})
	public LPMXLog run(PluginContext context, XLog log) {
		//params = new LocalProcessModelParameters();
		//params.setDiscoveryLog(log);
		//params.setEvaluationLog(log);
		//params.setSmartParameterDefaultsForDiscoveryLog();
		//return runHeadless(context, params); // uses default parameters
		//LocalProcessModelMiner miner = new LocalProcessModelMiner();
		return new LPMXLog(log);
	}
}
