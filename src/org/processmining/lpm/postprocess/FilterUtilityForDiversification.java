package org.processmining.lpm.postprocess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.UtilityLocalProcessModelRanking;

@Plugin(
		name = "Filter Utility Local Process Model ranking for Diversification", 
		parameterLabels = {"Local Process Model ranking"}, 
	    returnLabels = {"Filtered Local Process Model ranking"}, 
	    returnTypes = { UtilityLocalProcessModelRanking.class }
		)
public class FilterUtilityForDiversification{
	private static final double DIVERSITY_THRESHOLD = 0.67;
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Filter Local Process Model ranking for Diversification", requiredParameterLabels = {0})
	public LocalProcessModelRanking filter(PluginContext context, UtilityLocalProcessModelRanking nets) {
		return filterHeadless(nets, DIVERSITY_THRESHOLD);
	}
	
	public static LocalProcessModelRanking filterHeadless(UtilityLocalProcessModelRanking nets, double diversity_threshold) {
		LocalProcessModelRanking array = new LocalProcessModelRanking();
		List<LocalProcessModel> filteredLPMs = new ArrayList<LocalProcessModel>();
		Set<Set<String>> alphabets = new HashSet<Set<String>>();
		for(int i=0; i<nets.getSize(); i++){
			LocalProcessModel scoredNet = nets.getNet(i);
			double diversityScore = 1-getHighestJaccardSimilarity(alphabets, scoredNet.getAlphabet());
			alphabets.add(scoredNet.getAlphabet());
			if(diversityScore>diversity_threshold){
				System.out.println("diversity: "+diversityScore);
				filteredLPMs.add(nets.getNet(i));
			}
		}
		for(LocalProcessModel net : filteredLPMs)
			array.addNet(net);
		return array;
	}
	
	public static double getHighestJaccardSimilarity(Set<Set<String>> elements, Set<String> comparingElement){
		double highest = 0;
		for(Set<String> element : elements){
			Set<String> overlap = new HashSet<String>(element);
			overlap.retainAll(comparingElement);
			double jaccard = ((double)overlap.size()) / (element.size() + comparingElement.size() - overlap.size());
			if(jaccard>highest)
				highest = jaccard;
		}
		return highest;
	}
}