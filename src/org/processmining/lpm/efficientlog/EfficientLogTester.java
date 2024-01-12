package org.processmining.lpm.efficientlog;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.efficientlog.extractors.GapBasedExtractor;

@Plugin(
		name = "Test Gap Based Extractor", 
		parameterLabels = {"Input Log", "Accepting Petri Net"}, 
	    returnLabels = {"Extracted Log"}, 
	    returnTypes = { XLog.class }
		)
public class EfficientLogTester {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Test Gap Based Extractor", requiredParameterLabels = {0,1})
	public XLog run(PluginContext context, XLog log, AcceptingPetriNet apn) {
		EfficientEventSet ees = new EfficientEventSet(log);
		return ees.extractXLog(apn, new GapBasedExtractor()); // uses default parameters
	}
}
