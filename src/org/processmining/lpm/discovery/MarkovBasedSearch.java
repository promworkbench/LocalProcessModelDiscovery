package org.processmining.lpm.discovery;

import static org.processmining.lpm.util.LogUtils.projectLogOnEventNames;

import java.util.Set;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.projection.MCLBasedProjectionsTwoNorm;
import org.processmining.lpm.projection.Projector;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.LocalProcessModelTopSet;

@Plugin(
		name = "Search Local Process Models using Markov-based Projections", 
		parameterLabels = {"Input Log"}, 
	    returnLabels = {"Local Process Model Ranking"}, 
	    returnTypes = { LocalProcessModelRanking.class }
		)
public class MarkovBasedSearch {
	LocalProcessModelParameters params;
	
	private static final int MAX_SIZE_THRESHOLD = 7;
	private static final int MAX_SUBSEQUENT_ATTEMPTS = 100; 
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Search Local Process Models using Markov clustering", requiredParameterLabels = {0})
	public LocalProcessModelRanking bruteForceProcessTreeEpisode(PluginContext context, XLog log) {
		params = new LocalProcessModelParameters();
		params.setEvaluationLog(log);
		return runHeadless(context, 0, params, new LocalProcessModelDiscovery(), 1.5); // uses default parameters
	}

	public static LocalProcessModelRanking runHeadless(PluginContext context, int subsequentFailedProjections, LocalProcessModelParameters params, LocalProcessModelDiscovery processor, double inflationParameter) {
		Projector markovProjector = new MCLBasedProjectionsTwoNorm(inflationParameter, false);
		Set<Set<String>> projections = markovProjector.getProjections(params.getDiscoveryLog());
		XLogInfo info = XLogInfoFactory.createLogInfo(params.getDiscoveryLog(), params.getClassifier());
		LocalProcessModelTopSet container = new LocalProcessModelTopSet(params.getTop_k());
		if(params.isVerbose())
			System.out.println("Projections: ");
		for(Set<String> projection : projections){
			if(projection.size()==1)
				continue;
			if(params.isVerbose())
				System.out.println(projection);
			XLog projectionLog = projectLogOnEventNames(params.getEvaluationLog(), projection);
			LocalProcessModelParameters paramsCopy = params.copy();
			paramsCopy.setDiscoveryLog(projectionLog);
			//paramsCopy.setEvaluationLog(projectionLog);
			
			LocalProcessModelRanking result = null;
			if(projection.size()>MAX_SIZE_THRESHOLD){
				if(projection.size()==info.getEventClasses().size()){
					subsequentFailedProjections++;
					inflationParameter = inflationParameter * 1.2;
				}else{
					subsequentFailedProjections = 0;
				}
				if(subsequentFailedProjections<=MAX_SUBSEQUENT_ATTEMPTS){
					result = runHeadless(context, subsequentFailedProjections, paramsCopy, processor, inflationParameter + 0.1);
				}else{
					if(params.isVerbose())
						System.err.println("Failed to make feasible projection in "+MAX_SUBSEQUENT_ATTEMPTS+" attempts");
					continue;
				}
			}else{
				result = processor.runHeadlessProjectionless(context, paramsCopy);
			}
			for(int i=0; i<result.getSize(); i++){
				container.add(result.getNet(i));
			}
		}
		LocalProcessModelRanking array = new LocalProcessModelRanking();
		for(LocalProcessModel net : container)
			array.addNet(net);
		return array;
	}
}