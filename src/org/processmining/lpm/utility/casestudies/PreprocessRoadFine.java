package org.processmining.lpm.utility.casestudies;

import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Preprocess Road Fine with remaining_amount", parameterLabels = { "Event Log" }, returnLabels = { "Event Log" }, returnTypes = { XLog.class }, categories = {
		PluginCategory.Enhancement }, keywords = "", help = "", userAccessible = true, handlesCancel = false)
public class PreprocessRoadFine {
	public static final String attrName = "Costs";
	
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Niek Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Preprocess Road Fine with remaining_amount", requiredParameterLabels = { 0 })
	public XLog run(PluginContext context, XLog log) {
		XLog logClone = (XLog) log.clone();
		for(XTrace trace : logClone){
			double latestAmount = 0;
			double latestExpense = 0;
			double amountPaid = 0;
			for(XEvent event : trace){
				XAttributeMap xam = event.getAttributes();
				String conceptName = xam.get("concept:name").toString();
				if(conceptName.equals("Create Fine")){
					Double value = ((XAttributeContinuous) xam.get("amount")).getValue();
					latestAmount = value;
				}
				if(conceptName.equals("Send Fine")){
					Double value = ((XAttributeContinuous) xam.get("expense")).getValue();
					latestExpense = value;
				}
				if(conceptName.equals("Add penalty")){
					Double value = ((XAttributeContinuous) xam.get("amount")).getValue();
					latestAmount = value;
				}
				if(conceptName.equals("Payment")){
					Double value = ((XAttributeContinuous) xam.get("paymentAmount")).getValue();
					amountPaid += value;
				}
				xam.put(attrName, new XAttributeContinuousImpl(attrName, latestAmount+latestExpense-amountPaid));
			}
		}
		return logClone;
	}
}