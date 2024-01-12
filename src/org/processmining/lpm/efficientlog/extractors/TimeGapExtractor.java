package org.processmining.lpm.efficientlog.extractors;

import static org.processmining.lpm.util.PetrinetUtils.calculateAlphabet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.lpm.efficientlog.EfficientEvent;
import org.processmining.lpm.efficientlog.EfficientEventSet;

public class TimeGapExtractor implements EfficientLogExtractor {
	public int max_consecutive_nonfitting = 1;
	public long max_consecutive_timedif_millis = 60*60*1000/3;
	
	private static char SEPARATOR = '#';
	
	public TimeGapExtractor(int max_consecutive_nonfitting, long max_consecutive_timedif_millis){
		this.max_consecutive_nonfitting = max_consecutive_nonfitting;
		this.max_consecutive_timedif_millis = max_consecutive_timedif_millis;
	}
	
	public XLog extract(EfficientEventSet set, AcceptingPetriNet apn) {
		// Extract start events of LPM
		Set<String> netAlphabet = calculateAlphabet(apn.getNet());
		// Retrieve set of EfficientEvents that belong to a start event
		Map<EfficientEvent, XTrace> map = new HashMap<EfficientEvent, XTrace>();
		Set<EfficientEvent> startEvents = new HashSet<EfficientEvent>();
		Set<EfficientEvent> nonStartEvents = new HashSet<EfficientEvent>();
		for(String activity : netAlphabet){
			for(EfficientEvent event : set.getEfficientEventsByActivity(activity)){
				startEvents.add(event);
				if(event.getNext()!=null)
					nonStartEvents.add(event.getNext());
			}
		}
		startEvents.removeAll(nonStartEvents);
		for(EfficientEvent event : startEvents){
			EfficientEvent startEvent = event;
			XTrace trace = new XTraceImpl(new XAttributeMapImpl());
			boolean requirementsFulfilled = true;
			EfficientEvent pointerEvent = event;
			Long previousTime = null;
			while(pointerEvent!=null && requirementsFulfilled){
				// check if new event complies with the constraints
				requirementsFulfilled = comply(pointerEvent, previousTime, netAlphabet);
				if(!requirementsFulfilled || pointerEvent.getNext()==null){
					if(trace.size()>1)
						map.put(startEvent, (XTrace) trace.clone());
					trace = new XTraceImpl(new XAttributeMapImpl());
					startEvent = pointerEvent;
				}
				trace.add(pointerEvent);
				previousTime = ((XAttributeTimestamp) pointerEvent.getAttributes().get("time:timestamp")).getValueMillis();
				pointerEvent = pointerEvent.getNext();
			}

		}
		
		XLog log = new XLogImpl(new XAttributeMapImpl());
		for(EfficientEvent startEvent : map.keySet())
			log.add(map.get(startEvent));
		return log;
	}

	private boolean comply(EfficientEvent e, Long previousTime, Set<String> netAlphabet){
		XAttributeMap xam = e.getAttributes();
		if(!xam.containsKey("time:timestamp") || (previousTime!=null && ((XAttributeTimestamp) xam.get("time:timestamp")).getValueMillis()-previousTime>max_consecutive_timedif_millis))
			return false;
		return true;
	}
	
	public String hashEquals(AcceptingPetriNet apn) {
		return "FIXED";
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
