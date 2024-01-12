package org.processmining.lpm.projection;

import java.util.Set;

import org.deckfour.xes.model.XLog;

public interface Projector {
	public Set<Set<String>> getProjections(XLog log);
}
