package org.processmining.lpm.experiments;

import java.util.concurrent.atomic.AtomicInteger;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.dialogs.LocalProcessModelParameters.ProjectionMethods;
import org.processmining.lpm.discovery.LocalProcessModelDiscovery;
import org.processmining.lpm.util.CompareResults;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(
		name = "Run experiments with Constraints and Projections", 
		parameterLabels = {"Input Log"}, 
		returnLabels = {"Input Log"}, 
		returnTypes = { XLog.class }
		)
public class ConstraintsAndProjectionsWorkflow {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Run experiments with Constraints and Projections", requiredParameterLabels = {0})
	public XLog run(PluginContext context, XLog log){
		LocalProcessModelParameters params = new LocalProcessModelParameters();
		params.setDiscoveryLog(log);
		params.setEvaluationLog(log);
		params.setSmartParameterDefaultsForEvaluationLog();
		params.setNumberOfExploredLpms(new AtomicInteger(0));
		params.setVerbose(false);
		LocalProcessModelDiscovery lpmd = new LocalProcessModelDiscovery();
		
		System.out.println("projection, eventGap, timeGap, duration, sss, ndcg@5, ndcg@10, ndcg@20, ndcg@100");
		for(int eventGap=-1; eventGap<5; eventGap++){ // eventGap==-1 represents no constraint
			for(int timeGap=0; timeGap<10; timeGap++){ // timeGap==0 represents no constraint
				if(eventGap==-1&&timeGap==0)
					params.setUseEfficientLog(false);
				else{
					params.setUseEfficientLog(true);
					if(timeGap>0)
						params.setMax_consecutive_timedif_millis(timeGap*60*1000);
					else
						params.setMax_consecutive_timedif_millis(Long.MAX_VALUE);
					if(eventGap>=0)
						params.setMax_consecutive_nonfitting(eventGap);
					else
						params.setMax_consecutive_nonfitting(Integer.MAX_VALUE);
				}
				params.setProjectionMethod(ProjectionMethods.None);
				params.setNumberOfExploredLpms(new AtomicInteger(0));
				Long startTime = System.currentTimeMillis();
				LocalProcessModelRanking gt = lpmd.runHeadless(context, params);
				Long endTime = System.currentTimeMillis();
				System.out.println("none,"+eventGap+","+timeGap+","+(endTime-startTime)+","+params.getNumberOfExploredLpms()+",1,1,1,1");
				
				params.setProjectionMethod(ProjectionMethods.Markov);
				params.setNumberOfExploredLpms(new AtomicInteger(0));
				startTime = System.currentTimeMillis();
				LocalProcessModelRanking mined = lpmd.runHeadless(context, params);
				endTime = System.currentTimeMillis();
				System.out.println("markov,"+eventGap+","+timeGap+","+(endTime-startTime)+","+params.getNumberOfExploredLpms()+","+CompareResults.ndcg(5, gt, mined)+","+CompareResults.ndcg(10, gt, mined)+","+CompareResults.ndcg(20, gt, mined)+","+CompareResults.ndcg(100, gt, mined));
			}
		}

		return log;
	}
}
