package org.processmining.lpm.visualization;

import java.awt.Color;
import java.awt.geom.Point2D;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.semantics.petrinet.Marking;

@Plugin(name = "@0 Visualize Local Process Model", returnLabels = { "Visualized Local Process Model" }, returnTypes = {
		JComponent.class }, parameterLabels = { "Local Process Model" }, userAccessible = true)
@Visualizer
public class VisualizeLocalProcessModelPlugin {

	@PluginVariant(requiredParameterLabels = { 0 })
	public static JComponent visualize(UIPluginContext context, LocalProcessModel net) {
		return visualize((PluginContext) context, net);
	}

	public static JComponent visualize(PluginContext context, LocalProcessModel lpm) {
		AcceptingPetriNet net = lpm.getAcceptingPetriNet();
		ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();
		for (Place place : net.getInitialMarking().baseSet()) {
			map.putViewSpecific(place, AttributeMap.FILLCOLOR, new Color(127, 0, 0));
		}
		for (Marking marking : net.getFinalMarkings()) {
			for (Place place : marking.baseSet()) {
				if (net.getInitialMarking().baseSet().contains(place)) {
					map.putViewSpecific(place, AttributeMap.FILLCOLOR, new Color(127, 0, 127));
				} else {
					map.putViewSpecific(place, AttributeMap.FILLCOLOR, new Color(0, 0, 127));
				}
			}
		}
		for(Transition t : net.getNet().getTransitions()){
			if(!t.isInvisible()){
				System.out.println(t+": "+lpm.getCount(t));
				String label = t.getLabel()+": "+System.lineSeparator()+lpm.getCount(t);
				map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel());
				if(lpm.getLogActivityCountMap()!=null)
					label+="/"+lpm.getLogActivityCountMap().get(t.getLabel());
				map.putViewSpecific(t, AttributeMap.LABEL, label);
				
				map.putViewSpecific(t, AttributeMap.EXTRALABELS, new String[]{""+lpm.getCount(t)});
				map.putViewSpecific(t, AttributeMap.EXTRALABELPOSITIONS, new Point2D[]{new Point2D.Double(10, 10)});
			}
		}
		JComponent component = new JPanel();
		component.setLayout(new BoxLayout(component, BoxLayout.Y_AXIS));
		component.add(ProMJGraphVisualizer.instance().visualizeGraph(context, net.getNet(), map));
		component.add(lpm.getScorePanel());
		return component;
	}
}
