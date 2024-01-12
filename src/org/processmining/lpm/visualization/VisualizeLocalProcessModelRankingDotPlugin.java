package org.processmining.lpm.visualization;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(name = "@0 Visualize Local Process Model Ranking (Dot)", returnLabels = { "Visualized Local Process Model Ranking (Dot)" }, returnTypes = { JComponent.class }, parameterLabels = { "Accepting Petri Net Array" }, userAccessible = true)
@Visualizer
public class VisualizeLocalProcessModelRankingDotPlugin {

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, LocalProcessModelRanking nets) {
		return visualizeStatic(context, nets);
	}
	
	public static JComponent visualizeStatic(PluginContext context, LocalProcessModelRanking nets) {
		VisualizeLocalProcessModelDotPlugin visualizer = new VisualizeLocalProcessModelDotPlugin();
		JTabbedPane tabbedPane = new JTabbedPane();
		for (int index = 0; index < nets.getSize(); index++) {
			String label = "LPM " + (index + 1);
			System.out.println(label+": ");
			tabbedPane.add(label, visualizer.runUI(context, nets.getNet(index)));
			System.out.println();
		}
		return tabbedPane;
	}
}
