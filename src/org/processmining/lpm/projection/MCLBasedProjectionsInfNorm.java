package org.processmining.lpm.projection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.lpm.projection.util.LinfNorm;
import org.processmining.lpm.projection.util.VectorNorm;

public class MCLBasedProjectionsInfNorm implements Projector{
	
	
	public Set<Set<String>> getProjections(XLog log){
		Dfpg dfpg = ConvertLogToDfpg.log2Dfpg(log);
		VectorNorm lInfAggregator = new LinfNorm();
		Set<Set<XEventClass>> clustering = dfpg.applyMCLusteringAggregatorMLE(lInfAggregator, 1.5);
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