package org.processmining.lpm.resource;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.utils.XLogBuilder;

@Plugin(name = "Apply Day Case Notion", parameterLabels = { "Event Log" }, returnLabels = { "Event Log" }, returnTypes = { XLog.class }, categories = {
		PluginCategory.Enhancement }, keywords = "", help = "", userAccessible = true, handlesCancel = false)
public class ApplyDayCaseNotion {
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Niek Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Apply Day Case Notion", requiredParameterLabels = { 0 })
	public XLog run(PluginContext context, XLog log) {
		XLogBuilder xlb = XLogBuilder.newInstance();
		xlb.startLog(log.getAttributes().get("concept:name").toString());
	    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

		for(XTrace trace : log){
			XAttributeMap xamt = trace.getAttributes();
			xlb.addTrace(trace.getAttributes().get("concept:name").toString());
			for(String key : xamt.keySet()){
				XAttribute xa = xamt.get(key);
				xlb.addAttribute(xa);
			}
			Date previousDate = null;
			for(XEvent event : trace){
				XAttributeMap xame = event.getAttributes();
				Date date = null;
				if(xame.containsKey("time:start"))
					date = ((XAttributeTimestamp) xame.get("time:start")).getValue();
				if(date==null && xame.containsKey("time:timestamp"))
					date = ((XAttributeTimestamp) xame.get("time:timestamp")).getValue();
				if(previousDate!=null && !isSameDay(previousDate,date)){
					xlb.addTrace(dateFormat.format(date));
				}
				previousDate = date;
				xlb.addEvent(xame.get("concept:name").toString());
				for(String key : xame.keySet()){
					XAttribute xa = xame.get(key);
					xlb.addAttribute(xa);
				}
			}
		}
		return xlb.build();
	}
	
	public static boolean isSameDay(final Date date1, final Date date2){
		if(date1==null||date2==null)
			return false;
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
		                  cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
		return sameDay;
	}
}