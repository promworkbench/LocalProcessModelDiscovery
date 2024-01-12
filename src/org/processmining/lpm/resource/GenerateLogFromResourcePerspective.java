package org.processmining.lpm.resource;

import static org.processmining.lpm.util.LogUtils.generateLogFromAttributePerspective;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Generate log from org:resource perspective", 
		parameterLabels = {"Input Log"}, 
	    returnLabels = {"Resource Log"}, 
	    returnTypes = { XLog.class }
		)
public class GenerateLogFromResourcePerspective {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Generate log from org:resource perspective", requiredParameterLabels = {0})
	public XLog aggregateTracesByPeriodDefault(PluginContext context, XLog log) {
		return generateLogFromAttributePerspective(log, "org:resource");
	}	
}