package org.processmining.lpm.util;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Bring Lifecycle to Event Name", 
		parameterLabels = {"Input log"}, 
	    returnLabels = {"Output log"}, 
	    returnTypes = { XLog.class }
		)
public class BringLifecycleToEventName {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Bring Lifecycle to Event Name", requiredParameterLabels = {0})
	public XLog run(PluginContext context, XLog log) {
		XLog logClone = (XLog) log.clone();
		for(XTrace trace : logClone){
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				String newName = "";
				if(xam.containsKey("concept:name"))
					newName = newName + xam.get("concept:name").toString();
				if(xam.containsKey("lifecycle:transition"))
					newName = newName + "+" + xam.get("lifecycle:transition").toString();
				xam.put("concept:name", new XAttributeLiteralImpl("concept:name", newName));
				xam.put("lifecycle:transition", new XAttributeLiteralImpl("lifecycle:transition", "complete"));
			}
		}
		return logClone;
	}
}
