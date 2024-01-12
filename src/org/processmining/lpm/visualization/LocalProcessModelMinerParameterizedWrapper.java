package org.processmining.lpm.visualization;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.ParameterizedLPMXLog;

@Plugin(name = "@0 Mine Local Process Models", returnLabels = { "Local Process Model Discovery" }, returnTypes = {
		JComponent.class }, parameterLabels = { "XLog" }, userAccessible = true)
@Visualizer
public class LocalProcessModelMinerParameterizedWrapper {

	@Plugin(name = "Mine Local Process Models", returnLabels = { "Local Process Model Discovery" }, returnTypes = { JComponent.class }, parameterLabels = { "XLog" }, userAccessible = true)
	@Visualizer
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Mine Local Process Models", requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, ParameterizedLPMXLog log) {
		JComponent result = null;
		LocalProcessModelMiner miner = new LocalProcessModelMiner();
		result = miner.visualize(context, log.getLog(), log.getParams());
		return result;
	}
}