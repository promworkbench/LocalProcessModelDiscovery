package org.processmining.lpm.utility.casestudies;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Preprocess CoSeLoG (Due Date)", 
		parameterLabels = {"CoSeLoG Log"}, 
	    returnLabels = {"Preprocessed CoSeLoG Log"}, 
	    returnTypes = { XLog.class }
		)
public class PreprocessCoSeLoGDueDate{
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Preprocess CoSeLoG (Due Date)", requiredParameterLabels = {0})
	public XLog parse(PluginContext context, XLog log){
		XLog logClone = (XLog) log.clone();
		Set<String> activitiesSeen = new HashSet<String>();
		Set<String> activitiesWithCostSeen = new HashSet<String>();
		for(XTrace trace : logClone){
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				String taskName = null;
				if(xam.containsKey("taskName")){
					taskName = xam.get("taskName").toString();
					xam.put("concept:name", new XAttributeLiteralImpl("concept:name", taskName));
				}
				if(xam.containsKey("dueDate")){
					Date plannedDate = ((XAttributeTimestamp) xam.get("dueDate")).getValue();
					Date actualDate = ((XAttributeTimestamp) xam.get("time:timestamp")).getValue();
					long timediff = actualDate.getTime() - plannedDate.getTime();
					double timediffD = ((double)Math.max(0, timediff)) / (1000*60*60*24);
					xam.put("Costs", new XAttributeContinuousImpl("Costs", timediffD));
					activitiesWithCostSeen.add(taskName);
				}else{
					xam.put("Costs", new XAttributeContinuousImpl("Costs", 0d));
				}
				activitiesSeen.add(taskName);
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
