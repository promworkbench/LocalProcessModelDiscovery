package org.processmining.lpm.postprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(
		name = "Re-rank Local Process Model ranking for Diversification", 
		parameterLabels = {"Local Process Model ranking"}, 
	    returnLabels = {"Reweighed Local Process Model ranking"}, 
	    returnTypes = { LocalProcessModelRanking.class }
		)
public class ReweighForDiversification{
	private static final double DIVERSIFICATION_PARAMETER = 0.5;
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Re-rank Local Process Model ranking for Diversification", requiredParameterLabels = {0})
	public LocalProcessModelRanking rerank(PluginContext context, LocalProcessModelRanking nets) {
		return rerankHeadless(nets, DIVERSIFICATION_PARAMETER);
	}
	
	public static LocalProcessModelRanking rerankHeadless(LocalProcessModelRanking nets, double diversification_parameter) {
		LocalProcessModelRanking array = new LocalProcessModelRanking();
		Map<LocalProcessModel, Double> reweighedScores = new HashMap<LocalProcessModel, Double>();
		Set<Set<String>> alphabets = new HashSet<Set<String>>();
		for(int i=0; i<nets.getSize(); i++){
			LocalProcessModel scoredNet = nets.getNet(i);
			double highestJaccardSimilarity = getHighestJaccardSimilarity(alphabets, scoredNet.getAlphabet());
			alphabets.add(scoredNet.getAlphabet());
			System.out.println(highestJaccardSimilarity);
			double reweighedScore = (1-diversification_parameter) * scoredNet.getWeightedScore() + diversification_parameter * (1-highestJaccardSimilarity);
			reweighedScores.put(nets.getNet(i), reweighedScore);
		}
		List<LocalProcessModel> reweighedNets = new ArrayList<LocalProcessModel>(sortByValue(reweighedScores).keySet());
		for(LocalProcessModel net : reweighedNets)
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
	
	private static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
	    List<Entry<K, V>> list = new LinkedList<>(map.entrySet());
	    Collections.sort(list, new Comparator<Object>() {
	        @SuppressWarnings("unchecked")
	        public int compare(Object o1, Object o2) {
	            return ((Comparable<V>) ((Map.Entry<K, V>) (o1)).getValue()).compareTo(((Map.Entry<K, V>) (o2)).getValue());
	        }
	    });
	    Collections.reverse(list);
	    Map<K, V> result = new LinkedHashMap<>();
	    for (Iterator<Entry<K, V>> it = list.iterator(); it.hasNext();) {
	        Map.Entry<K, V> entry = (Map.Entry<K, V>) it.next();
	        result.put(entry.getKey(), entry.getValue());
	    }

	    return result;
	}
}