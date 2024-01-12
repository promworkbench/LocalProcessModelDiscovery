package org.processmining.lpm.plugins;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNetArray;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;


@Plugin(
		name = "Unpack Accepting Petri net Array", 
		parameterLabels = {"Accepting Petri net Array"}, 
	    returnLabels = {"Accepting Petri net Array"}, 
	    returnTypes = { AcceptingPetriNetArray.class }
		)
public class UnpackAcceptingPetrinetArray{
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Unpack Accepting Petri net Array", requiredParameterLabels = {0})
	public AcceptingPetriNetArray unpack(PluginContext context, AcceptingPetriNetArray nets) {
		for(int i=1; i<=nets.getSize(); i++){
			context.getProvidedObjectManager().createProvidedObject("net "+i, nets.getNet(i-1), AcceptingPetriNet.class, context);
		}
		return nets;
	}
}