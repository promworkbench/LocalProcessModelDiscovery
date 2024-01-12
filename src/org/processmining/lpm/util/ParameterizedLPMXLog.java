package org.processmining.lpm.util;

import org.deckfour.xes.model.XLog;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;

public class ParameterizedLPMXLog extends LPMXLog{
	private LocalProcessModelParameters params;
	
	public ParameterizedLPMXLog(XLog log, LocalProcessModelParameters params){
		super(log);
		this.setParams(params);
	}

	public LocalProcessModelParameters getParams() {
		return params;
	}

	public void setParams(LocalProcessModelParameters params) {
		this.params = params;
	}
}