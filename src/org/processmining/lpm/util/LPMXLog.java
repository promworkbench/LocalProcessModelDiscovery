package org.processmining.lpm.util;

import org.deckfour.xes.model.XLog;

public class LPMXLog {
	private XLog log;
	
	public LPMXLog(XLog log){
		setLog(log);
	}
	public XLog getLog() {
		return log;
	}
	public void setLog(XLog log) {
		this.log = log;
	}
}
