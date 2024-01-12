package org.processmining.lpm.util;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

import com.google.common.math.DoubleMath;

@Plugin(
		name = "Compare Local Process Model Results", 
		parameterLabels = {"Ground truth", "Obtained"}, 
	    returnLabels = {"Double"}, 
	    returnTypes = { String.class }
		)
public class CompareResults {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Compare Local Process Model Result", requiredParameterLabels = {0,1})
	public static String compare(PluginContext context, LocalProcessModelRanking res1, LocalProcessModelRanking res2) {
		StringBuilder s = new StringBuilder();
		s.append("Recall:  "+recall(res1, res2));
		s.append(System.getProperty("line.separator"));
		s.append("nDCG@5: "+ndcg(5, res1, res2));
		s.append(System.getProperty("line.separator"));
		s.append("nDCG@10: "+ndcg(10, res1, res2));
		s.append(System.getProperty("line.separator"));
		s.append("nDCG@20: "+ndcg(20, res1, res2));
		System.out.println(s.toString());
		return s.toString();
		
	}
	
	public static double ndcg(LocalProcessModelRanking truth, LocalProcessModelRanking mined){
		int p = truth.getSize();
		double minedD = dcg(p, mined);
		double truthD = dcg(p, truth);
		return minedD/truthD; // TODO: check this method, can numerical stability cause values > 1?
	}
	
	public static double ndcg(int p, LocalProcessModelRanking truth, LocalProcessModelRanking mined){
		double minedD = dcg(p, mined);
		double truthD = dcg(p, truth);
		return minedD/truthD; // TODO: check this method, can numerical stability cause values > 1?
	}
	
	public static double dcg(int p, LocalProcessModelRanking results){
		double total = 0d;
		//DecimalFormat df = new DecimalFormat("#.0000"); 
		for(int i=0; i<results.getSize(); i++){
			if(i>=p)
				break;
			//System.out.print(df.format(total));
			double toAdd = (Math.pow(2, results.getNet(i).getWeightedScore())-1)/(DoubleMath.log2(i+2));
			total += toAdd;
			//System.out.println("  "+df.format(results.getNet(i).weightedValue())+": + "+df.format(toAdd)+" = "+df.format(total)+"   ("+results.getNet(i).getLanguage()+")");
		}
		return total;
	}
	
	public static double recall(LocalProcessModelRanking truth, LocalProcessModelRanking mined){
		int numFound = 0;
		for(int i=0; i<truth.getSize(); i++){
			LocalProcessModel apn1 = truth.getNet(i);
			boolean found = false;
			for(int j=0; j<mined.getSize(); j++){
				LocalProcessModel apn2 = mined.getNet(j);
				if(apn1.equals(apn2)){
					found = true;
					break;
				}
			}
			if(found)
				numFound++;
		}
		Double retVal = ((double) numFound)/truth.getSize();
		return retVal;
	}
}
