package org.processmining.lpm.util;

import static org.processmining.lpm.util.LogUtils.getActivityCounts;

import java.util.Map;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNetArray;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;

@Plugin(
		name = "Rescore Local Process Model ranking to Log", 
		parameterLabels = {"LPM ranking", "Event Log"}, 
	    returnLabels = {"New LPM ranking"}, 
	    returnTypes = { LocalProcessModelRanking.class }
		)
public class RescoreToLog {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Rescore Local Process Model ranking to Log", requiredParameterLabels = {0,1})
	public LocalProcessModelRanking rescoreToLog(PluginContext context, XLog log, LocalProcessModelRanking ranking) {
		LocalProcessModelRanking newRanking = new LocalProcessModelRanking();
		Map<String, Integer> activityCountsMap = getActivityCounts(log);
		for(int i=0; i<ranking.getSize(); i++){
			AcceptingPetriNet apn = ranking.getNet(i).getAcceptingPetriNet();
			LocalProcessModelParameters params = new LocalProcessModelParameters();
			params.setEvaluationLog(log);
			LocalProcessModelEvaluator scorer = new LocalProcessModelEvaluator(params);
			newRanking.addElement(scorer.evaluateNetOnLog(context, apn, activityCountsMap, false));
		}
		return newRanking;
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Rescore Local Process Model ranking to Log", requiredParameterLabels = {0,1})
	public LocalProcessModelRanking rescoreToLog(PluginContext context, XLog log, AcceptingPetriNetArray array) {
		LocalProcessModelRanking newRanking = new LocalProcessModelRanking();
		Map<String, Integer> activityCountsMap = getActivityCounts(log);
		for(int i=0; i<array.getSize(); i++){
			AcceptingPetriNet apn = array.getNet(i);
			LocalProcessModelParameters params = new LocalProcessModelParameters();
			params.setEvaluationLog(log);
			LocalProcessModelEvaluator scorer = new LocalProcessModelEvaluator(params);
			newRanking.addElement(scorer.evaluateNetOnLog(context, apn, activityCountsMap, false));
		}
		return newRanking;
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Rescore Local Process Model ranking to Log", requiredParameterLabels = {0,1})
	public LocalProcessModelRanking rescoreToLog(PluginContext context, XLog log, AcceptingPetriNet... array) {
		LocalProcessModelRanking newRanking = new LocalProcessModelRanking();
		Map<String, Integer> activityCountsMap = getActivityCounts(log);
		for(int i=0; i<array.length; i++){
			AcceptingPetriNet apn = array[i];
			LocalProcessModelParameters params = new LocalProcessModelParameters();
			params.setEvaluationLog(log);
			LocalProcessModelEvaluator scorer = new LocalProcessModelEvaluator(params);
			newRanking.addElement(scorer.evaluateNetOnLog(context, apn, activityCountsMap, false));
		}
		return newRanking;
	}
}
