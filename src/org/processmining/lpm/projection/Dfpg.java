package org.processmining.lpm.projection;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.iterators.ArrayIterator;
import org.deckfour.xes.classification.XEventClass;
import org.processmining.lpm.projection.util.VectorNorm;
import org.processmining.markovclustering.algorithms.clustering.impl.MCLAlgorithm_JAMA_Par;
import org.processmining.markovclustering.algorithms.clustering.impl.ProvidedMatrixClusterer;
import org.processmining.markovclustering.models.MCLMatrix;
import org.processmining.markovclustering.parameters.impl.MarkovClusteringParameters;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.graphs.Graph;
import org.processmining.plugins.InductiveMiner.graphs.GraphFactory;

import no.uib.cipr.matrix.DenseMatrix;

public class Dfpg {
	private Graph<XEventClass> directlyFollowsGraph;

	private final MultiSet<XEventClass> startActivities;
	private final MultiSet<XEventClass> endActivities;
	private MultiSet<XEventClass> activitiesCounts;

	private boolean verbose = true;
	
	public Dfpg() {
		this(1);
	}

	public Dfpg(int initialSize) {
		directlyFollowsGraph = GraphFactory.create(XEventClass.class, initialSize);
		startActivities = new MultiSet<>();
		endActivities = new MultiSet<>();
		activitiesCounts = new MultiSet<>();
	}

	public Dfpg(Graph<XEventClass> directlyFollowsGraph, Graph<XEventClass> directlyPrecedesGraph) {
		this.directlyFollowsGraph = directlyFollowsGraph;
		startActivities = new MultiSet<>();
		endActivities = new MultiSet<>();
		activitiesCounts = new MultiSet<>();
	}

	public Dfpg(final Graph<XEventClass> directlyFollowsGraph, final Graph<XEventClass> directlyPrecedesGraph,
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
	
	public void setVerbose(boolean verbose){
		this.verbose = verbose;
	}
	
	public void setDirectlyFollowsGraph(Graph<XEventClass> directlyFollowsGraph) {
		this.directlyFollowsGraph = directlyFollowsGraph;
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
	
	public MCLMatrix getMLEDpgMatrix(){
		Map<Integer, XEventClass> mapping = getMappingFromIDToXEventClass();
		int numUniqueActivities = mapping.keySet().size();
		double[][] values = new double[numUniqueActivities][numUniqueActivities];
		for(int i : mapping.keySet()){
			for(int j : mapping.keySet()){
				long cardj = activitiesCounts.getCardinalityOf(mapping.get(j));
				Long weight = directlyFollowsGraph.getEdgeWeight(i, j);
				Double weightAsDouble = weight == null ? 0d : (double) weight; // MLE estimate of categorical distribution over preceding events of activity j
				if(weightAsDouble>0 && cardj>0)
					weightAsDouble /= cardj;
				values[i][j] = weightAsDouble;
			}
		}
		if(verbose)
			printArray(values);
		MCLMatrix dMatrix = new MCLMatrix(values);
		return dMatrix;
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
	
	public MCLMatrix getLaplaceDpgMatrix(){
		Map<Integer, XEventClass> mapping = getMappingFromIDToXEventClass();
		int numUniqueActivities = mapping.keySet().size();
		double[][] values = new double[numUniqueActivities][numUniqueActivities];
		for(int i : mapping.keySet()){
			for(int j : mapping.keySet()){
				long cardj = activitiesCounts.getCardinalityOf(mapping.get(j));
				Long weight = directlyFollowsGraph.getEdgeWeight(i, j);
				Double weightAsDouble = weight == null ? 0d : (double) weight;
				weightAsDouble = (cardj*weightAsDouble+(1d/numUniqueActivities)) / cardj+1; // Laplace estimate of categorical distribution over preceding events of activity j
				values[i][j] = weightAsDouble;
			}
		}
		if(verbose)
			printArray(values);
		return new MCLMatrix(values);
	}
	
	public static void printArray(double matrix[][]) {
	    for (double[] row : matrix) 
	        System.out.println(Arrays.toString(row));       
	}
	
	public MCLMatrix getMLEDfgMatrix(){
		Map<Integer, XEventClass> mapping = getMappingFromIDToXEventClass();
		int numUniqueActivities = mapping.keySet().size();
		double[][] values = new double[numUniqueActivities][numUniqueActivities];
		for(int i : mapping.keySet()){
			long cardi = activitiesCounts.getCardinalityOf(mapping.get(i));
			for(int j : mapping.keySet()){
				Long weight = directlyFollowsGraph.getEdgeWeight(i, j);
				Double weightAsDouble = weight == null ? 0d : (double) weight;
				if(weightAsDouble>0 && cardi>0)
					weightAsDouble /= cardi;
				values[i][j] = weightAsDouble;
			}
		}
		return new MCLMatrix(values);
	}
	
	public MCLMatrix getLaplaceDfgMatrix(){
		Map<Integer, XEventClass> mapping = getMappingFromIDToXEventClass();
		int numUniqueActivities = mapping.keySet().size();
		double[][] values = new double[numUniqueActivities][numUniqueActivities];
		for(int i : mapping.keySet()){
			long cardi = activitiesCounts.getCardinalityOf(mapping.get(i));
			for(int j : mapping.keySet()){
				Long weight = directlyFollowsGraph.getEdgeWeight(i, j);
				Double weightAsDouble = weight == null ? 0d : (double) weight;
				weightAsDouble = (cardi*weightAsDouble+(1d/numUniqueActivities)) / (cardi+1); // Laplace estimate of categorical distribution over preceding events of activity j
				values[i][j] = weightAsDouble;
			}
		}
		return new MCLMatrix(values);
	}
	
	private Set<Set<XEventClass>> clusterMCL(MCLMatrix tm, double inflationParameter) {
		System.out.println("input matrix: ");
		printMatrix(toRowStochasticMatrix(tm));
		
		MCLAlgorithm_JAMA_Par mclAlg = new MCLAlgorithm_JAMA_Par();
		MarkovClusteringParameters param = new MarkovClusteringParameters();
		param.setVerbose(false);
		param.setMaxIterationsParameter(10000);
		param.setInflationParameter(inflationParameter);
		ProvidedMatrixClusterer<XEventClass> pmc = new ProvidedMatrixClusterer<XEventClass>(toRowStochasticMatrix(tm));
		XEventClass[] idToXec = new XEventClass[getMappingFromIDToXEventClass().size()];
		for(int i : getMappingFromIDToXEventClass().keySet())
			idToXec[i] = getMappingFromIDToXEventClass().get(i);
		return pmc.cluster(Arrays.asList(idToXec), mclAlg, param);
	}
	
	public Set<Set<XEventClass>> applyMCLonDPGMatrix(){
		MCLMatrix dpgMatrix = getMLEDpgMatrix();
		return clusterMCL(dpgMatrix, 1.5);
	}
	
	public Set<Set<XEventClass>> applyMCLonDFGMatrix(){
		MCLMatrix dfgMatrix = getMLEDfgMatrix();
		return clusterMCL(dfgMatrix, 1.5);
	}
	
	public Set<Set<XEventClass>> applyMCLusteringAggregatorMLE(VectorNorm aggregator, double inflationParameter){
		List<DenseMatrix> matrices = new ArrayList<DenseMatrix>();
		matrices.add(getMLEDfgMatrix());
		matrices.add(getMLEDpgMatrix());
		MCLMatrix normMatrix = new MCLMatrix(matrices.get(0), true);
		for(int i=0; i<normMatrix.numRows(); i++){
			for(int j=0; j<normMatrix.numColumns(); j++){
				double[] row = new double[matrices.size()];
				for(int k=0; k<matrices.size();k++){
					row[k] = matrices.get(k).get(i, j);
				}
				normMatrix.set(i,j, aggregator.aggregate(row));
			}
		}
		if(verbose){
			System.out.println("input matrix: ");
			printMatrix(normMatrix);
		}
		return clusterMCL(normMatrix, inflationParameter);
	}
	
	public MCLMatrix toRowStochasticMatrix(MCLMatrix matrix){
		for(int row=0; row<matrix.numRows(); row++){
			double sum = 0;
			for(int col=0; col<matrix.numColumns(); col++)
				sum += matrix.get(row, col);
			for(int col=0; col<matrix.numColumns(); col++)
				if(sum>0)
					matrix.set(row, col, matrix.get(row, col)/sum);
		}
		
		return matrix;
	}
	
	public Set<Set<XEventClass>> applyMCLusteringAggregatorLaplace(VectorNorm aggregator, double inflationParameter){
		List<DenseMatrix> matrices = new ArrayList<DenseMatrix>();
		matrices.add(getLaplaceDfgMatrix());
		matrices.add(getLaplaceDpgMatrix());
		MCLMatrix normMatrix = new MCLMatrix(matrices.get(0), true);
		for(int i=0; i<normMatrix.numRows(); i++){
			for(int j=0; j<normMatrix.numColumns(); j++){
				double[] row = new double[matrices.size()];
				for(int k=0; k<matrices.size();k++){
					row[k] = matrices.get(k).get(i, j);
				}
				normMatrix.set(i,j, aggregator.aggregate(row));
			}
		}
		if(verbose){
			System.out.println("input matrix: ");
			printMatrix(toRowStochasticMatrix(normMatrix));
		}
		return clusterMCL(normMatrix, inflationParameter);
	}
	
	public Set<Set<XEventClass>> applyMCLonNormMatrix(Integer norm, double inflationParameter){
		List<DenseMatrix> matrices = new ArrayList<DenseMatrix>();
		matrices.add(getMLEDfgMatrix());
		matrices.add(getMLEDpgMatrix());
		MCLMatrix normMatrix = new MCLMatrix(matrices.get(0), true);
		for(int i=0; i<normMatrix.numRows(); i++){
			for(int j=0; j<normMatrix.numColumns(); j++){
				double normValue = normMatrix.get(i, j);
				if(norm>1&&norm<Integer.MAX_VALUE)
					normValue = Math.pow(normValue, norm);
				for(int k=1; k<matrices.size();k++){
					if(norm==Integer.MAX_VALUE){ // treat Integer.MAX_VALUE as infinity
						if(matrices.get(k).get(i, j)>normValue)
							normValue = matrices.get(k).get(i, j);
					}else{
						normValue += Math.pow(matrices.get(k).get(i, j), norm);
					}
				}
				normMatrix.set(i,j, normValue);
			}
		}
		if(verbose){
			System.out.println("input matrix: ");
			printMatrix(normMatrix);
		}

		return clusterMCL(normMatrix, inflationParameter);
	}

	private void printMatrix(MCLMatrix maxMatrix) {
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