package org.processmining.lpm.visualization;

import java.util.HashMap;

import javax.swing.JComponent;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.projection.Dfpg;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.graphviz.colourMaps.ColourMap;
import org.processmining.plugins.graphviz.colourMaps.ColourMaps;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;

public class GraphvizDirectlyFollowsPrecedesGraph {

	private static final int multiplier = 1000;
	private static final int basePenWidth = 5;

	
	@Plugin(name = "Graphviz directly-follows-precedes graph visualisation", returnLabels = { "Dot visualization" }, returnTypes = { JComponent.class }, parameterLabels = { "Process Tree" }, userAccessible = true)
	@Visualizer
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Display directly-follows-precedes graph", requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, Dfpg dfpg) {
		/*if (dfpg.getDirectlyFollowsGraph().getVertices().length > 50) {
			return new JPanel();
		}*/
		return new DotPanel(dfg2Dot(dfpg, true));
	}

	public static long[] getMaxCounts(Dfpg dfpg){
		MultiSet<XEventClass> activityCounts = dfpg.getActivitiesCounts();
		long[] highest = new long[]{0,0,0,0};
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {
			XEventClass from = dfpg.getDirectlyFollowsGraph().getEdgeSource(edge);
			double weight = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge) * multiplier;
			weight /= activityCounts.getCardinalityOf(from);
			long weightLong = (long) weight;
			if(weightLong>highest[0])
				highest[0] = weightLong;
		}
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {
			XEventClass from = dfpg.getDirectlyFollowsGraph().getEdgeTarget(edge);
			double weight = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge) * multiplier;
			weight /= activityCounts.getCardinalityOf(from);
			long weightLong = (long) weight;
			if(weightLong>highest[1])
				highest[1] = weightLong;
		}
		return highest;
	}
	
	public static Dot dfg2Dot(Dfpg dfpg, boolean includeParalelEdges) {
		Dot dot = new Dot();
		
		long[] highest = getMaxCounts(dfpg);

		//prepare the nodes
		MultiSet<XEventClass> activityCounts = dfpg.getActivitiesCounts();
		HashMap<XEventClass, DotNode> activityToNode = new HashMap<XEventClass, DotNode>();
		for (XEventClass activity : dfpg.getDirectlyFollowsGraph().getVertices()) {
			DotNode node = dot.addNode(activity.toString());
			activityToNode.put(activity, node);

			node.setOption("shape", "box");

			//determine node colour using start and end activities
			if (dfpg.getStartActivities().contains(activity) && dfpg.getEndActivities().contains(activity)) {
				node.setOption("style", "filled");
				node.setOption(
						"fillcolor",
						ColourMap.toHexString(ColourMaps.colourMapGreen(
								(dfpg.getStartActivities().getCardinalityOf(activity)*multiplier)/activityCounts.getCardinalityOf(activity), multiplier))
								+ ":"
								+ ColourMap.toHexString(ColourMaps.colourMapRed((dfpg.getEndActivities()
										.getCardinalityOf(activity)*multiplier)/activityCounts.getCardinalityOf(activity), multiplier)));
			} else if (dfpg.getStartActivities().contains(activity)) {
				node.setOption("style", "filled");
				node.setOption(
						"fillcolor",
						ColourMap.toHexString(ColourMaps.colourMapGreen(
								(dfpg.getStartActivities().getCardinalityOf(activity)*multiplier)/activityCounts.getCardinalityOf(activity), multiplier))
								+ ":white");
			} else if (dfpg.getEndActivities().contains(activity)) {
				node.setOption("style", "filled");
				node.setOption(
						"fillcolor",
						"white:"
								+ ColourMap.toHexString(ColourMaps.colourMapRed((dfpg.getEndActivities()
										.getCardinalityOf(activity)*1000)/activityCounts.getCardinalityOf(activity), multiplier)));
			}
		}

		//add the directly-follows edges
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {
			XEventClass from = dfpg.getDirectlyFollowsGraph().getEdgeSource(edge);
			XEventClass to = dfpg.getDirectlyFollowsGraph().getEdgeTarget(edge);
			double weight = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge) * multiplier;
			weight /= activityCounts.getCardinalityOf(from);
			long weightLong = (long) weight;

			DotNode source = activityToNode.get(from);
			DotNode target = activityToNode.get(to);
			String label = String.valueOf(weightLong);
			
			if(weightLong==0)
				continue;
			DotEdge dotEdge = dot.addEdge(source, target, label);
			dotEdge.setOption("color", ColourMap.toHexString(ColourMaps.colourMapBlue(weightLong, highest[0])));
			dotEdge.setOption("penwidth", ""+Math.pow(weight/highest[0],2)*basePenWidth);
		}
		
		//add the directly-precedes edges
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {
			XEventClass from = dfpg.getDirectlyFollowsGraph().getEdgeTarget(edge);
			XEventClass to = dfpg.getDirectlyFollowsGraph().getEdgeSource(edge);
			double weight = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge) * multiplier;
			weight /= activityCounts.getCardinalityOf(from);
			long weightLong = (long) weight;
			
			DotNode source = activityToNode.get(from);
			DotNode target = activityToNode.get(to);
			String label = String.valueOf(weightLong);
			
			if(weightLong==0)
				continue;
			DotEdge dotEdge = dot.addEdge(source, target, label);
			dotEdge.setOption("color", ColourMap.toHexString(ColourMaps.colourMapRed(weightLong, highest[1])));
			dotEdge.setOption("penwidth", ""+Math.pow(weight/highest[1],2)*basePenWidth);
		}

		return dot;
	}
}