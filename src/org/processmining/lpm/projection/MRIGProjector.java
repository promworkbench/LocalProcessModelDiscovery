package org.processmining.lpm.projection;

import static org.processmining.lpm.util.LogUtils.projectLogOnEventNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.graphs.Graph;

import com.google.common.math.DoubleMath;


public class MRIGProjector implements Projector{
	private double entropyThreshold = 0.1;
	private XLog log;
	
	public MRIGProjector(){
	}
	
	public MRIGProjector(double entropyThreshold){
		this.entropyThreshold = entropyThreshold;
	}
	
	public Set<Set<String>> getProjections(XLog log) {
		this.log = log;
		Set<Set<String>> projectionSet = new HashSet<Set<String>>();
		// build initial set
		XEventClasses info = XLogInfoFactory.createLogInfo(log, new XEventNameClassifier()).getEventClasses();
		for(int i=0; i<info.size(); i++){
			Set<String> sizeOneSet = new HashSet<String>();
			sizeOneSet.add(info.getByIndex(i).getId());
			projectionSet.add(sizeOneSet);
		}
		Map<Integer, Set<Set<String>>> projections = new HashMap<Integer, Set<Set<String>>>();
		projections.put(1, projectionSet);
		projections = recurseProjectionSetGrowth(projections, 1);
		List<Integer> is = new ArrayList<Integer>(projections.keySet());
		Collections.sort(is);
		Collections.reverse(is);
		for(int i : is){
			Set<Set<String>> projectionsAtI = projections.get(i);
			for(Set<String> projection : projectionsAtI){
				boolean foundCoveringProjection = false;
				for(Set<String> existingProjection : projectionSet){
					if(existingProjection.containsAll(projection)){
						foundCoveringProjection = true;
						break;
					}
				}
				if(!foundCoveringProjection)
					projectionSet.addAll(projections.get(i));
			}
		}
		return projectionSet;
	}
	
	public Map<Integer, Set<Set<String>>> recurseProjectionSetGrowth(Map<Integer, Set<Set<String>>> projectionSet, int iteration){
		Iterator<Set<String>> iterator1 = projectionSet.get(iteration).iterator();
		Iterator<Set<String>> iterator2 = projectionSet.get(iteration).iterator();
		Set<Set<String>> newProjectionSetElements = new HashSet<Set<String>>();
		while(iterator1.hasNext()){
			Set<String> elem1 = iterator1.next();
			while(iterator2.hasNext()){
				Set<String> elem2 = iterator2.next();
				Set<String> joined = new HashSet<String>();
				joined.addAll(elem1);
				joined.addAll(elem2);
				if(joined.size()!=iteration+1)
					continue;
				XLog projectedLogElem1 = projectLogOnEventNames(log, elem1);
				XLog projectedLogElem2 = projectLogOnEventNames(log, elem2);
				XLog projectedLogJoined = projectLogOnEventNames(log, joined);
				System.out.println("calculating gain of "+joined+" over"+elem1);
				double informationGain1 = getLogRelationBasedRelativeInformationGain(projectedLogElem1, projectedLogJoined);
				System.out.println("calculating gain of "+joined+" over"+elem2);
				double informationGain2 = getLogRelationBasedRelativeInformationGain(projectedLogElem2, projectedLogJoined);
				System.out.println("informationGain1: "+informationGain1);
				System.out.println("informationGain2: "+informationGain2);
				System.out.println("entropyThreshold: "+entropyThreshold);
				//if((informationGain1!=0 &&informationGain1>entropyThreshold) || (informationGain2!=0 && informationGain2>entropyThreshold)){
				if(informationGain1<entropyThreshold || informationGain2<entropyThreshold){
					newProjectionSetElements.add(joined);
					System.out.println("added: "+joined);
				}
				System.out.println();

				/*
				if(joined.equals(elem1) || joined.equals(elem2)) // TODO: checken, is dit nodig?
					continue;
				*/
				
			}
		}
		if(newProjectionSetElements.isEmpty())
			return projectionSet;
		else{
			System.out.println("new found: "+newProjectionSetElements);
			projectionSet.put(iteration+1, newProjectionSetElements);
			return recurseProjectionSetGrowth(projectionSet, iteration+1);
		}
	}
	
	public double getLogRelationBasedRelativeInformationGain(XLog originalLog, XLog newLog){
		return getEntropyFromDfpg(ConvertLogToDfpg.log2Dfpg(originalLog), ConvertLogToDfpg.log2Dfpg(newLog));
	}

	private static double getEntropyFromDfpg(Dfpg originalDfpg, Dfpg newDfpg) {
		Graph<XEventClass> dfg = originalDfpg.getDirectlyFollowsGraph();
		Graph<XEventClass> dfg2 = newDfpg.getDirectlyFollowsGraph();
		double result = 1d;
		for(XEventClass xec : originalDfpg.getActivities()){
			// Calculate original distributions for xec
			long total = 0;
			List<Long> edgeWeightList = new LinkedList<Long>();
			for(Long edgeId : dfg.getOutgoingEdgesOf(xec)){
				long w = dfg.getEdgeWeight(edgeId);
				if(w>0){
					total += w;
					edgeWeightList.add(w);
				}else{
					System.err.println("Zero weight for original dfg edge id: "+edgeId);
				}
			}
			Long endCount = originalDfpg.getEndActivities().getCardinalityOf(xec);
			if(endCount>0){
				total += endCount;
				edgeWeightList.add(endCount);
			}else{
				System.err.println("Zero weight for original dfpg end activity: "+xec.getId());
			}

			double[] distribution = new double[edgeWeightList.size()];
			for(int i=0; i<edgeWeightList.size(); i++){
				distribution[i] = ((double)edgeWeightList.get(i))/total;
				if(Double.isNaN(distribution[i]))
					System.err.println("edgeWeight: "+edgeWeightList.get(i)+", total: "+total+" is NaN");
			}
			double originalEntropy = getEntropyFromDistribution(distribution);
			
			// Calculate new distributions for xec
			long total2 = 0;
			List<Long> edgeWeightList2 = new LinkedList<Long>();
			for(Long edgeId : dfg2.getOutgoingEdgesOf(xec)){
				long w = dfg2.getEdgeWeight(edgeId);
				if(w>0){
					total2 += w;
					edgeWeightList2.add(w);
				}else{
					System.err.println("Zero weight for refined dfg edge id: "+edgeId);
				}
			}
			Long endCount2 = newDfpg.getEndActivities().getCardinalityOf(xec);
			if(endCount2>0){
				total2 += endCount2;
				edgeWeightList2.add(endCount2);
			}else{
				System.err.println("Zero weight for refined dfpg end activity: "+xec.toString());
			}

			double[] distribution2 = new double[edgeWeightList2.size()];
			for(int i=0; i<edgeWeightList2.size(); i++){
				distribution2[i] = ((double)edgeWeightList2.get(i))/total2;
				if(Double.isNaN(distribution2[i]))
					System.err.println("edgeWeight: "+edgeWeightList2.get(i)+", total: "+total2+" is NaN");
			}
			double newEntropy = getEntropyFromDistribution(distribution2);
			double gain = (newEntropy-originalEntropy)/originalEntropy;
			if(Double.isNaN(gain))
				gain = 0;
			System.out.println("comparing distribution "+printDoubleArray(distribution)+" (entropy: "+originalEntropy+") with "+printDoubleArray(distribution2)+" (entropy: "+newEntropy+"), gain = "+gain);
			result = Math.min(result, gain);
		}
		
		for(XEventClass xec : originalDfpg.getActivities()){
			// Calculate original distributions for xec
			long total = 0;
			List<Long> edgeWeightList = new LinkedList<Long>();
			for(Long edgeId : dfg.getIncomingEdgesOf(xec)){
				long w = dfg.getEdgeWeight(edgeId);
				if(w>0){
					total += w;
					edgeWeightList.add(w);
				}else{
					System.err.println("Zero weight for original dpg edge id: "+edgeId);
				}
			}
			Long startCount = originalDfpg.getStartActivities().getCardinalityOf(xec);
			if(startCount>0){
				total += startCount;
				edgeWeightList.add(startCount);
			}else{
				System.err.println("Zero weight for original dfpg start activity: "+xec.getId());
			}

			double[] distribution = new double[edgeWeightList.size()];
			for(int i=0; i<edgeWeightList.size(); i++){
				distribution[i] = ((double)edgeWeightList.get(i))/total;
				if(Double.isNaN(distribution[i]))
					System.err.println("edgeWeight: "+edgeWeightList.get(i)+", total: "+total+" is NaN");
			}
			double originalEntropy = getEntropyFromDistribution(distribution);
			
			// Calculate new distributions for xec
			long total2 = 0;
			List<Long> edgeWeightList2 = new LinkedList<Long>();
			for(Long edgeId : dfg2.getIncomingEdgesOf(xec)){
				long w = dfg2.getEdgeWeight(edgeId);
				if(w>0){
					total2 += dfg2.getEdgeWeight(edgeId);
					edgeWeightList2.add(dfg2.getEdgeWeight(edgeId));
				}else{
					System.err.println("Zero weight for refined dpg edge id: "+edgeId);
				}
			}
			Long startCount2 = newDfpg.getStartActivities().getCardinalityOf(xec);
			if(startCount2>0){
				total2 += startCount2;
				edgeWeightList2.add(startCount2);
			}else{
				System.err.println("Zero weight for refined dfpg start activity: "+xec.getId());
			}

			double[] distribution2 = new double[edgeWeightList2.size()];
			for(int i=0; i<edgeWeightList2.size(); i++){
				distribution2[i] = ((double)edgeWeightList2.get(i))/total2;
				if(Double.isNaN(distribution2[i]))
					System.err.println("edgeWeight: "+edgeWeightList2.get(i)+", total: "+total2+" is NaN");
			}
			double newEntropy = getEntropyFromDistribution(distribution2);
			double gain = (newEntropy-originalEntropy)/originalEntropy;
			if(Double.isNaN(gain))
				gain = 0;
			System.out.println("comparing distribution "+printDoubleArray(distribution)+" (entropy: "+originalEntropy+") with "+printDoubleArray(distribution2)+" (entropy: "+newEntropy+"), gain = "+gain);
			result = Math.min(result, gain);
		}
		return result;
	}
	
	public static double getEntropyFromDistribution(double[] distribution){
		/*
		double sum = 0d;
		for(double d : distribution)
			sum+=d;
		
		if(sum!=1)
			System.err.println("distribution sums to "+sum+"!");
		*/
		double entropy = 0d;
		for(double d : distribution)
			if(d>0)
				entropy += -d*DoubleMath.log2(d);
		if(Double.isNaN(entropy))
			System.err.println("Entropy of distribution is NaN: "+Arrays.toString(distribution));
		return entropy;
	}
	
	public static String printDoubleArray(double[] array){
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		boolean nonfirst = false;
		for(double a : array){
			if(nonfirst)
				sb.append(", ");
			sb.append(a);
			nonfirst = true;
		}
		sb.append(']');
		return sb.toString();
	}
}