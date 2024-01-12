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

public class GraphvizL2Graph {

	private static final int multiplier = 1000;
	private static final int basePenWidth = 5;

	
	@Plugin(name = "Graphviz L2 graph visualisation", returnLabels = { "Dot visualization" }, returnTypes = { JComponent.class }, parameterLabels = { "Process Tree" }, userAccessible = true)
	@Visualizer
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Display L2 graph", requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, Dfpg dfpg) {
		/*if (dfpg.getDirectlyFollowsGraph().getVertices().length > 50) {
			return new JPanel();
		}*/
		return new DotPanel(dfg2Dot(dfpg, true));
	}

	public static long getMaxCounts(Dfpg dfpg){
		MultiSet<XEventClass> activityCounts = dfpg.getActivitiesCounts();
		long highest = 0;
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {			
			XEventClass from = dfpg.getDirectlyFollowsGraph().getEdgeSource(edge);
			XEventClass to = dfpg.getDirectlyFollowsGraph().getEdgeTarget(edge);
			double weight1 = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge) * multiplier;
			double weight2 = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(to,from) * multiplier;
			double weight = Math.pow(weight1, 2) + Math.pow(weight2, 2);
			weight /= Math.pow(activityCounts.getCardinalityOf(from),2) + Math.pow(activityCounts.getCardinalityOf(to),2);
			long weightLong = (long) weight;
			if(weightLong>highest)
				highest = weightLong;
		}
		return highest;
	}
	
	public static Dot dfg2Dot(Dfpg dfpg, boolean includeParalelEdges) {
		Dot dot = new Dot();
		
		long highest = getMaxCounts(dfpg);

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
		
		//add the l2 edges
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {
			XEventClass from = dfpg.getDirectlyFollowsGraph().getEdgeSource(edge);
			XEventClass to = dfpg.getDirectlyFollowsGraph().getEdgeTarget(edge);
			double weight1 = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge) * multiplier;
			double weight2 = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(to, from) * multiplier;
			double weight = Math.pow(weight1, 2) + Math.pow(weight2, 2);
			weight /= Math.pow(activityCounts.getCardinalityOf(from),2) +  Math.pow(activityCounts.getCardinalityOf(to),2);
			long weightLong = (long) weight;

			DotNode source = activityToNode.get(from);
			DotNode target = activityToNode.get(to);
			String label = String.valueOf(weightLong);
			
			if(weightLong==0)
				continue;
			DotEdge dotEdge = dot.addEdge(source, target, label);
			System.out.println("weightLong: "+weightLong);
			System.out.println("highest:    "+highest);
			dotEdge.setOption("color", ColourMap.toHexString(ColourMaps.colourMapBlue(weightLong, highest)));
			dotEdge.setOption("penwidth", ""+(weight/highest)*basePenWidth);
		}
		
		return dot;
	}
}