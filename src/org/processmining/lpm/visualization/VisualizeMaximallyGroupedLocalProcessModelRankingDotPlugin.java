package org.processmining.lpm.visualization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(name = "@0 Visualize Grouped Local Process Model Ranking (Maximally)", returnLabels = { "@0 Visualize Grouped Local Process Model Ranking (Maximally)" }, returnTypes = { JComponent.class }, parameterLabels = { "Accepting Petri Net Array" }, userAccessible = true)
@Visualizer
public class VisualizeMaximallyGroupedLocalProcessModelRankingDotPlugin {

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, LocalProcessModelRanking nets) {
		return visualizeStatic(context, nets, false);
	}
	
	public static JComponent visualizeStatic(PluginContext context, LocalProcessModelRanking nets, boolean verbose) {
		VisualizeLocalProcessModelDotPlugin visualizer = new VisualizeLocalProcessModelDotPlugin();
		Set<Set<String>> maximalAlphabets = new HashSet<Set<String>>();
		for (int index = 0; index < nets.getSize(); index++) {
			LocalProcessModel lpm = nets.getNet(index);
			Set<String> currentAlphabet = lpm.getAlphabet();
			Set<Set<String>> removeFromMaximalAlphabets = new HashSet<Set<String>>();
			boolean superAlphabetFound = false;
			for(Set<String> existingAlphabet : maximalAlphabets){
				if(currentAlphabet.containsAll(existingAlphabet)){
					removeFromMaximalAlphabets.add(existingAlphabet);
				}else{
					if(existingAlphabet.containsAll(currentAlphabet))
						superAlphabetFound = true;
				}
			}
			maximalAlphabets.removeAll(removeFromMaximalAlphabets);
			if(!superAlphabetFound)
				maximalAlphabets.add(currentAlphabet);
		}

		JTabbedPane groupsPane = new JTabbedPane();
		HashMap<Set<String>, JTabbedPane> paneMap = new HashMap<Set<String>, JTabbedPane>();
		int i = 1;
		for (int index = 0; index < nets.getSize(); index++) {
			LocalProcessModel lpm = nets.getNet(index);
			Set<String> currentAlphabet = lpm.getAlphabet();
			
			for(Set<String> group : maximalAlphabets){
				if(group.containsAll(currentAlphabet)){
					JTabbedPane innerPane = paneMap.get(group);
					if(innerPane==null){
						innerPane = new JTabbedPane();
						groupsPane.add("Group "+i, innerPane);
						i++;
					}
					innerPane.setTabPlacement(JTabbedPane.BOTTOM);

				
					String label = "LPM " + (index + 1);
					if(verbose){
						System.out.println(label+": ");
						System.out.println();
					}
					innerPane.add(label, visualizer.runUI(context, nets.getNet(index)));
					paneMap.put(group, innerPane);
				}
			}
		}
		
		return groupsPane;
	}
}
