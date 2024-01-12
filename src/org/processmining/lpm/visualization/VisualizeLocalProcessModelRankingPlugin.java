package org.processmining.lpm.visualization;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(name = "@0 Visualize Local Process Model Ranking", returnLabels = { "Visualized Local Process Model Ranking" }, returnTypes = { JComponent.class }, parameterLabels = { "Accepting Petri Net Array" }, userAccessible = true)
@Visualizer
public class VisualizeLocalProcessModelRankingPlugin {

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(UIPluginContext context, LocalProcessModelRanking nets) {
		VisualizeLocalProcessModelPlugin visualizer = new VisualizeLocalProcessModelPlugin();
		JTabbedPane tabbedPane = new JTabbedPane();
		for (int index = 0; index < nets.getSize(); index++) {
			String label = "LPM " + (index + 1);
			System.out.println(label+": ");
			tabbedPane.add(label, visualizer.visualize(context, nets.getNet(index)));
			System.out.println();
		}
		return tabbedPane;
	}
}
