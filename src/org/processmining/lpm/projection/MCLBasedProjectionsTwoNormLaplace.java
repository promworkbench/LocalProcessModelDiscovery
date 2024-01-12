package org.processmining.lpm.projection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.lpm.projection.util.L2Norm;
import org.processmining.lpm.projection.util.VectorNorm;

public class MCLBasedProjectionsTwoNormLaplace implements Projector{
	private double inflationParameter = 1.5;
	private boolean verbose = true;
	
	public MCLBasedProjectionsTwoNormLaplace(double inflationParameter, boolean verbose){
		this.inflationParameter = inflationParameter;
		this.verbose = verbose;
	}
	
	public MCLBasedProjectionsTwoNormLaplace(double inflationParameter){
		this.inflationParameter = inflationParameter;
	}
	
	public Set<Set<String>> getProjections(XLog log){
		Dfpg dfpg = ConvertLogToDfpg.log2Dfpg(log);
		dfpg.setVerbose(verbose);
		VectorNorm l2Aggregator = new L2Norm();
		Set<Set<XEventClass>> clustering = dfpg.applyMCLusteringAggregatorLaplace(l2Aggregator, inflationParameter);
		Iterator<Set<XEventClass>> clusterIterator = clustering.iterator();
		Set<Set<String>> result = new HashSet<Set<String>>();
		while(clusterIterator.hasNext()){
			Set<XEventClass> cluster = clusterIterator.next();
			Iterator<XEventClass> clusterMemberIterator = cluster.iterator();
			Set<String> projection = new HashSet<String>(); 
			while(clusterMemberIterator.hasNext()){
				XEventClass memberClass = clusterMemberIterator.next();
				projection.add(memberClass.getId());
			}
			result.add(projection);
		}
		return result;
	}
}