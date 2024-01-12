package org.processmining.lpm.visualization;

import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.projection.LiftDfpg;
import org.processmining.plugins.graphviz.colourMaps.ColourMap;
import org.processmining.plugins.graphviz.colourMaps.ColourMaps;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;

public class GraphvizLiftDirectlyFollowsPrecedesGraph {

	private static final int basePenWidth = 5;

	
	@Plugin(name = "Graphviz lift directly-follows-precedes graph visualisation", returnLabels = { "Dot visualization" }, returnTypes = { JComponent.class }, parameterLabels = { "Process Tree" }, userAccessible = true)
	@Visualizer
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Display lift directly-follows-precedes graph", requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, LiftDfpg dfpg) {
		if (dfpg.getDirectlyFollowsGraph().getVertices().length > 50) {
			return new JPanel();
		}

		return new DotPanel(dfg2Dot(dfpg, true));
	}

	
	public static long[] getMaxCounts(LiftDfpg dfpg){
		long[] highest = new long[]{0,0,0,0};
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {
			double weight = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge);
			long weightLong = (long) weight;
			if(weightLong>highest[0])
				highest[0] = weightLong;
		}
		return highest;
	}
	
	public static Dot dfg2Dot(LiftDfpg dfpg, boolean includeParalelEdges) {
		Dot dot = new Dot();
		long[] highest = getMaxCounts(dfpg);
		//prepare the nodes
		HashMap<XEventClass, DotNode> activityToNode = new HashMap<XEventClass, DotNode>();
		for (XEventClass activity : dfpg.getDirectlyFollowsGraph().getVertices()) {
			DotNode node = dot.addNode(activity.toString());
			activityToNode.put(activity, node);

			node.setOption("shape", "box");
		}

		//add the directly-follows edges
		for (long edge : dfpg.getDirectlyFollowsGraph().getEdges()) {
			XEventClass from = dfpg.getDirectlyFollowsGraph().getEdgeSource(edge);
			XEventClass to = dfpg.getDirectlyFollowsGraph().getEdgeTarget(edge);
			double weight = (int) dfpg.getDirectlyFollowsGraph().getEdgeWeight(edge);
			long weightLong = (long) weight;

			DotNode source = activityToNode.get(from);
			DotNode target = activityToNode.get(to);
			String label = String.valueOf(weightLong);
			
			if(weightLong==0)
				continue;
			DotEdge dotEdge = dot.addEdge(source, target, label);
			dotEdge.setOption("color", ColourMap.toHexString(ColourMaps.colourMapBlue(weightLong, highest[0])));
			dotEdge.setOption("penwidth", ""+Math.pow(weight/highest[0],1)*basePenWidth);
		}

		return dot;
	}
}