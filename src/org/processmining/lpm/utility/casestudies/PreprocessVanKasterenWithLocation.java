package org.processmining.lpm.utility.casestudies;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name = "Preprocess Van Kasteren with Location", 
		parameterLabels = {"BPI'14 Incidents Log"}, 
	    returnLabels = {"Preprocessed Van Kasteren Log"}, 
	    returnTypes = { XLog.class }
		)
public class PreprocessVanKasterenWithLocation{
	public final String utilityName = "Priority";
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Preprocess Van Kasteren with Location", requiredParameterLabels = {0})
	public XLog preprocess(PluginContext context, XLog log){
		XLog clone = (XLog) log.clone();
		for(XTrace trace : clone){
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				if(xam.containsKey("concept:name")){
					String cname = xam.get("concept:name").toString();
					String locationString = null;
					switch(cname){
			            case "use toilet":  locationString = "Bathroom";
	                    	break;
			            case "take shower":  locationString = "Bathroom";
                			break;
			            case "go to bed":  locationString = "Bedroom";
	                		break;
			            case "prepare breakfast":  locationString = "Kitchen";
            				break;
			            case "get drink": locationString = "Kitchen";
			            	break;
			            case "prepare dinner": locationString = "Kitchen";
			            	break;
			            case "leave house":  locationString = "Outside house";
			                break;
			            default: locationString = "Unknown Location";
			            	break;
					}
					xam.put("location", new XAttributeLiteralImpl("location", locationString));
				}
			}
		}
		clone.getGlobalEventAttributes().add(new XAttributeLiteralImpl("Location", "Unknown Location"));
		return clone;
	}
}
