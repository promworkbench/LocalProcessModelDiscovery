package org.processmining.lpm.targeted;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Find Non-Activity-Specific Event Attributes", 
		parameterLabels = {"Input Log"}, 
	    returnLabels = {"Log with only non-acticity-specific attributes"}, 
	    returnTypes = { XLog.class }
		)
public class FindNonActivitySpecificEventAttributes {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Find Non-Activity-Specific Event Attributes", requiredParameterLabels = {0})
	public XLog run(PluginContext context, XLog log) {
		return runHeadless(log);
	}
	
	public static XLog runHeadless(XLog log){
		Map<String, Set<String>> attributeNamesToConceptNamesMap = new HashMap<String, Set<String>>();
		XLog clone = (XLog) log.clone();
		for(XTrace trace : clone){
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				if(!xam.containsKey("concept:name"))
					continue;
				String conceptName = xam.get("concept:name").toString();
				for(String attributeName : xam.keySet()){
					Set<String> currentCnames = attributeNamesToConceptNamesMap.get(attributeName);
					if(currentCnames==null)
						currentCnames = new HashSet<String>();
					currentCnames.add(conceptName);
					attributeNamesToConceptNamesMap.put(attributeName, currentCnames);
				}
			}
		}
		
		Set<String> allowedAttributeNames = new HashSet<String>();
		for(String attributeName : attributeNamesToConceptNamesMap.keySet()){
			if(attributeNamesToConceptNamesMap.get(attributeName).size()>1){
				System.out.println(attributeName+": "+attributeNamesToConceptNamesMap.get(attributeName));
				allowedAttributeNames.add(attributeName);
			}
		}
		System.out.println();
		for(String attributeName : attributeNamesToConceptNamesMap.keySet()){
			if(attributeNamesToConceptNamesMap.get(attributeName).size()>1){
				System.out.println(attributeName);
			}
		}
		for(XTrace trace : clone){
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				Set<String> attributesToRemove = new HashSet<String>();
				for(String key : xam.keySet()){
					if(!allowedAttributeNames.contains(key)){
						attributesToRemove.add(key);
					}
				}
				for(String key : attributesToRemove)
					xam.remove(key);
			}
		}
		return clone;
	}
}