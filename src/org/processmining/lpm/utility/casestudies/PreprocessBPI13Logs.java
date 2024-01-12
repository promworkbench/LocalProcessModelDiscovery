package org.processmining.lpm.utility.casestudies;

import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Preprocess BPI'13 Log", 
		parameterLabels = {"BPI'13 Log"}, 
	    returnLabels = {"Preprocessed BPI'13 Log"}, 
	    returnTypes = { XLog.class }
		)
public class PreprocessBPI13Logs{
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Preprocess BPI'13 Log", requiredParameterLabels = {0})
	public XLog parse(PluginContext context, XLog log){
		XLog logClone = (XLog) log.clone();
		Set<String> impacts = new HashSet<String>();
		for(XTrace trace : logClone){
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				if(xam.containsKey("impact")){
					String impact = xam.get("impact").toString();
					impacts.add(impact);
					double cost = 0d;
					if(impact.equals("Major"))
						cost = 75;
					if(impact.equals("High"))
						cost = 50;
					if(impact.equals("Medium"))
						cost = 25;
					if(impact.equals("Low"))
						cost = 0;
					xam.put("Costs", new XAttributeContinuousImpl("Costs", cost));
				}
			}
		}
		System.out.println(impacts);
		return logClone;
	}
}
