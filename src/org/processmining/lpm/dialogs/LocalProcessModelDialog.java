package org.processmining.lpm.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.processmining.framework.util.ui.widgets.ProMScrollablePanel;
import org.processmining.lpm.dialogs.LocalProcessModelParameters.ProjectionMethods;

import com.fluxicon.slickerbox.colors.SlickerColors;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

public class LocalProcessModelDialog extends ProMScrollablePanel implements ActionListener, ChangeListener{

	private static final long serialVersionUID = -2738311006716982301L;

	private LocalProcessModelParameters lpmp;
	
	private JCheckBox duplicateTransitionChkBx;
	private JCheckBox seqChkBx;
	private JCheckBox andChkBx;
	private JCheckBox orChkBx;
	private JCheckBox xorChkBx;
	private JCheckBox xorloopChkBx;
	
	private NiceGroupedIntegerSlider numTransitionsSlider;
	private NiceGroupedIntegerSlider top_kSlider;
	
	private NiceGroupedIntegerSlider frequencyMinimumSlider;
	private NiceGroupedDoubleSlider minimumDeterminismSlider;
	
	private JComboBox<ProjectionMethods> projectionCBox;
	
	private NiceGroupedDoubleSlider wSupportSlider;
	private NiceGroupedDoubleSlider wLanguageFitSlider;
	private NiceGroupedDoubleSlider wConfidenceSlider;
	private NiceGroupedDoubleSlider wCoverageSlider;
	private NiceGroupedDoubleSlider wDeterminismSlider;
	private NiceGroupedDoubleSlider wAvgNumFiringsSlider;
		
	boolean manualChange = true;

	
	public LocalProcessModelDialog(LocalProcessModelParameters params){
		this.lpmp = params;
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBackground(SlickerColors.COLOR_BG_1);
		
		JPanel pane0 = SlickerFactory.instance().createTabbedPane("General", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		NiceSliderGroup generalGroup = new NiceSliderGroup();
		pane0.setLayout(new BoxLayout(pane0, BoxLayout.PAGE_AXIS));
		numTransitionsSlider = new NiceGroupedIntegerSliderImpl("Maximum number of transitions in LPMs", 1, 5, 4, generalGroup, Orientation.HORIZONTAL);
		numTransitionsSlider.addChangeListener(this);
		pane0.add(numTransitionsSlider);
		top_kSlider = new NiceGroupedIntegerSliderImpl("Number of LPMs to discover", 1, 500, 20, generalGroup, Orientation.HORIZONTAL);
		top_kSlider.addChangeListener(this);
		pane0.add(top_kSlider);
		JPanel dupPanel = new JPanel(new BorderLayout());
		dupPanel.setBackground(SlickerColors.COLOR_TRANSPARENT);
		duplicateTransitionChkBx = SlickerFactory.instance().createCheckBox("Allow duplicate transitions", params.isDuplicateTransitions());
		duplicateTransitionChkBx.addActionListener(this);
		dupPanel.add(duplicateTransitionChkBx, BorderLayout.WEST);
		pane0.add(dupPanel);
		this.add(pane0);
		
		JPanel pane1 = SlickerFactory.instance().createTabbedPane("Operators", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
		
		JPanel seqPanel = new JPanel(new BorderLayout());
		seqPanel.setBackground(SlickerColors.COLOR_TRANSPARENT);
		seqChkBx = SlickerFactory.instance().createCheckBox("Use sequence operator", params.isUseSeq());
		seqChkBx.setAlignmentX(Component.LEFT_ALIGNMENT);
		seqChkBx.addActionListener(this);
		seqPanel.add(seqChkBx, BorderLayout.WEST);
		pane1.add(seqPanel);
		
		JPanel andPanel = new JPanel(new BorderLayout());
		andPanel.setBackground(SlickerColors.COLOR_TRANSPARENT);
		andChkBx = SlickerFactory.instance().createCheckBox("Use concurrency operator", params.isUseAnd());
		andChkBx.addActionListener(this);
		andPanel.add(andChkBx, BorderLayout.WEST);
		pane1.add(andPanel);
		
		JPanel orPanel = new JPanel(new BorderLayout());
		orPanel.setBackground(SlickerColors.COLOR_TRANSPARENT);
		orChkBx = SlickerFactory.instance().createCheckBox("Use inclusive choice operator", params.isUseOr());
		orChkBx.addActionListener(this);
		orPanel.add(orChkBx, BorderLayout.WEST);
		pane1.add(orPanel);
		
		JPanel xorPanel = new JPanel(new BorderLayout());
		xorPanel.setBackground(SlickerColors.COLOR_TRANSPARENT);
		xorChkBx = SlickerFactory.instance().createCheckBox("Use exclusve choice operator", params.isUseXor());
		xorChkBx.addActionListener(this);
		xorPanel.add(xorChkBx, BorderLayout.WEST);
		pane1.add(xorPanel);
		
		JPanel xorloopPanel = new JPanel(new BorderLayout());
		xorloopPanel.setBackground(SlickerColors.COLOR_TRANSPARENT);
		xorloopChkBx = SlickerFactory.instance().createCheckBox("Use loop operator", params.isUseXorloop());
		xorloopChkBx.addActionListener(this);
		xorloopPanel.add(xorloopChkBx, BorderLayout.WEST);
		pane1.add(xorloopPanel);
		this.add(pane1);

		JPanel pane2 = SlickerFactory.instance().createTabbedPane("Pruning", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_BG_1, SlickerColors.COLOR_FG);
		NiceSliderGroup pruningGroup = new NiceSliderGroup();
		pane2.setLayout(new BoxLayout(pane2, BoxLayout.PAGE_AXIS));
		frequencyMinimumSlider = new NiceGroupedIntegerSliderImpl("Minimum number of occurrences in log", 1, params.getMaxActivityFrequencyInLog(), params.getFrequencyMinimum(), pruningGroup, Orientation.HORIZONTAL);
		frequencyMinimumSlider.addChangeListener(this);
		pane2.add(frequencyMinimumSlider);
		
		minimumDeterminismSlider = new NiceGroupedDoubleSliderImpl("Minimum determinism", 0, 1, 0.5, pruningGroup, Orientation.HORIZONTAL);
		minimumDeterminismSlider.addChangeListener(this);
		pane2.add(minimumDeterminismSlider);
		this.add(pane2);
		
		JPanel pane3 = SlickerFactory.instance().createTabbedPane("Projections", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		projectionCBox = SlickerFactory.instance().createComboBox(ProjectionMethods.values());
		projectionCBox.setSelectedItem(params.getProjectionMethod());			
		projectionCBox.addActionListener(this);
		pane3.add(projectionCBox);
		this.add(pane3);		
		
		NiceSliderGroup metricGroup = new NiceSliderGroup(true, 1d);
		JPanel pane4 = SlickerFactory.instance().createTabbedPane("Ranking", SlickerColors.COLOR_TRANSPARENT, SlickerColors.COLOR_FG, SlickerColors.COLOR_FG);
		pane4.setLayout(new BoxLayout(pane4, BoxLayout.PAGE_AXIS));
		wSupportSlider = new NiceGroupedDoubleSliderImpl("Support", 0, 1, lpmp.getSupportWeight()/lpmp.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wSupportSlider.addChangeListener(this);
		pane4.add(wSupportSlider);
		
		wLanguageFitSlider = new NiceGroupedDoubleSliderImpl("Language fit", 0, 1, lpmp.getLanguageFitWeight()/lpmp.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wLanguageFitSlider.addChangeListener(this);
		pane4.add(wLanguageFitSlider);
		
		wConfidenceSlider = new NiceGroupedDoubleSliderImpl("Confidence", 0, 1, lpmp.getConfidenceWeight()/lpmp.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wConfidenceSlider.addChangeListener(this);
		pane4.add(wConfidenceSlider);
		
		wCoverageSlider = new NiceGroupedDoubleSliderImpl("Coverage", 0, 1, lpmp.getCoverageWeight()/lpmp.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wCoverageSlider.addChangeListener(this);
		pane4.add(wCoverageSlider);
		
		wDeterminismSlider = new NiceGroupedDoubleSliderImpl("Determinism", 0, 1, lpmp.getDeterminismWeight()/lpmp.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wDeterminismSlider.addChangeListener(this);
		pane4.add(wDeterminismSlider);
		
		// TODO: deze hiden, of advanced option van maken
		wAvgNumFiringsSlider = new NiceGroupedDoubleSliderImpl("Avg. num. firings", 0, 1, lpmp.getAvgNumFiringsWeight()/lpmp.getTotalWeight(), metricGroup, Orientation.HORIZONTAL);
		wAvgNumFiringsSlider.addChangeListener(this);
		pane4.add(wAvgNumFiringsSlider);
		this.add(pane4);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==duplicateTransitionChkBx)
			lpmp.setDuplicateTransitions(duplicateTransitionChkBx.isSelected());
		if(e.getSource()==seqChkBx)
			lpmp.setUseSeq(seqChkBx.isSelected());
		if(e.getSource()==andChkBx)
			lpmp.setUseAnd(andChkBx.isSelected());
		if(e.getSource()==orChkBx)
			lpmp.setUseOr(orChkBx.isSelected());
		if(e.getSource()==xorChkBx)
			lpmp.setUseXor(xorChkBx.isSelected());
		if(e.getSource()==xorloopChkBx)
			lpmp.setUseXorloop(xorloopChkBx.isSelected());
		if(e.getSource()==projectionCBox){
			lpmp.setProjectionMethod((ProjectionMethods) projectionCBox.getSelectedItem());
		}
	}

	public void stateChanged(ChangeEvent e) {
		Container trigger = ((JComponent)e.getSource()).getParent();
		if(trigger == numTransitionsSlider)
			lpmp.setNumTransitions(numTransitionsSlider.getValue());
		if(trigger == top_kSlider)
			lpmp.setTop_k(top_kSlider.getValue());
		if(trigger == frequencyMinimumSlider)
			lpmp.setFrequencyMinimum(frequencyMinimumSlider.getValue());
		if(trigger == minimumDeterminismSlider)
			lpmp.setDeterminismMinimum(minimumDeterminismSlider.getValue());
		
		if(trigger == wSupportSlider){
			lpmp.setSupportWeight(wSupportSlider.getValue());
		}
		if(trigger == wLanguageFitSlider){
			lpmp.setLanguageFitWeight(wLanguageFitSlider.getValue());
		}
		if(trigger == wConfidenceSlider){
			lpmp.setConfidenceWeight(wConfidenceSlider.getValue());
		}
		if(trigger == wCoverageSlider){
			lpmp.setCoverageWeight(wCoverageSlider.getValue());
		}
		if(trigger == wDeterminismSlider){
			lpmp.setDeterminismWeight(wDeterminismSlider.getValue());
		}
		if(trigger == wAvgNumFiringsSlider){
			lpmp.setAvgNumFiringsWeight(wAvgNumFiringsSlider.getValue());
		}
	}
}