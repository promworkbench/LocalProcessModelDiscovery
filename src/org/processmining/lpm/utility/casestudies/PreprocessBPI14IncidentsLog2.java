package org.processmining.lpm.utility.casestudies;

import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Preprocess BPI'14 Incidents Log v2", 
		parameterLabels = {"BPI'14 Incidents Log"}, 
	    returnLabels = {"Preprocessed BPI'14 Incidents Log"}, 
	    returnTypes = { XLog.class }
		)
public class PreprocessBPI14IncidentsLog2{
	public final String utilityName = "# Related Interactions";
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Preprocess BPI'14 Incidents Log v2", requiredParameterLabels = {0})
	public XLog parse(PluginContext context, XLog log){
		Map<String, XTrace> cases = new HashMap<String,XTrace>();
		for(XTrace trace : log){
			for(XEvent event : trace){
				XEvent eventCopy = (XEvent) event.clone();
				XAttributeMap xam = eventCopy.getAttributes();
				if(xam.containsKey("concept:name")){
					String cname = xam.get("concept:name").toString();
					XTrace curTrace = null;
					if(cases.containsKey(cname)){
						curTrace = cases.get(cname);
					}else{
						XAttributeMap traceXam = new XAttributeMapImpl();
						traceXam.put("concept:name", new XAttributeLiteralImpl("concept:name", cname));
						curTrace = new XTraceImpl(traceXam);
					}
					if(xam.containsKey(utilityName))
						xam.put("Costs", new XAttributeContinuousImpl("Costs", ((XAttributeDiscrete) xam.get(utilityName)).getValue()));
					else
						xam.put("Costs", new XAttributeContinuousImpl("Costs", 0));
					String name = "";
					if(xam.containsKey("Category"))
						name = name + xam.get("Category").toString();
					if(xam.containsKey("Closure Code"))
						name = name.equals("") ? name + xam.get("Closure Code").toString() : name + " " + xam.get("Closure Code").toString();
					if(xam.containsKey("CI Type (CBy)") && xam.get("CI Type (CBy)").toString()!=null){
						String r = xam.get("CI Type (CBy)").toString();
						name = name.equals("") ? name + r : name + " " + r;
					}
					if(xam.containsKey("CI Subtype (CBy)") && xam.get("CI Subtype (CBy)").toString()!=null)
						name = name.equals("") ? name + xam.get("CI Subtype (CBy)").toString() : name + " " + xam.get("CI Subtype (CBy)").toString();
					if(xam.containsKey("ServiceComp WBS (CBy)") && xam.get("ServiceComp WBS (CBy)").toString()!=null){
						String r = xam.get("ServiceComp WBS (CBy)").toString();
						name = name.equals("") ? name + r : name + " " + r;
					}
					if(!name.contains("#N/B") && !name.contains("Unknown") && !name.contains("Other")){
						xam.put("concept:name", new XAttributeLiteralImpl("concept:name", name));
						curTrace.add(eventCopy);
						cases.put(cname, curTrace);
					}
				}
			}
		}
		XAttributeMap newLogXam = new XAttributeMapImpl();
		newLogXam.put("concept:name", new XAttributeLiteralImpl("concept:name", "Transformed BPI'14 Incidents log"));
		XLog newLog = new XLogImpl(newLogXam);
		for(XTrace trace : cases.values())
			newLog.add(trace);
		return newLog;
	}
}
