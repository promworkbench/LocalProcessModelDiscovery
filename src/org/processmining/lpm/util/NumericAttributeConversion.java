package org.processmining.lpm.util;

import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Convert Literal Attribute to Proper Type", 
		parameterLabels = {"Event log"}, 
	    returnLabels = {"Fixed Event log"}, 
	    returnTypes = { XLog.class }
		)
public class NumericAttributeConversion {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Convert Literal Attribute to Proper Type", requiredParameterLabels = {0})
	public XLog rescoreToLog(PluginContext context, XLog log) {
		XLog logClone = (XLog) log.clone();
		
		Set<String> attributesSeen = new HashSet<String>();
		Set<String> attributesAsDiscrete = new HashSet<String>();
		Set<String> attributesAsContinuous = new HashSet<String>();
		
		for(XTrace trace : logClone){
			for(XEvent event : trace){
				for(XAttribute xa : event.getAttributes().values()){
					if(xa instanceof XAttributeLiteral){
						String val = ((XAttributeLiteral) xa).getValue();
						boolean isContinuous = true;
						try{
							Double.parseDouble(val);
						}catch(NumberFormatException e){
							isContinuous = false;
						}
						
						boolean isDiscrete = true;
						if(isContinuous){
							try{
								Integer.parseInt(val);
							}catch(NumberFormatException e){
								isDiscrete = false;
							}
						}else{
							isDiscrete = false;
						}
						
						if(isContinuous)
							if(!attributesSeen.contains(xa.getKey()))
								attributesAsContinuous.add(xa.getKey());
						else
							if(attributesAsContinuous.remove(xa.getKey()));
						
						if(isDiscrete)
						{
							if(!attributesSeen.contains(xa.getKey()))
								attributesAsDiscrete.add(xa.getKey());
						}
							
						else
							attributesAsDiscrete.remove(xa.getKey());
						
						attributesSeen.add(xa.getKey());
					}
				}
			}
		}
		
		for(XTrace trace : logClone){
			for(XEvent event : trace){
				Set<XAttribute> newAttributes = new HashSet<XAttribute>();
				Set<XAttribute> removeAttributes = new HashSet<XAttribute>();
				XAttributeMap xam = event.getAttributes();
				for(XAttribute xa : xam.values()){
					if(attributesAsDiscrete.contains(xa.getKey())){
						removeAttributes.add(xa);
						newAttributes.add(new XAttributeDiscreteImpl(xa.getKey(), Integer.parseInt(xa.toString())));
					}else{
						if(attributesAsContinuous.contains(xa.getKey())){
							removeAttributes.add(xa);
							newAttributes.add(new XAttributeContinuousImpl(xa.getKey(), Double.parseDouble(xa.toString())));
						}
					}
				}
				for(XAttribute xa : removeAttributes)
					xam.remove(xa);
				for(XAttribute xa : newAttributes)
					xam.put(xa.getKey(), xa);
			}
		}
		
		return logClone;
	}
}
