package org.processmining.lpm.efficientlog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.efficientlog.extractors.EfficientLogExtractor;

@Plugin(
		name = "Create Efficient Event Set from Log", 
		parameterLabels = {"Input Log"}, 
	    returnLabels = {"Efficient Event Set"}, 
	    returnTypes = { EfficientEventSet.class }
		)
public class EfficientEventSet {
	private Map<String, Set<EfficientEvent>> efficientEventsByActivity;
	private int size; 
	
	public EfficientEventSet(XLog log){
		efficientEventsByActivity = new HashMap<String, Set<EfficientEvent>>();
		for(XTrace trace : log){
			EfficientEvent previousEvent = null;
			for(XEvent event : trace){
				EfficientEvent effEvent = new EfficientEvent(event.getAttributes());
				if(previousEvent!=null)
					previousEvent.setNext(effEvent);
				String cname = effEvent.getAttributes().get("concept:name").toString();
				Set<EfficientEvent> cnameEvents = efficientEventsByActivity.get(cname);
				if(cnameEvents==null)
					cnameEvents = new HashSet<EfficientEvent>();
				cnameEvents.add(effEvent);
				efficientEventsByActivity.put(cname, cnameEvents);
				size += 1;
				previousEvent = effEvent;
			}
		}
	}
	
	public Set<EfficientEvent> getEfficientEventsByActivity(String activityName){
		return efficientEventsByActivity.get(activityName);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Create Efficient Event Set from Log", requiredParameterLabels = {0})
	public EfficientEventSet run(PluginContext context, XLog log) {
		return new EfficientEventSet(log);
	}
	
	public XLog extractXLog(AcceptingPetriNet apn, EfficientLogExtractor extractor){
		return extractor.extract(this, apn);
	}

	public int getSize() {
		return size;
	}
}
