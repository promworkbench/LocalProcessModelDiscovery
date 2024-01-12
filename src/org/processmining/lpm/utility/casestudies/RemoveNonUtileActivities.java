package org.processmining.lpm.utility.casestudies;

import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Remove Activities that never have Utility", 
		parameterLabels = {"CoSeLoG Log"}, 
	    returnLabels = {"Preprocessed CoSeLoG Log"}, 
	    returnTypes = { XLog.class }
		)
public class RemoveNonUtileActivities{
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "PRemove Activities that never have Utility", requiredParameterLabels = {0})
	public XLog parse(PluginContext context, XLog log){
		XLog logClone = (XLog) log.clone();
		Set<String> activitiesSeen = new HashSet<String>();
		Set<String> activitiesWithCostSeen = new HashSet<String>();
		for(XTrace trace : logClone){
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				if(xam.containsKey("concept:name")){
					String taskName = xam.get("concept:name").toString();
					activitiesSeen.add(taskName);
					if(!activitiesWithCostSeen.contains(taskName) && xam.containsKey("Costs")){
						double cost = ((XAttributeContinuous) xam.get("Costs")).getValue();
						if(cost>0)
							activitiesWithCostSeen.add(taskName);
					}
				}
			}
		}
		Set<String> noCostActivities = new HashSet<String>(activitiesSeen);
		noCostActivities.removeAll(activitiesWithCostSeen);
		for(XTrace trace : logClone){
			Set<XEvent> toRemove = new HashSet<XEvent>();
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				if(xam.containsKey("concept:name")){
					String taskName = xam.get("concept:name").toString();
					if(noCostActivities.contains(taskName))
						toRemove.add(event);
				}
			}
			for(XEvent removeEvent : toRemove)
				trace.remove(removeEvent);
		}
		return logClone;
	}
}
