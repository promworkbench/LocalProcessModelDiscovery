package org.processmining.lpm.util;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

import com.google.common.math.DoubleMath;

@Plugin(
		name = "Compare Utility Local Process Model Results", 
		parameterLabels = {"Ground truth", "Obtained"}, 
	    returnLabels = {"Double"}, 
	    returnTypes = { String.class }
		)
public class CompareResultsUtility {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Compare Utility Local Process Model Result", requiredParameterLabels = {0,1})
	public static String compare(PluginContext context, UtilityLocalProcessModelRanking res1, UtilityLocalProcessModelRanking res2) {
		StringBuilder s = new StringBuilder();
		s.append("nDCG@5: "+ndcg(5, res1, res2));
		s.append(System.getProperty("line.separator"));
		s.append("nDCG@10: "+ndcg(10, res1, res2));
		s.append(System.getProperty("line.separator"));
		s.append("nDCG@20: "+ndcg(20, res1, res2));
		System.out.println(s.toString());
		return s.toString();
	}
	
	public static double ndcg(UtilityLocalProcessModelRanking truth, UtilityLocalProcessModelRanking mined){
		return ndcg(truth.getSize(), truth, mined);
	}
	
	public static double ndcg(int p, UtilityLocalProcessModelRanking truth, UtilityLocalProcessModelRanking mined){
		UtilityLocalProcessModel bestUtilityModel = truth.getElement(0);
		double scaleFactor = bestUtilityModel.getUtility();
		double minedD = dcg(p, mined, scaleFactor);
		double truthD = dcg(p, truth, scaleFactor);
		return minedD/truthD;
	}
	
	public static double dcg(int p, UtilityLocalProcessModelRanking results, double scaleFactor){
		double total = 0d;
		for(int i=0; i<results.getSize(); i++){
			if(i>=p)
				break;
			if(!(results.getNet(i) instanceof UtilityLocalProcessModel)){
				System.err.println("Error, ranking does not contain utility LPMs");
				return 0d;
			}
			UtilityLocalProcessModel ulpm = (UtilityLocalProcessModel) results.getNet(i);
			double toAdd = (Math.pow(2, ulpm.getUtility()/scaleFactor)-1)/(DoubleMath.log2(i+2));
			total += toAdd;
		}
		return total;
	}
	
	public static double dcg(int p, UtilityLocalProcessModelRanking results){
		double total = 0d;
		for(int i=0; i<results.getSize(); i++){
			if(i>=p)
				break;
			if(!(results.getNet(i) instanceof UtilityLocalProcessModel)){
				System.err.println("Error, ranking does not contain utility LPMs");
				return 0d;
			}
			UtilityLocalProcessModel ulpm = (UtilityLocalProcessModel) results.getNet(i);
			double toAdd = (Math.pow(2, ulpm.getUtility())-1)/(DoubleMath.log2(i+2));
			total += toAdd;
		}
		return total;
	}
}
