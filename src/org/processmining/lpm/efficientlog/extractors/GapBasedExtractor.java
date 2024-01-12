package org.processmining.lpm.efficientlog.extractors;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.calculateStartingActivities;
import static org.processmining.lpm.util.PetrinetUtils.calculateAlphabet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.lpm.efficientlog.EfficientEvent;
import org.processmining.lpm.efficientlog.EfficientEventSet;

public class GapBasedExtractor implements EfficientLogExtractor {
	public static int MAX_CONSECUTIVE_NONFITTING = 0;
	public static int MAX_TOTAL_NONFITTING = 0;
	
	private static char SEPARATOR = '#';
	
	public XLog extract(EfficientEventSet set, AcceptingPetriNet apn) {
		// Extract start events of LPM
		Set<String> startingActivities = calculateStartingActivities(apn, 2);
		Set<String> netAlphabet = calculateAlphabet(apn.getNet());
		//XLogBuilder xlb = XLogBuilder.newInstance();
		//xlb.startLog("");
		XLog log = new XLogImpl(new XAttributeMapImpl());
		// Retrieve set of EfficientEvents that belong to a start event
		//int traceId = 1;
		for(String startingActivity : startingActivities){
			Set<EfficientEvent> eventSet = set.getEfficientEventsByActivity(startingActivity);
			for(EfficientEvent event : eventSet){
				//xlb.addTrace(""+traceId);
				XTrace trace = new XTraceImpl(new XAttributeMapImpl());
				boolean requirementsFulfilled = true;
				int consecutiveNonFitting = 0;
				int totalNonFitting = 0;
				EfficientEvent pointerEvent = event;
				while(pointerEvent!=null && requirementsFulfilled){
					String activity = pointerEvent.getAttributes().get("concept:name").toString();
					if(!netAlphabet.contains(activity)){
						consecutiveNonFitting++;
						totalNonFitting++;
					}else{
						consecutiveNonFitting=0;
						//xlb.addEvent(activity);
						trace.add(pointerEvent);
					}
					if(consecutiveNonFitting>MAX_CONSECUTIVE_NONFITTING || totalNonFitting>MAX_TOTAL_NONFITTING)
						requirementsFulfilled = false;
					pointerEvent = pointerEvent.getNext();
				}
				log.add(trace);
				//traceId++;
			}
		}
				
		//return xlb.build();
		return log;
	}

	public String hashEquals(AcceptingPetriNet apn) {
		List<String> startActivities = new ArrayList<String>(calculateStartingActivities(apn, 2));
		Collections.sort(startActivities);
		List<String> activities = new ArrayList<String>(calculateAlphabet(apn.getNet()));
		Collections.sort(activities);
		return ""+startActivities+SEPARATOR+activities;
	}

	public Set<String> requiredAttributes() {
		return new HashSet<String>();
	}
}
