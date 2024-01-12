package org.processmining.lpm.visualization;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.plugins.ProcessTreeArray;
import org.processmining.processtree.visualization.tree.TreeVisualization;

@Plugin(name = "@0 Visualize Process Tree Array", returnLabels = { "Visualized Process Tree Array" }, returnTypes = { JComponent.class }, parameterLabels = { "Process Tree Array" }, userAccessible = true)
@Visualizer
public class VisualizeProcessTreeArrayPlugin {

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(UIPluginContext context, ProcessTreeArray trees) {
		TreeVisualization visualizer = new TreeVisualization();
		JTabbedPane tabbedPane = new JTabbedPane();
		for (int index = 0; index < trees.getSize(); index++) {
			String label = "Tree " + (index + 1);
			System.out.println(label+": ");
			tabbedPane.add(label, visualizer.visualize(context, trees.getElement(index)));
			System.out.println();
		}
		return tabbedPane;
	}
}
