package org.processmining.lpm.visualization;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelRanking;

@Plugin(name = "@0 _Visualize Grouped Local Process Model Ranking (Ranking-Based)", returnLabels = { "@0 _Visualize Grouped Local Process Model Ranking (Ranking-Based)" }, returnTypes = { JComponent.class }, parameterLabels = { "Accepting Petri Net Array" }, userAccessible = true)
@Visualizer
public class VisualizeRankingBasedGroupedLocalProcessModelRankingDotPlugin implements ChangeListener, ActionListener{
	private LocalProcessModelRanking nets;
	private JTabbedPane groupsPane;
	private Map<Integer, List<Integer>> groupToLpmsMap;
	private Map<Integer, JTabbedPane> groupToLpmsPaneMap;
	private Map<Set<String>, Integer> alphabetToGroupMap;
	private Set<Integer> paintedGroups;
	private PluginContext context;
	
	private Timer timer = new Timer(2000, this);

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, LocalProcessModelRanking nets) {
		this.nets = nets;
		this.context = context;

		groupsPane = new JTabbedPane();
		
		groupToLpmsMap = new HashMap<Integer, List<Integer>>();
		groupToLpmsPaneMap = new HashMap<Integer, JTabbedPane>();
		alphabetToGroupMap = new HashMap<Set<String>, Integer>();
		paintedGroups = new HashSet<Integer>();
		
		int groupIndex = 0;
		for (int lpmIndex = 0; lpmIndex < nets.getSize(); lpmIndex++) {
			boolean lpmPlaced = false;
			LocalProcessModel lpm = nets.getNet(lpmIndex);
			Set<String> currentAlphabet = lpm.getAlphabet();
			for(Set<String> group : alphabetToGroupMap.keySet()){
				if(group.containsAll(currentAlphabet)){
					Integer groupIndex2 = alphabetToGroupMap.get(group);
					List<Integer> currentLpms = groupToLpmsMap.get(groupIndex2);
					currentLpms.add(lpmIndex);
					lpmPlaced = true;
				}
			}
			if(!lpmPlaced){
				alphabetToGroupMap.put(currentAlphabet, groupIndex);
				List<Integer> currentLpms = new ArrayList<Integer>();
				currentLpms.add(lpmIndex);
				groupToLpmsMap.put(groupIndex, currentLpms);
				JTabbedPane innerPane = new JTabbedPane();
				groupToLpmsPaneMap.put(groupIndex, innerPane);
				innerPane.setTabPlacement(JTabbedPane.BOTTOM);
				JLabel loadingScreen = new JLabel("LOADING...");
				innerPane.add("loading", loadingScreen);
				groupsPane.addTab("Group "+(groupIndex+1), innerPane);
				groupIndex++;
			}
		}
		if(groupIndex>0){
			paintGroup(0);
			paintedGroups.add(0);
		}
	    groupsPane.addChangeListener(this);
	    timer.start();
		return groupsPane;
	}
	
    public void stateChanged(ChangeEvent e) {
    	final Integer groupIndex = groupsPane.getSelectedIndex();
    	if(paintedGroups.contains(groupIndex)){
            System.out.println("Already Painted Tab: " + groupIndex);
    	}else{
    		SwingUtilities.invokeLater(new Runnable(){
				public void run() {
		            paintGroup(groupIndex);
				}
    		});
    	}
    }

	private JTabbedPane paintGroup(Integer groupIndex) {
		System.out.println("Painting Tab:        " + groupIndex);
		JTabbedPane innerPane = groupToLpmsPaneMap.get(groupIndex);
		
		List<Integer> lpmIndices = groupToLpmsMap.get(groupIndex);
		VisualizeLocalProcessModelDotPlugin visualizer = new VisualizeLocalProcessModelDotPlugin();
		innerPane.removeTabAt(0);

		for (int lpmIndex = 0; lpmIndex < nets.getSize(); lpmIndex++) {
			if(lpmIndices.contains(lpmIndex)){
				innerPane.addTab("LPM "+(lpmIndex+1), visualizer.runUI(context, nets.getNet(lpmIndex)));
			}
		}
		paintedGroups.add(groupIndex);
		return innerPane;
	}

	public void actionPerformed(ActionEvent ev){
		if(ev.getSource()==timer){
			Set<Integer> toDraw = groupToLpmsMap.keySet();
			toDraw.removeAll(paintedGroups);
			if(toDraw.size()>0){
				Integer[] toDrawAsArray = toDraw.toArray(new Integer[toDraw.size()]);
				final int nowDraw = toDrawAsArray[0];
	    		SwingUtilities.invokeLater(new Runnable(){
					public void run() {
			            paintGroup(nowDraw);
					}
	    		});
			}else{
				timer.stop();
			}
		}
	}
}
