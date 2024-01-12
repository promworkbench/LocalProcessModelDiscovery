package org.processmining.lpm.projection;


import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import no.uib.cipr.matrix.DenseMatrix;

import org.apache.commons.collections15.iterators.ArrayIterator;
import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.graphs.Graph;
import org.processmining.plugins.InductiveMiner.graphs.GraphFactory;

public class LiftDfpg {
	private Graph<XEventClass> directlyFollowsGraph;

	private final MultiSet<XEventClass> startActivities;
	private final MultiSet<XEventClass> endActivities;
	private MultiSet<XEventClass> activitiesCounts;

	public LiftDfpg() {
		this(1);
	}

	public LiftDfpg(int initialSize) {
		directlyFollowsGraph = GraphFactory.create(XEventClass.class, initialSize);
		startActivities = new MultiSet<>();
		endActivities = new MultiSet<>();
		activitiesCounts = new MultiSet<>();
	}

	public LiftDfpg(Graph<XEventClass> directlyFollowsGraph) {
		this.directlyFollowsGraph = directlyFollowsGraph;
		startActivities = new MultiSet<>();
		endActivities = new MultiSet<>();
		activitiesCounts = new MultiSet<>();
	}

	public LiftDfpg(final Graph<XEventClass> directlyFollowsGraph, final Graph<XEventClass> directlyPrecedesGraph,
			final MultiSet<XEventClass> startActivities, final MultiSet<XEventClass> endActivities, final MultiSet<XEventClass> activities) {
		this.directlyFollowsGraph = directlyFollowsGraph;

		this.startActivities = startActivities;
		this.endActivities = endActivities;
		this.activitiesCounts = activities;		
	}

	public void addActivity(XEventClass activity) {
		directlyFollowsGraph.addVertex(activity);
		activitiesCounts.add(activity);
	}

	public Graph<XEventClass> getDirectlyFollowsGraph() {
		return directlyFollowsGraph;
	}
	
	
	public void setDirectlyFollowsGraph(Graph<XEventClass> directlyFollowsGraph) {
		this.directlyFollowsGraph = directlyFollowsGraph;
	}

	public Iterable<XEventClass> getActivities() {
		return new Iterable<XEventClass>() {
			public Iterator<XEventClass> iterator() {
				return new ArrayIterator<XEventClass>(directlyFollowsGraph.getVertices());
			}
		};

	}
	
	public Map<Integer, XEventClass> getMappingFromIDToXEventClass(){
		Iterator<XEventClass> activities = getActivities().iterator();
		Map<Integer, XEventClass> mapping = new HashMap<Integer, XEventClass>();
		int i =0;
		while(activities.hasNext()){
			XEventClass xec = activities.next();
			mapping.put(i, xec);
			i++;
		}
		return mapping;
	}

	public void setActivitiesCounts(MultiSet<XEventClass> counts){
		this.activitiesCounts = counts;
	}
	
	public MultiSet<XEventClass> getActivitiesCounts(){
		return activitiesCounts;
	}
	
	public MultiSet<XEventClass> getStartActivities() {
		return startActivities;
	}

	public MultiSet<XEventClass> getEndActivities() {
		return endActivities;
	}

	public void addEdge(final XEventClass source, final XEventClass target, final long cardinality) {
		directlyFollowsGraph.addEdge(source, target, cardinality);
	}

	public void addStartActivity(XEventClass activity, long cardinality) {
		startActivities.add(activity, cardinality);
	}

	public void addEndActivity(XEventClass activity, long cardinality) {
		endActivities.add(activity, cardinality);
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		for (long edgeIndex : directlyFollowsGraph.getEdges()) {
			result.append(directlyFollowsGraph.getEdgeSource(edgeIndex));
			result.append("->");
			result.append(directlyFollowsGraph.getEdgeTargetIndex(edgeIndex));
			result.append(", ");
		}
		return result.toString();
	}
	
	public static void printArray(double matrix[][]) {
	    for (double[] row : matrix) 
	        System.out.println(Arrays.toString(row));       
	}
	
	private void printMatrix(DenseMatrix maxMatrix) {
		Map<Integer, XEventClass> mapping = getMappingFromIDToXEventClass();
		DecimalFormat df = new DecimalFormat("#.00"); 
		for(int i=0; i<maxMatrix.numRows(); i++){
			System.out.println();
			System.out.print(padRight(mapping.get(i).getId(),15)+" ");
			for(int j=0; j<maxMatrix.numColumns(); j++){
				System.out.print(df.format(maxMatrix.get(i, j))+"   ");
			}
		}
		System.out.println();
	}
	
	public static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}
}