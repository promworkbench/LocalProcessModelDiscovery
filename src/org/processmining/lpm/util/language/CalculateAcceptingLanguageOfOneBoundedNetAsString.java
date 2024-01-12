package org.processmining.lpm.util.language;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.calculateLanguage;
import static org.processmining.lpm.util.AcceptingPetrinetUtils.prettyPrintLanguage;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Calculate accepting Language of 1-bounded Accepting Petri Net as String", 
		parameterLabels = {"Accepting Petri Net"}, 
	    returnLabels = {"String"}, 
	    returnTypes = { String.class }
	)
public class CalculateAcceptingLanguageOfOneBoundedNetAsString {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Calculate accepting Language of 1-bounded Accepting Petri Net as String", requiredParameterLabels = {0})
	public String calculateAll(PluginContext context, AcceptingPetriNet apn) {
		return prettyPrintLanguage(calculateLanguage(apn, 5, true));
	}
}