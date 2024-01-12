package org.processmining.lpm.efficientlog.extractors;

import static org.processmining.lpm.util.AcceptingPetrinetUtils.calculateStartingActivities;
import static org.processmining.lpm.util.PetrinetUtils.calculateAlphabet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

public class GapAndTimeBasedExtractor2 implements EfficientLogExtractor {
	public int max_consecutive_nonfitting = 0;
	public int max_total_nonfitting = 0;
	public long max_consecutive_timedif_millis = 60*60*1000/3;
	public long max_total_timedif_millis = 2*60*60*1000;
	
	private static char SEPARATOR = '#';
	
	public GapAndTimeBasedExtractor2(int max_consecutive_nonfitting, int max_total_nonfitting, long max_consecutive_timedif_millis, long max_total_timedif_millis){
		this.max_consecutive_nonfitting = max_consecutive_nonfitting;
		this.max_total_nonfitting = max_total_nonfitting;
		this.max_consecutive_timedif_millis = max_consecutive_timedif_millis;
		this.max_total_timedif_millis = max_total_timedif_millis;
	}
	
	public XLog extract(EfficientEventSet set, AcceptingPetriNet apn) {
		// Extract start events of LPM
		Set<String> startingActivities = calculateStartingActivities(apn, 2);
		Set<String> netAlphabet = calculateAlphabet(apn.getNet());
		XLog log = new XLogImpl(new XAttributeMapImpl());
		// Retrieve set of EfficientEvents that belong to a start event
		//int traceId = 1;
		for(String startingActivity : startingActivities){
			Set<EfficientEvent> eventSet = set.getEfficientEventsByActivity(startingActivity);
			Set<EfficientEvent> skipSet = new HashSet<EfficientEvent>();
			for(EfficientEvent event : eventSet){
				if(skipSet.contains(event))
					continue;
				XTrace trace = new XTraceImpl(new XAttributeMapImpl());
				boolean requirementsFulfilled = true;
				int consecutiveNonFitting = 0;
				int totalNonFitting = 0;
				Long previousTime = null;
				Long startTime = null;
				EfficientEvent pointerEvent = event;
				String activity = null;
				boolean allSame = true;
				while(pointerEvent!=null && requirementsFulfilled){
					XAttributeMap xam = pointerEvent.getAttributes();
					if(xam.containsKey("concept:name")){
						if(activity==null || !activity.equals(xam.get("concept:name").toString()))
							allSame = false;
						activity = xam.get("concept:name").toString();
						if(allSame && totalNonFitting==0)
							skipSet.add(pointerEvent);
					}
					Long time = null;
					if(xam.containsKey("time:timestamp"))
						time = ((XAttributeTimestamp) xam.get("time:timestamp")).getValueMillis();
					else
						System.out.println(xam);
					if(previousTime==null)
						previousTime = time;
					if(startTime==null)
						startTime = time;
					if(!netAlphabet.contains(activity)){
						consecutiveNonFitting++;
						totalNonFitting++;
					}else{
						consecutiveNonFitting=0;
					}
					if(consecutiveNonFitting>max_consecutive_nonfitting 
							|| totalNonFitting>max_total_nonfitting 
							|| (time-previousTime)>max_consecutive_timedif_millis
							|| (time-startTime)>max_total_timedif_millis){
						requirementsFulfilled = false;
					}
					if(requirementsFulfilled && netAlphabet.contains(activity))
						trace.add(pointerEvent);
					previousTime = time;
					pointerEvent = pointerEvent.getNext();
				}
				if(trace.size()>1)
					log.add(trace);
			}
		}
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
		return new HashSet<String>(Arrays.asList("time:timestamp"));
	}
	
	public int getMax_consecutive_nonfitting() {
		return max_consecutive_nonfitting;
	}

	public void setMax_consecutive_nonfitting(int max_consecutive_nonfitting) {
		this.max_consecutive_nonfitting = max_consecutive_nonfitting;
	}

	public int getMax_total_nonfitting() {
		return max_total_nonfitting;
	}

	public void setMax_total_nonfitting(int max_total_nonfitting) {
		this.max_total_nonfitting = max_total_nonfitting;
	}

	public long getMax_consecutive_timedif_millis() {
		return max_consecutive_timedif_millis;
	}

	public void setMax_consecutive_timedif_millis(long max_consecutive_timedif_millis) {
		this.max_consecutive_timedif_millis = max_consecutive_timedif_millis;
	}

	public long getMax_total_timedif_millis() {
		return max_total_timedif_millis;
	}

	public void setMax_total_timedif_millis(long max_total_timedif_millis) {
		this.max_total_timedif_millis = max_total_timedif_millis;
	}
}
