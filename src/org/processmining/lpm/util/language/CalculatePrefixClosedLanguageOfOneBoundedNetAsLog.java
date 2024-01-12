package org.processmining.lpm.util.language;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.calculateLanguage;
import static org.processmining.lpm.util.LogUtils.setOfEventListsToLog;

import java.util.List;
import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Calculate prefix-closed Language of 1-bounded Accepting Petri Net as Log", 
		parameterLabels = {"Accepting Petri Net"}, 
	    returnLabels = {"language"}, 
	    returnTypes = { XLog.class }
	)
public class CalculatePrefixClosedLanguageOfOneBoundedNetAsLog {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Calculate prefix-closed Language of 1-bounded Accepting Petri Net as Log", requiredParameterLabels = {0})
	public XLog calculateAll(PluginContext context, AcceptingPetriNet apn) {
		Set<List<String>> logAsSet = calculateLanguage(apn, 5, false);
		return setOfEventListsToLog(logAsSet);
	}
}