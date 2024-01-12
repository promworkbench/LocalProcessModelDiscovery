package org.processmining.lpm.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.lpm.dialogs.LocalProcessModelParameters.ProjectionMethods;
import org.processmining.lpm.dialogs.NiceGroupedDoubleSlider;
import org.processmining.lpm.dialogs.NiceGroupedDoubleSliderImpl;
import org.processmining.lpm.dialogs.NiceGroupedIntegerSlider;
import org.processmining.lpm.dialogs.NiceGroupedIntegerSliderImpl;
import org.processmining.lpm.dialogs.NiceGroupedSlider;
import org.processmining.lpm.dialogs.NiceSliderGroup;
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters;
import org.processmining.lpm.discovery.HighUtilityLocalProcessModelDiscovery;
import org.processmining.lpm.discovery.LocalProcessModelDiscovery;
import org.processmining.lpm.efficientlog.extractors.EventGapExtractor;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.UtilityLocalProcessModel;
import org.processmining.lpm.util.UtilityLocalProcessModelRanking;
import org.processmining.lpm.util.UtilityLocalProcessModelRankingFactory;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.utils.ProvidedObjectHelper;

import com.fluxicon.slickerbox.colors.SlickerColors;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

@Plugin(name = "@9 Mine Local Process Models", returnLabels = { "Local Process Model Discovery" }, returnTypes = {
		JComponent.class }, parameterLabels = { "XLog" }, userAccessible = true, level = PluginLevel.PeerReviewed)
@Visualizer
public class LocalProcessModelMiner implements ChangeListener, ActionListener, Observer {

	protected XLog log;
	protected LocalProcessModelRanking nets;
	protected JTabbedPane resultsTabbedPanel;
	protected Map<Integer, List<Integer>> groupToLpmsMap;
	protected Map<Integer, JTabbedPane> groupToLpmsPaneMap;
	protected Map<Set<String>, Integer> alphabetToGroupMap;
	protected Vector<Integer> groupsToPaint;
	protected PluginContext context;
	
	protected LocalProcessModelParameters params;
	
	protected JCheckBox duplicateTransitionChkBx;
	protected JCheckBox seqChkBx;
	protected JCheckBox andChkBx;
	protected JCheckBox orChkBx;
	protected JCheckBox xorChkBx;
	protected JCheckBox xorloopChkBx;
	
	protected NiceGroupedIntegerSlider numTransitionsSlider;
	protected NiceGroupedIntegerSlider top_kSlider;
	
	protected NiceGroupedIntegerSlider frequencyMinimumSlider;
	protected NiceGroupedDoubleSlider minimumDeterminismSlider;
	
	protected JComboBox<ProjectionMethods> projectionComboBox;
	
	protected NiceGroupedDoubleSlider wSupportSlider;
	protected NiceGroupedDoubleSlider wLanguageFitSlider;
	protected NiceGroupedDoubleSlider wConfidenceSlider;
	protected NiceGroupedDoubleSlider wCoverageSlider;
	protected NiceGroupedDoubleSlider wDeterminismSlider;
	protected NiceGroupedDoubleSlider wAvgNumFiringsSlider;
	
	protected JPanel resultsContainerPanel;
	
	protected JButton startMiningButton;
	protected JButton exportPetrinetsButton;
	protected JButton exportLPMRButton;
	
	protected JComboBox<GroupingMethods> groupingComboBox;
	
	protected boolean manualChange = true;
	
	protected Timer timer = new Timer(2000, this);
	
	protected Timer timer2 = new Timer(500, this);
	protected boolean suppressUpdates = false;
	protected NiceGroupedIntegerSlider numActivitiesFilterSlider;
	protected JLabel resultsMessageLabel;

	protected JCheckBox useAdjacencyConstraintChkBox;
	protected NiceGroupedIntegerSlider localAdjacencyConstraintSlider;
	protected NiceGroupedIntegerSlider globalAdjacencyConstraintSlider;

	protected JCheckBox useTimeConstraintChkBox;
	protected NiceGroupedIntegerSlider localTimeConstraintSlider;
	protected JComboBox<TimeUnits> localTimeUnitComboBox;
	protected NiceGroupedIntegerSlider globalTimeConstraintSlider;
	protected JComboBox<TimeUnits> globalTimeUnitComboBox;


	protected String[] overlayOptions;
	protected JComboBox<String> overlayComboBox;
	protected VisualizeLocalProcessModelDotPlugin visualizer;

	public JComponent visualize(PluginContext context, XLog log, LocalProcessModelParameters params){
		this.params = params;
		JComponent c = visualize(context, log);
		return c;
	}
	
	@Plugin(name = "Mine Local Process Models", returnLabels = { "Local Process Model Discovery" }, returnTypes = { JComponent.class }, parameterLabels = { "XLog" }, userAccessible = true)
	@Visualizer
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Mine Local Process Models", requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, XLog log) {
		JPanel containerPanel = new JPanel();
		containerPanel.setLayout(new BorderLayout());

		this.log = log;
		
		if(params == null){
			params = new LocalProcessModelParameters();
			params.setDiscoveryLog(log);
			params.setEvaluationLog(log);
			params.setSmartParameterDefaultsForLog(log);
		}
		this.context = context;

		JComponent parametersPanel = generateParameterPanel(context);
		containerPanel.add(parametersPanel, BorderLayout.WEST);
		
		JComponent resultsPanel = generateResultsPanel(context);
		containerPanel.add(resultsPanel, BorderLayout.CENTER);
		
		JComponent navigationPanel = generateNavigationPanel(context);
		containerPanel.add(navigationPanel, BorderLayout.EAST);
		
		this.visualizer = new VisualizeLocalProcessModelDotPlugin(log);
		return containerPanel;
	}

	public enum TimeUnits {
	    MILLISECONDS ("Milliseconds"),
	    SECONDS ("Seconds"),
	    MINUTES ("Minutes"),
		HOURS ("Hours"),
		DAYS ("Days"),
		WEEKS ("Weeks"),
		MONTHS ("Months"),
		YEARS ("Years");

		protected final String name;       

	    private TimeUnits(String s) {
	        name = s;
	    }

	    public boolean equalsName(String otherName) {
	        return (otherName == null) ? false : name.equals(otherName);
	    }

	    public String toString() {
	       return this.name;
	    }
	    
	    public int getMaxValue(){
	    	switch(this){
	    		case MILLISECONDS : return 1000;
	    		case SECONDS : return 60;
	    		case MINUTES : return 60;
	    		case HOURS : return 24;
	    		case DAYS : return 7;
	    		case WEEKS : return 4;
	    		case MONTHS : return 12;
	    		case YEARS : return 1000;
				default :
					return 1;
	    	}
	    }
	}
	
	public enum GroupingMethods {
	    NONE ("None"),
	    RANKINGBASED ("Ranking-based"),
	    MAXIMAL ("Maximal");

		protected final String name;       

	    private GroupingMethods(String s) {
	        name = s;
	    }

	    public boolean equalsName(String otherName) {
	        return (otherName == null) ? false : name.equals(otherName);
	    }

	    public String toString() {
	       return this.name;
	    }
	}
	
	protected JComponent generateNavigationPanel(PluginContext context){
		JPanel navigationPanel = new JPanel();
		navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.PAGE_AXIS));
		navigationPanel.setBackground(SlickerColors.COLOR_BG_4);

		JLabel header = new JLabel("Results Navigation Parameters");
		header.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.setFont(new Font("Serif", Font.BOLD, 24));
		header.setForeground(Color.BLACK);
		navigationPanel.add(header);
		
		JPanel pane3 = SlickerFactory.instance().createTabbedPane("Grouping", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		groupingComboBox = SlickerFactory.instance().createComboBox(GroupingMethods.values());
		groupingComboBox.setSelectedItem(GroupingMethods.RANKINGBASED);			
		groupingComboBox.addActionListener(this);
		pane3.add(groupingComboBox);
		resizeSlickerTabbedPane(pane3);
		navigationPanel.add(pane3);
		
		JPanel pane5 = SlickerFactory.instance().createTabbedPane("Filters", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		pane5.setLayout(new BoxLayout(pane5, BoxLayout.PAGE_AXIS));
		NiceSliderGroup constraintsGroup = new NiceSliderGroup();
		numActivitiesFilterSlider = new NiceGroupedIntegerSliderImpl("Activities in LPM", 2, 5, 2, constraintsGroup, Orientation.HORIZONTAL);
		numActivitiesFilterSlider.addChangeListener(this);
		pane5.add(numActivitiesFilterSlider);
		resizeSlickerTabbedPane(pane5);
		navigationPanel.add(pane5);
		
		JPanel pane6 = SlickerFactory.instance().createTabbedPane("Overlay", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		pane6.setLayout(new BoxLayout(pane6, BoxLayout.PAGE_AXIS));
		Set<String> literalAttributes = new HashSet<String>();
		for(XTrace trace : log){
			for(XEvent event : trace){
				for(XAttribute attribute : event.getAttributes().values()){
					if(attribute instanceof XAttributeLiteral){
						literalAttributes.add(attribute.getKey());
					}
				}
			}
		}
		List<String> overlayOptions = new ArrayList<String>(literalAttributes);
		Collections.sort(overlayOptions);
		overlayOptions.add(0, "None");
		this.overlayOptions = overlayOptions.toArray(new String[overlayOptions.size()]);
		overlayComboBox = SlickerFactory.instance().createComboBox(this.overlayOptions);
		overlayComboBox.setSelectedItem("None");
		overlayComboBox.addActionListener(this);
		pane6.add(overlayComboBox);
		resizeSlickerTabbedPane(pane6);
		navigationPanel.add(pane6);
		
		NiceSliderGroup metricGroup = new NiceSliderGroup(true, 1d);
		JPanel pane4 = SlickerFactory.instance().createTabbedPane("Ranking", SlickerColors.COLOR_BG_4, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		pane4.setLayout(new BoxLayout(pane4, BoxLayout.PAGE_AXIS));
		wSupportSlider = new NiceGroupedDoubleSliderImpl("Support", 0, 1, params.getSupportWeight()/params.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wSupportSlider.addChangeListener(this);
		pane4.add(wSupportSlider);
		
		wLanguageFitSlider = new NiceGroupedDoubleSliderImpl("Language fit", 0, 1, params.getLanguageFitWeight()/params.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wLanguageFitSlider.addChangeListener(this);
		pane4.add(wLanguageFitSlider);
		
		wConfidenceSlider = new NiceGroupedDoubleSliderImpl("Confidence", 0, 1, params.getConfidenceWeight()/params.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wConfidenceSlider.addChangeListener(this);
		pane4.add(wConfidenceSlider);
		
		wCoverageSlider = new NiceGroupedDoubleSliderImpl("Coverage", 0, 1, params.getCoverageWeight()/params.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wCoverageSlider.addChangeListener(this);
		pane4.add(wCoverageSlider);
		
		wDeterminismSlider = new NiceGroupedDoubleSliderImpl("Determinism", 0, 1, params.getDeterminismWeight()/params.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wDeterminismSlider.addChangeListener(this);
		pane4.add(wDeterminismSlider);
		
		// TODO: deze hiden, of advanced option van maken
		wAvgNumFiringsSlider = new NiceGroupedDoubleSliderImpl("Avg. num. firings", 0, 1, params.getAvgNumFiringsWeight()/params.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wAvgNumFiringsSlider.addChangeListener(this);
		pane4.add(wAvgNumFiringsSlider);
		resizeSlickerTabbedPane(pane4);
		navigationPanel.add(pane4);
		
		exportPetrinetsButton = new JButton("Export Local Process Models as Petri nets");
		exportPetrinetsButton.addActionListener(this);
		navigationPanel.add(exportPetrinetsButton);
		
		exportLPMRButton = new JButton("Export results as Local Process Model Ranking");
		exportLPMRButton.addActionListener(this);
		navigationPanel.add(exportLPMRButton);

		return navigationPanel;
	}
	
	protected void refreshResultsPanel(PluginContext context){
		if(resultsTabbedPanel==null)
			resultsTabbedPanel = new JTabbedPane();
		else
			resultsTabbedPanel.removeAll();
		if(nets!=null){
			groupToLpmsMap = new ConcurrentHashMap<Integer, List<Integer>>();
			groupToLpmsPaneMap = new ConcurrentHashMap<Integer, JTabbedPane>();
			alphabetToGroupMap = new ConcurrentHashMap<Set<String>, Integer>();
			if(groupsToPaint!=null)
				groupsToPaint.clear();
			else
				groupsToPaint = new Vector<Integer>(groupToLpmsMap.keySet());
			if(groupingComboBox.getSelectedItem()==GroupingMethods.RANKINGBASED){
				int groupIndex = 0;
				
				while(nets.isRescoringOngoing()){
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				for (int lpmIndex = 0; lpmIndex < nets.getSize(); lpmIndex++) {
					boolean lpmPlaced = false;
					LocalProcessModel lpm = nets.getNet(lpmIndex);
					int num_visible = 0;
					for(Transition t : lpm.getAcceptingPetriNet().getNet().getTransitions())
						if(!t.isInvisible())
							num_visible++;
					if(num_visible<numActivitiesFilterSlider.getValue())
						continue;
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
						innerPane.addTab("loading", loadingScreen);
						resultsTabbedPanel.addTab("Group "+(groupIndex+1), innerPane);
						groupsToPaint.add(groupIndex);
						groupIndex++;
					}
				}
				if(groupIndex>0){
					paintGroup(0);
				}else{
					resultsMessageLabel.setText("No Local Process Models found that fulfill the pruning criteria and constraints");
					resultsMessageLabel.setForeground(Color.RED);
					//resultsContainerPanel.add(resultsMessageLabel, BorderLayout.SOUTH);
					resultsContainerPanel.updateUI();
				}
			}else if (groupingComboBox.getSelectedItem()==GroupingMethods.NONE){
				JTabbedPane innerPane = new JTabbedPane();
				List<Integer> allLpms = new ArrayList<Integer>();
				for (int lpmIndex = 0; lpmIndex < nets.getSize(); lpmIndex++)
					allLpms.add(lpmIndex);
				groupToLpmsMap.put(0, allLpms);				
				innerPane.setTabPlacement(JTabbedPane.BOTTOM);
				JLabel loadingScreen = new JLabel("LOADING...");
				innerPane.addTab("loading", loadingScreen);
				resultsTabbedPanel.addTab("All LPMs", innerPane);
				groupToLpmsPaneMap.put(0, innerPane);
				groupsToPaint.add(0);
			}else if (groupingComboBox.getSelectedItem()==GroupingMethods.MAXIMAL){
				List<Set<String>> maximalAlphabets = new ArrayList<Set<String>>();
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
						
				for (int lpmIndex = 0; lpmIndex < nets.getSize(); lpmIndex++) {
					LocalProcessModel lpm = nets.getNet(lpmIndex);
					Set<String> currentAlphabet = lpm.getAlphabet();
					int groupIndex = 0;
					for(Set<String> group : maximalAlphabets){
						if(group.containsAll(currentAlphabet)){				
							List<Integer> lpmsInGroup = groupToLpmsMap.get(groupIndex);
							if(lpmsInGroup==null)
								lpmsInGroup = new ArrayList<Integer>();
							lpmsInGroup.add(lpmIndex);
							groupToLpmsMap.put(groupIndex, lpmsInGroup);
						}
						groupIndex++;
					}
				}

				for(int groupIndex=0; groupIndex<maximalAlphabets.size(); groupIndex++){
					JTabbedPane innerPane = new JTabbedPane();
					innerPane.setTabPlacement(JTabbedPane.BOTTOM);
					JLabel loadingScreen = new JLabel("LOADING...");
					innerPane.addTab("loading", loadingScreen);
					resultsTabbedPanel.addTab("Group "+(groupIndex+1), innerPane);
					groupToLpmsPaneMap.put(groupIndex, innerPane);
					groupsToPaint.add(groupIndex);
				}
			}
		    resultsTabbedPanel.addChangeListener(this);
		    timer.start();
		}else{
			resultsMessageLabel.setText("Start Mining Local Process Models to get results (See the \"Mining Parameters\" panel)");
			resultsMessageLabel.setForeground(Color.RED);
			//resultsContainerPanel.add(resultsMessageLabel, BorderLayout.SOUTH);
		}
	}
	
	protected JComponent generateResultsPanel(PluginContext context) {
		resultsContainerPanel = new JPanel();
		resultsContainerPanel.setLayout(new BorderLayout());
		
		JLabel header = new JLabel("Mining Results");
		header.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.setHorizontalAlignment(SwingConstants.CENTER);
		header.setFont(new Font("Serif", Font.BOLD, 24));
		header.setForeground(Color.BLACK);
		resultsContainerPanel.add(header, BorderLayout.NORTH);
		
		resultsTabbedPanel = new JTabbedPane();
		resultsContainerPanel.add(resultsTabbedPanel, BorderLayout.CENTER);
		
		resultsMessageLabel = new JLabel();
		resultsContainerPanel.add(resultsMessageLabel, BorderLayout.SOUTH);
		
		refreshResultsPanel(context);
	    return resultsContainerPanel;
	}

	protected JComponent generateParameterPanel(PluginContext context) {
		JPanel parametersPanel = new JPanel();
		
		parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.PAGE_AXIS));
		parametersPanel.setBackground(SlickerColors.COLOR_BG_4);
		
		JLabel header = new JLabel("Mining Parameters");
		header.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.setFont(new Font("Serif", Font.BOLD, 24));
		header.setForeground(Color.BLACK);
		parametersPanel.add(header);
		
		JPanel pane0 = SlickerFactory.instance().createTabbedPane("General Settings", SlickerColors.COLOR_BG_4, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		NiceSliderGroup generalGroup = new NiceSliderGroup();
		pane0.setLayout(new BoxLayout(pane0, BoxLayout.PAGE_AXIS));
		numTransitionsSlider = new NiceGroupedIntegerSliderImpl("Maximum number of transitions in LPMs", 1, 5, 4, generalGroup, Orientation.HORIZONTAL);
		numTransitionsSlider.addChangeListener(this);
		pane0.add(numTransitionsSlider);
		top_kSlider = new NiceGroupedIntegerSliderImpl("Number of LPMs to discover", 1, 500, params.getTop_k(), generalGroup, Orientation.HORIZONTAL);
		top_kSlider.addChangeListener(this);
		pane0.add(top_kSlider);
		JPanel dupPanel = new JPanel(new BorderLayout());
		dupPanel.setBackground(SlickerColors.COLOR_BG_4);
		duplicateTransitionChkBx = SlickerFactory.instance().createCheckBox("Allow duplicate transitions", params.isDuplicateTransitions());
		duplicateTransitionChkBx.addActionListener(this);
		dupPanel.add(duplicateTransitionChkBx, BorderLayout.WEST);
		pane0.add(dupPanel);
		resizeSlickerTabbedPane(pane0);
		parametersPanel.add(pane0);
		
		JPanel pane1 = SlickerFactory.instance().createTabbedPane("Operators", SlickerColors.COLOR_BG_4, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
		
		JPanel seqPanel = new JPanel(new BorderLayout());
		seqPanel.setBackground(SlickerColors.COLOR_BG_4);
		seqChkBx = SlickerFactory.instance().createCheckBox("Use sequence operator", params.isUseSeq());
		seqChkBx.setAlignmentX(Component.LEFT_ALIGNMENT);
		seqChkBx.addActionListener(this);
		seqPanel.add(seqChkBx, BorderLayout.WEST);
		pane1.add(seqPanel);
		
		JPanel andPanel = new JPanel(new BorderLayout());
		andPanel.setBackground(SlickerColors.COLOR_BG_4);
		andChkBx = SlickerFactory.instance().createCheckBox("Use concurrency operator", params.isUseAnd());
		andChkBx.addActionListener(this);
		andPanel.add(andChkBx, BorderLayout.WEST);
		pane1.add(andPanel);
		
		JPanel orPanel = new JPanel(new BorderLayout());
		orPanel.setBackground(SlickerColors.COLOR_BG_4);
		orChkBx = SlickerFactory.instance().createCheckBox("Use inclusive choice operator", params.isUseOr());
		orChkBx.addActionListener(this);
		orPanel.add(orChkBx, BorderLayout.WEST);
		pane1.add(orPanel);
		
		JPanel xorPanel = new JPanel(new BorderLayout());
		xorPanel.setBackground(SlickerColors.COLOR_BG_4);
		xorChkBx = SlickerFactory.instance().createCheckBox("Use exclusve choice operator", params.isUseXor());
		xorChkBx.addActionListener(this);
		xorPanel.add(xorChkBx, BorderLayout.WEST);
		pane1.add(xorPanel);
		
		JPanel xorloopPanel = new JPanel(new BorderLayout());
		xorloopPanel.setBackground(SlickerColors.COLOR_BG_4);
		xorloopChkBx = SlickerFactory.instance().createCheckBox("Use loop operator", params.isUseXorloop());
		xorloopChkBx.addActionListener(this);
		xorloopPanel.add(xorloopChkBx, BorderLayout.WEST);
		pane1.add(xorloopPanel);
		resizeSlickerTabbedPane(pane1);
		parametersPanel.add(pane1);

		JPanel pane2 = SlickerFactory.instance().createTabbedPane("Pruning", SlickerColors.COLOR_BG_4, SlickerColors.COLOR_BG_4, SlickerColors.COLOR_FG);
		NiceSliderGroup pruningGroup = new NiceSliderGroup();
		pane2.setLayout(new BoxLayout(pane2, BoxLayout.PAGE_AXIS));
		int sliderInitial = params.getFrequencyMinimum()<2 ? 2 : params.getFrequencyMinimum();
		int sliderMaximum = params.getMaxActivityFrequencyInLog()<2 ? 2 : params.getMaxActivityFrequencyInLog();
		frequencyMinimumSlider = new NiceGroupedIntegerSliderImpl("Minimum number of occurrences in log", 1, sliderMaximum, sliderInitial, pruningGroup, Orientation.HORIZONTAL);
		frequencyMinimumSlider.addChangeListener(this);
		pane2.add(frequencyMinimumSlider);
		
		minimumDeterminismSlider = new NiceGroupedDoubleSliderImpl("Minimum determinism", 0, 1, 0.5, pruningGroup, Orientation.HORIZONTAL);
		minimumDeterminismSlider.addChangeListener(this);
		pane2.add(minimumDeterminismSlider);
		resizeSlickerTabbedPane(pane2);
		parametersPanel.add(pane2);
		
		JPanel pane6 = SlickerFactory.instance().createTabbedPane("Constraints", SlickerColors.COLOR_BG_4, SlickerColors.COLOR_BG_4, SlickerColors.COLOR_FG);
		pane6.setLayout(new BoxLayout(pane6, BoxLayout.PAGE_AXIS));
		useAdjacencyConstraintChkBox = SlickerFactory.instance().createCheckBox("Event gaps", params.isUseEfficientLog());
		useAdjacencyConstraintChkBox.addActionListener(this);
		NiceSliderGroup gapConstraintsGroup = new NiceSliderGroup();
		localAdjacencyConstraintSlider = new NiceGroupedIntegerSliderImpl("Maximum number of adjacent non-fitting event", 0, 100, params.getMax_consecutive_nonfitting(), gapConstraintsGroup, Orientation.HORIZONTAL);
		localAdjacencyConstraintSlider.addChangeListener(this);
		//globalAdjacencyConstraintSlider = new NiceGroupedIntegerSliderImpl("Maximum number of non-fitting events per instance", 0, 100, params.getMax_total_nonfitting(), gapConstraintsGroup, Orientation.HORIZONTAL);
		//globalAdjacencyConstraintSlider.addChangeListener(this);

		pane6.add(useAdjacencyConstraintChkBox);
		pane6.add(localAdjacencyConstraintSlider);
		//pane6.add(globalAdjacencyConstraintSlider);
		if(params.logHasTime()){
			useTimeConstraintChkBox = SlickerFactory.instance().createCheckBox("Time gaps", params.isUseEfficientLog());
			useTimeConstraintChkBox.addActionListener(this);
		
			NiceSliderGroup timeConstraintsGroup = new NiceSliderGroup();
			
			TimeUnits startingTimeUnit = getTimeUnit(params.getMax_consecutive_timedif_millis());
			int valueInTimeUnit = millisToTimeUnitValue(params.getMax_consecutive_timedif_millis(), startingTimeUnit);
			localTimeConstraintSlider = new NiceGroupedIntegerSliderImpl("Maximum time gap between adjacent fitting event", 0, startingTimeUnit.getMaxValue(), valueInTimeUnit, timeConstraintsGroup, Orientation.HORIZONTAL);;
			localTimeConstraintSlider.addChangeListener(this);
			
			localTimeUnitComboBox = SlickerFactory.instance().createComboBox(TimeUnits.values());
			localTimeUnitComboBox.setSelectedItem(startingTimeUnit);
			localTimeUnitComboBox.addActionListener(this);
			/*
			TimeUnits globalStartingTimeUnit = getTimeUnit(params.getMax_total_timedif_millis());
			int globalValueInTimeUnit = millisToTimeUnitValue(params.getMax_total_timedif_millis(), globalStartingTimeUnit);
			globalTimeConstraintSlider = new NiceGroupedIntegerSliderImpl("Maximum time duration of instance", 0, globalStartingTimeUnit.getMaxValue(), globalValueInTimeUnit, timeConstraintsGroup, Orientation.HORIZONTAL);;
			globalTimeConstraintSlider.addChangeListener(this);
			globalTimeUnitComboBox = SlickerFactory.instance().createComboBox(TimeUnits.values());
			globalTimeUnitComboBox.setSelectedItem(globalStartingTimeUnit);			
			globalTimeUnitComboBox.addActionListener(this);
			*/
			if(!params.isUseEfficientLog()){
				localAdjacencyConstraintSlider.setEnabled(false);
				//globalAdjacencyConstraintSlider.setEnabled(false);
				localTimeConstraintSlider.setEnabled(false);
				localTimeUnitComboBox.setEnabled(false);
				//globalTimeConstraintSlider.setEnabled(false);
				//globalTimeUnitComboBox.setEnabled(false);
			}
			pane6.add(useTimeConstraintChkBox);
			pane6.add(localTimeConstraintSlider);
			pane6.add(localTimeUnitComboBox);
			//pane6.add(globalTimeConstraintSlider);
			//pane6.add(globalTimeUnitComboBox);
		}else{
			params.setEfficientLogExtractor(new EventGapExtractor(params.getMax_consecutive_nonfitting(), params.getMax_total_timedif_millis()));
		}
		resizeSlickerTabbedPane(pane6);
		parametersPanel.add(pane6);
		
		JPanel pane3 = SlickerFactory.instance().createTabbedPane("Projections", SlickerColors.COLOR_BG_4, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		projectionComboBox = SlickerFactory.instance().createComboBox(ProjectionMethods.values());
		projectionComboBox.setSelectedItem(params.getProjectionMethod());			
		projectionComboBox.addActionListener(this);
		pane3.add(projectionComboBox);
		resizeSlickerTabbedPane(pane3);
		parametersPanel.add(pane3);		
		
		JPanel pane5 = SlickerFactory.instance().createTabbedPane("MINING", SlickerColors.COLOR_BG_4, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		startMiningButton = SlickerFactory.instance().createButton("Start Mining LPMs");
		startMiningButton.addActionListener(this);
		pane5.add(startMiningButton);
		resizeSlickerTabbedPane(pane5);
		parametersPanel.add(pane5);
		
		return parametersPanel;
	}

	public long timeUnitValuetoMillis(int timeUnitValue, TimeUnits startingTimeUnit) {
		switch(startingTimeUnit){
			case MILLISECONDS : return ((long) timeUnitValue);
			case SECONDS : return ((long) timeUnitValue)*1000;
			case MINUTES : return ((long) timeUnitValue)*(1000*60);
			case HOURS : return ((long) timeUnitValue)*(1000*60*60);
			case DAYS : return ((long) timeUnitValue)*(1000*60*60*24);
			case WEEKS : return ((long) timeUnitValue)*(1000*60*60*24*7);
			case MONTHS : return ((long) timeUnitValue)*(1000*60*60*24*7*4);
			case YEARS : return ((long) timeUnitValue)*(1000*60*60*24*365);
		}
		return 0;
	}
	
	public int millisToTimeUnitValue(long timeInMillis, TimeUnits startingTimeUnit) {
		switch(startingTimeUnit){
			case MILLISECONDS : return (int) timeInMillis;
			case SECONDS : return (int) timeInMillis/1000;
			case MINUTES : return (int) timeInMillis/(1000*60);
			case HOURS : return (int) timeInMillis/(1000*60*60);
			case DAYS : return (int) timeInMillis/(1000*60*60*24);
			case WEEKS : return (int) timeInMillis/(1000*60*60*24*7);
			case MONTHS : return (int) timeInMillis/(1000*60*60*24*7*4);
			case YEARS : return (int) timeInMillis/(1000*60*60*24*365);
		}
		return 0;
	}

	public TimeUnits getTimeUnit(long time_in_millis) {
		if(time_in_millis<=1000){
			return TimeUnits.MILLISECONDS;
		}else if (time_in_millis<=1000*60){
			return TimeUnits.SECONDS;
		}else if (time_in_millis<=1000*60*60){
			return TimeUnits.MINUTES;
		}else if (time_in_millis<=1000*60*60*24){
			return TimeUnits.HOURS;
		}else if (time_in_millis<=1000*60*60*24*7){
			return TimeUnits.DAYS;
		}else if (time_in_millis<=1000*60*60*24*7*4){
			return TimeUnits.WEEKS;
		}else if (time_in_millis<=1000*60*60*24*365){
			return TimeUnits.MONTHS;
		}else{
			return TimeUnits.YEARS;
		}
	}

	protected void resizeSlickerTabbedPane(JPanel pane6) {
		int width = 0;
		int height = 0;
		for(Component c : pane6.getComponents()){
			int thisWidth = (int) Math.ceil(c.getSize().getWidth());
			height+=c.getPreferredSize().getHeight();
			if(c instanceof NiceGroupedSlider)
				thisWidth+=1000;
			width = Math.min(width, thisWidth);
		}
		pane6.setMinimumSize(new Dimension(width, 0));
		pane6.setPreferredSize(new Dimension(width+500, height));
	}
	
	protected JTabbedPane paintGroup(Integer groupIndex) {
		System.out.println("Painting Tab:        " + groupIndex);
		JTabbedPane innerPane = groupToLpmsPaneMap.get(groupIndex);
		
		List<Integer> lpmIndices = groupToLpmsMap.get(groupIndex);
		
		innerPane.removeTabAt(0);
		for (int lpmIndex = 0; lpmIndex < nets.getSize(); lpmIndex++) {
			if(lpmIndices.contains(lpmIndex)){
				if(overlayComboBox.getSelectedItem().equals("None")){
					innerPane.addTab("LPM "+(lpmIndex+1), visualizer.runUI(context, nets.getNet(lpmIndex)));
				}else{
					innerPane.addTab("LPM "+(lpmIndex+1), visualizer.runUI(context, nets.getNet(lpmIndex), overlayComboBox.getSelectedItem().toString()));
				}
			}
		}
		groupsToPaint.remove(groupIndex);
		return innerPane;
	}

	
    public void stateChanged(ChangeEvent e) {
		Container trigger = ((JComponent)e.getSource()).getParent();
    	if(trigger == resultsTabbedPanel){
        	final Integer groupIndex = resultsTabbedPanel.getSelectedIndex();
        	if(!groupsToPaint.contains(groupIndex)){
                System.out.println("Already Painted Tab: " + groupIndex);
        	}else{
        		SwingUtilities.invokeLater(new Runnable(){
    				public void run() {
    		            paintGroup(groupIndex);
    				}
        		});
        	}
    		resultsContainerPanel.invalidate();
    	}
    	
    	else if(trigger==numActivitiesFilterSlider){
    		if(!numActivitiesFilterSlider.getSlider().getValueIsAdjusting()){
        		SwingUtilities.invokeLater(new Runnable(){
					public void run() {
						refreshResultsPanel(context);
					}
	    		});
    		}
    	}else if(trigger == numTransitionsSlider)
			params.setNumTransitions(numTransitionsSlider.getValue());
    	else if(trigger == top_kSlider)
			params.setTop_k(top_kSlider.getValue());
    	else if(trigger == frequencyMinimumSlider)
			params.setFrequencyMinimum(frequencyMinimumSlider.getValue());
    	else if(trigger == minimumDeterminismSlider)
			params.setDeterminismMinimum(minimumDeterminismSlider.getValue());
    	else if(trigger==wSupportSlider||trigger==wLanguageFitSlider||trigger==wConfidenceSlider||trigger==wCoverageSlider||trigger==wDeterminismSlider||trigger==wAvgNumFiringsSlider){
        	if(trigger == wSupportSlider)
    			params.setSupportWeight(wSupportSlider.getValue());
        	else if(trigger == wLanguageFitSlider)
    			params.setLanguageFitWeight(wLanguageFitSlider.getValue());
        	else if(trigger == wConfidenceSlider)
    			params.setConfidenceWeight(wConfidenceSlider.getValue());
        	else if(trigger == wCoverageSlider)
    			params.setCoverageWeight(wCoverageSlider.getValue());
        	else if(trigger == wDeterminismSlider)
    			params.setDeterminismWeight(wDeterminismSlider.getValue());
        	else if(trigger == wAvgNumFiringsSlider)
    			params.setAvgNumFiringsWeight(wAvgNumFiringsSlider.getValue());
        	if(!wSupportSlider.getSlider().getValueIsAdjusting() && !wLanguageFitSlider.getSlider().getValueIsAdjusting() && !wConfidenceSlider.getSlider().getValueIsAdjusting() && !wCoverageSlider.getSlider().getValueIsAdjusting() && !wDeterminismSlider.getSlider().getValueIsAdjusting() && !wAvgNumFiringsSlider.getSlider().getValueIsAdjusting()){ 
        		if(!suppressUpdates){
        			nets.rescoreWithNewWeights(params);
        			suppressUpdates = true;
        			timer2.start();
	        		SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							refreshResultsPanel(context);
						}
		    		});
        		}
        	}
    	}
    	else if(trigger==localAdjacencyConstraintSlider){
    		if(!localAdjacencyConstraintSlider.getSlider().getValueIsAdjusting())
    			params.setMax_consecutive_nonfitting(localAdjacencyConstraintSlider.getValue());
    	}
    	else if(trigger==globalAdjacencyConstraintSlider){
    		if(!globalAdjacencyConstraintSlider.getSlider().getValueIsAdjusting())
    			params.setMax_total_nonfitting(globalAdjacencyConstraintSlider.getValue());
    	}
    	else if(trigger==localTimeConstraintSlider){
    		if(!localTimeConstraintSlider.getSlider().getValueIsAdjusting()){
        		TimeUnits timeUnit = (TimeUnits) localTimeUnitComboBox.getSelectedItem();
    			params.setMax_consecutive_timedif_millis(timeUnitValuetoMillis(localTimeConstraintSlider.getValue(), timeUnit));
    		}
    	}
    	else if(trigger==globalTimeConstraintSlider){
    		if(!globalTimeConstraintSlider.getSlider().getValueIsAdjusting()){
        		TimeUnits timeUnit = (TimeUnits) globalTimeUnitComboBox.getSelectedItem();
    			params.setMax_total_timedif_millis(timeUnitValuetoMillis(globalTimeConstraintSlider.getValue(), timeUnit));
    		}
    	}
    }

	public void update(Observable arg0, Object arg1) {
		if(arg1 instanceof LocalProcessModelParameters){
			((LocalProcessModelParameters) arg1).addObserver(this);
		}else if (arg1 instanceof Integer){
			resultsMessageLabel.setText(arg1+" Local Process Models explored");
			resultsMessageLabel.setForeground(Color.BLUE);
			resultsContainerPanel.updateUI();
		}
	}
    
	public void actionPerformed(ActionEvent e){
		if(e.getSource()==startMiningButton){
			startMiningButton.setEnabled(false);
			params.setNumberOfExploredLpms(new AtomicInteger(0));
			LocalProcessModelDiscovery lpmd_temp = null;
			if(params instanceof UtilityLocalProcessModelParameters){
				lpmd_temp = new HighUtilityLocalProcessModelDiscovery();
			}else{
				lpmd_temp = new LocalProcessModelDiscovery();
			}
			final LocalProcessModelDiscovery lpmd = lpmd_temp;
			params.addObserver(this);
			
			System.out.println("start time: "+System.currentTimeMillis());
			SwingWorker<LocalProcessModelRanking, Void> lpmThread = new SwingWorker<LocalProcessModelRanking, Void>() {
				@Override
				protected LocalProcessModelRanking doInBackground() throws Exception {
					return lpmd.runHeadless(context, params);
				}
				
			    @Override
			    protected void done() {
			    	LocalProcessModelRanking lpmr = null;
			        try {
			            lpmr = get();
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			        nets = lpmr;
					refreshResultsPanel(context);
					resultsMessageLabel.setText("Finished mining Local Process Models!");
					System.out.println("end time: "+System.currentTimeMillis());
					resultsMessageLabel.setForeground(new Color(51,115,9));
					startMiningButton.setEnabled(true);
			    }
			};
			lpmThread.execute();
		} 
		else if(e.getSource()==timer){
			if(groupsToPaint.size()>0){
				Integer[] toDrawAsArray = groupsToPaint.toArray(new Integer[groupsToPaint.size()]);
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
		else if(e.getSource()==timer2){
			suppressUpdates = false;
			timer2.stop();
		}
		else if(e.getSource()==duplicateTransitionChkBx)
			params.setDuplicateTransitions(duplicateTransitionChkBx.isSelected());
		else if(e.getSource()==seqChkBx)
			params.setUseSeq(seqChkBx.isSelected());
		else if(e.getSource()==andChkBx)
			params.setUseAnd(andChkBx.isSelected());
		else if(e.getSource()==orChkBx)
			params.setUseOr(orChkBx.isSelected());
		else if(e.getSource()==xorChkBx)
			params.setUseXor(xorChkBx.isSelected());
		else if(e.getSource()==xorloopChkBx)
			params.setUseXorloop(xorloopChkBx.isSelected());
		else if(e.getSource()==projectionComboBox)
			params.setProjectionMethod((ProjectionMethods) projectionComboBox.getSelectedItem());
		else if(e.getSource()==useAdjacencyConstraintChkBox){
			if(useAdjacencyConstraintChkBox.isSelected()){
				params.setUseEfficientLog(true);
				localAdjacencyConstraintSlider.setEnabled(true);
				//globalAdjacencyConstraintSlider.setEnabled(true);
			}else{
				localAdjacencyConstraintSlider.setEnabled(false);
				localAdjacencyConstraintSlider.setValue(localAdjacencyConstraintSlider.getSlider().getMaximum());
				//params.setMax_consecutive_nonfitting(Integer.MAX_VALUE);
				//globalAdjacencyConstraintSlider.setEnabled(false);
				//globalAdjacencyConstraintSlider.setValue(globalAdjacencyConstraintSlider.getSlider().getMaximum());
				//params.setMax_total_nonfitting(Integer.MAX_VALUE);
				if(!useTimeConstraintChkBox.isSelected())
					params.setUseEfficientLog(false);
			}
		}
		else if(e.getSource()==useTimeConstraintChkBox){
			if(useTimeConstraintChkBox.isSelected()){
				params.setUseEfficientLog(true);
				localTimeConstraintSlider.setEnabled(true);
				//globalTimeConstraintSlider.setEnabled(true);
				localTimeUnitComboBox.setEnabled(true);
				//globalTimeUnitComboBox.setEnabled(true);
			}else{
				localTimeConstraintSlider.setEnabled(false);
				localTimeUnitComboBox.setEnabled(false);
				localTimeConstraintSlider.setValue(localTimeConstraintSlider.getSlider().getMaximum());
				params.setMax_consecutive_timedif_millis(Integer.MAX_VALUE);
				//globalTimeConstraintSlider.setEnabled(false);
				//globalTimeUnitComboBox.setEnabled(false);
				//globalTimeConstraintSlider.setValue(globalTimeConstraintSlider.getSlider().getMaximum());
				//params.setMax_total_timedif_millis(Integer.MAX_VALUE);
				if(!useAdjacencyConstraintChkBox.isSelected())
					params.setUseEfficientLog(false);
			}
		}else if(e.getSource()==groupingComboBox){
    		SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					refreshResultsPanel(context);
				}
    		});
    	}else if(e.getSource()==overlayComboBox){
    		SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					refreshResultsPanel(context);
				}
    		});
    	}else if(e.getSource()==localTimeUnitComboBox){
    		TimeUnits timeUnit = (TimeUnits) localTimeUnitComboBox.getSelectedItem();
    		localTimeConstraintSlider.getSlider().setMaximum(timeUnit.getMaxValue());
    		localTimeConstraintSlider.setValue(1);
			params.setMax_consecutive_timedif_millis(timeUnitValuetoMillis(1, timeUnit));
    	//}else if(e.getSource()==globalTimeUnitComboBox){
    		//TimeUnits timeUnit = (TimeUnits) globalTimeUnitComboBox.getSelectedItem();
    		//globalTimeConstraintSlider.getSlider().setMaximum(timeUnit.getMaxValue());
    		//globalTimeConstraintSlider.setValue(1);
			//params.setMax_total_timedif_millis(timeUnitValuetoMillis(1, timeUnit));
    	}else if(e.getSource()==exportPetrinetsButton){
    		ProvidedObjectManager pom = context.getProvidedObjectManager();
    		for(int i=0; i<nets.getSize(); i++){
    			Object result = pom.createProvidedObject("LPM "+i, nets.getNet(i).getAcceptingPetriNet(), AcceptingPetriNet.class, context);
    			ProvidedObjectHelper.setFavorite(context, result, true);
    		}
    	}else if(e.getSource()==exportLPMRButton){
    		ProvidedObjectManager pom = context.getProvidedObjectManager();
    		if(params instanceof UtilityLocalProcessModelParameters){
        		UtilityLocalProcessModelRanking petriList = UtilityLocalProcessModelRankingFactory.createCountedAcceptingPetriNetArray();
        		for(int i=0; i<nets.getSize(); i++){
        			petriList.addElement((UtilityLocalProcessModel) nets.getElement(i));
        		}
    	  		Object result = pom.createProvidedObject("High-Utility Local Process Model Ranking", petriList, UtilityLocalProcessModelRanking.class, context);
        		ProvidedObjectHelper.setFavorite(context, result, true);
    		}else{
     	  		Object result = pom.createProvidedObject("Local Process Model Ranking", nets, LocalProcessModelRanking.class, context);
        		ProvidedObjectHelper.setFavorite(context, result, true);
    		}
  
    	}
	}

}