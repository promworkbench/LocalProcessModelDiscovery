package org.processmining.lpm.plugins;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNetArray;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetArrayImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModelRanking;


@Plugin(
		name = "Transform Alignment-scored Accepting Petri net Array into Accepting Petri net Array", 
		parameterLabels = {"Alignment-scored Accepting Petri net Array"}, 
	    returnLabels = {"Accepting Petri net Array"}, 
	    returnTypes = { AcceptingPetriNetArray.class }
		)
public class UnpackAlignmentScoredAcceptingPetriNetArrayImpl{
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Transform Alignment-scored Accepting Petri net Array into Accepting Petri net Array", requiredParameterLabels = {0})
	public AcceptingPetriNetArray unpack(PluginContext context, LocalProcessModelRanking nets) {
		AcceptingPetriNetArray array = new AcceptingPetriNetArrayImpl();
		for(int i=1; i<=nets.getSize(); i++){
			array.addNet(nets.getNet(i-1).getAcceptingPetriNet());
		}
		return array;
	}
}