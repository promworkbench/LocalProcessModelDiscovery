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

public class RelativeEntropyBasedProjector implements Projector{
	private double entropyThreshold = -0.1;
	private XLog log;
	
	public RelativeEntropyBasedProjector(){
	}
	
	public RelativeEntropyBasedProjector(double entropyThreshold){
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
					projectionSet.add(projection);
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
				double informationGain1 = getLogRelationBasedRelativeInformationGain(projectedLogElem1, projectedLogJoined);
				double informationGain2 = getLogRelationBasedRelativeInformationGain(projectedLogElem2, projectedLogJoined);
				System.out.println("informationGain1: "+informationGain1);
				System.out.println("informationGain2: "+informationGain2);
				System.out.println("entropyThreshold: "+entropyThreshold);
				//if((informationGain1!=0 &&informationGain1>entropyThreshold) || (informationGain2!=0 && informationGain2>entropyThreshold)){
				if(informationGain1>entropyThreshold || informationGain2>entropyThreshold){
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
	
	public double getLogRelationBasedRelativeInformationGain(XLog originalLog, XLog refinedLog){
		double oldEntropy = getEntropyFromDfpg(ConvertLogToDfpg.log2Dfpg(originalLog));
		double newEntropy = getEntropyFromDfpg(ConvertLogToDfpg.log2Dfpg(refinedLog));
		return oldEntropy-newEntropy;
	}

	private static double getEntropyFromDfpg(Dfpg originalDfpg) {
		double oldEntropy = 0d;
		Graph<XEventClass> dfg = originalDfpg.getDirectlyFollowsGraph();
		int divisor = 0;
		for(XEventClass xec : originalDfpg.getActivities()){
			long total = 0;
			List<Long> edgeWeightList = new LinkedList<Long>();
			for(Long edgeId : dfg.getOutgoingEdgesOf(xec)){
				total += dfg.getEdgeWeight(edgeId);
				edgeWeightList.add(dfg.getEdgeWeight(edgeId));
			}
			Long endCount = originalDfpg.getEndActivities().getCardinalityOf(xec);
			total += endCount;
			edgeWeightList.add(endCount);

			double[] distribution = new double[edgeWeightList.size()];
			for(int i=0; i<edgeWeightList.size(); i++){
				distribution[i] = ((double)edgeWeightList.get(i))/total;
				if(Double.isNaN(distribution[i]))
					System.err.println("edgeWeight: "+edgeWeightList.get(i)+", total: "+total+" is NaN");
			}
			
			divisor++;
			oldEntropy += getEntropyFromDistribution(distribution);
		}
		
		for(XEventClass xec : originalDfpg.getActivities()){
			long total = 0;
			List<Long> edgeWeightList = new LinkedList<Long>();
			for(Long edgeId : dfg.getIncomingEdgesOf(xec)){
				total += dfg.getEdgeWeight(edgeId);
				edgeWeightList.add(dfg.getEdgeWeight(edgeId));
			}
			Long startCount = originalDfpg.getStartActivities().getCardinalityOf(xec);
			total += startCount;
			edgeWeightList.add(startCount);
			
			double[] distribution = new double[edgeWeightList.size()];
			for(int i=0; i<edgeWeightList.size(); i++){
				distribution[i] = ((double)edgeWeightList.get(i))/total;
				if(Double.isNaN(distribution[i]))
					System.err.println("edgeWeight: "+edgeWeightList.get(i)+", total: "+total+" is NaN");
			}
			divisor++;
			oldEntropy += getEntropyFromDistribution(distribution);
		}
		return oldEntropy/divisor;
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
}
