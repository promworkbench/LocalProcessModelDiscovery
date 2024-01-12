package org.processmining.lpm.efficientlog.extractors;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.calculateStartingActivities;
import static org.processmining.lpm.util.PetrinetUtils.calculateAlphabet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.lpm.efficientlog.EfficientEvent;
import org.processmining.lpm.efficientlog.EfficientEventSet;

public class EventGapExtractor implements EfficientLogExtractor {
	public int max_consecutive_nonfitting = 1;
	public long max_consecutive_timedif_millis = 60*60*1000/3;
	
	private static char SEPARATOR = '#';
	
	public EventGapExtractor(int max_consecutive_nonfitting, long max_consecutive_timedif_millis){
		this.max_consecutive_nonfitting = max_consecutive_nonfitting;
		this.max_consecutive_timedif_millis = max_consecutive_timedif_millis;
	}
	
	public XLog extract(EfficientEventSet set, AcceptingPetriNet apn) {
		// Extract start events of LPM
		Set<String> startingActivities = calculateStartingActivities(apn, 2);
		Set<String> netAlphabet = calculateAlphabet(apn.getNet());
		Set<EfficientEvent> skipSet = new HashSet<EfficientEvent>();
		// Retrieve set of EfficientEvents that belong to a start event
		Map<EfficientEvent, XTrace> map = new HashMap<EfficientEvent, XTrace>();
		for(String startingActivity : startingActivities){
			Set<EfficientEvent> eventSet = set.getEfficientEventsByActivity(startingActivity);
			for(EfficientEvent event : eventSet){
				if(skipSet.contains(event))
					continue;
				XTrace trace = new XTraceImpl(new XAttributeMapImpl());
				boolean requirementsFulfilled = true;
				EfficientEvent pointerEvent = event;
				AtomicInteger currentGap = new AtomicInteger(0);
				while(pointerEvent!=null && requirementsFulfilled){
					// check if new event complies with the constraints
					requirementsFulfilled = comply(pointerEvent, netAlphabet, currentGap);
					if(requirementsFulfilled){
						// if so, add to trace
						skipSet.add(pointerEvent);
						trace.add(pointerEvent);
						map.remove(pointerEvent);
						// move event pointer
						pointerEvent = pointerEvent.getNext();
					}
				}
				if(trace.size()>1)
					map.put(event, trace);
			}
		}
		XLog log = new XLogImpl(new XAttributeMapImpl());
		for(EfficientEvent startEvent : map.keySet())
			log.add(map.get(startEvent));
		return log;
	}

	private boolean comply(EfficientEvent e, Set<String> netAlphabet, AtomicInteger consecutiveNonFitting){
		XAttributeMap xam = e.getAttributes();
		if(!xam.containsKey("concept:name") || !netAlphabet.contains(xam.get("concept:name").toString())){
			consecutiveNonFitting.incrementAndGet();
			if(consecutiveNonFitting.get()>max_consecutive_nonfitting)
				return false;
		}else{
			consecutiveNonFitting.set(0);
		}
		return true;
	}
	
	public String hashEquals(AcceptingPetriNet apn) {
		List<String> startActivities = new ArrayList<String>(calculateStartingActivities(apn, 2));
		Collections.sort(startActivities);
		List<String> activities = new ArrayList<String>(calculateAlphabet(apn.getNet()));
		Collections.sort(activities);
		return ""+startActivities+SEPARATOR+activities;
	}	

	public Set<String> requiredAttributes() {
		return new HashSet<String>(Arrays.asList("time:timestamp"));
	}
	
	public int getMax_consecutive_nonfitting() {
		return max_consecutive_nonfitting;
	}

	public void setMax_consecutive_nonfitting(int max_consecutive_nonfitting) {
		this.max_consecutive_nonfitting = max_consecutive_nonfitting;
	}

	public long getMax_consecutive_timedif_millis() {
		return max_consecutive_timedif_millis;
	}

	public void setMax_consecutive_timedif_millis(long max_consecutive_timedif_millis) {
		this.max_consecutive_timedif_millis = max_consecutive_timedif_millis;
	}
}
