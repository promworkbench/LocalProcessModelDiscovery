package org.processmining.lpm.efficientlog.extractors;

import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.lpm.efficientlog.EfficientEventSet;

public interface EfficientLogExtractor {
	/*
	 * Creates an XLog object for a specific Accepting Petri Net for an Efficient Event Set
	 */
	public XLog extract(EfficientEventSet set, AcceptingPetriNet apn);
	
	/*
	 * Creates a hash String such that hashEquals(apn1).equals(hashEquals(apn2)) iff extract(set,apn1).equals(extract(set,apn2))
	 */
	public String hashEquals(AcceptingPetriNet apn);
	
	/*
	 * Returns a set of XAttribute names needed in the log to perform extract()
	 */
	public Set<String> requiredAttributes();
}
