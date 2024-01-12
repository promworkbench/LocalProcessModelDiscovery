package org.processmining.lpm.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.ui.SlickerSliderUI;

public abstract class NiceGroupedSlider extends JPanel implements NiceSliderGroupListener{ 
         
	private static final long serialVersionUID = 4557843725748850677L;
	protected JSlider slider; 
    protected JLabel title; 
    protected JLabel label; 
    protected Font font;
    protected NiceSliderGroup sliderGroup;
    protected boolean manualChange;
    
    protected NiceGroupedSlider(String title, int min, int max, int initial, NiceSliderGroup sliderGroup, Orientation orientation) { 
    	this.manualChange = true;
    	this.sliderGroup = sliderGroup;
        if(orientation.equals(Orientation.HORIZONTAL)) { 
            this.setMinimumSize(new Dimension(200, 25)); 
            this.setMaximumSize(new Dimension(4000, 25)); 
            this.setPreferredSize(new Dimension(500, 25)); 
            this.slider = new JSlider(JSlider.HORIZONTAL, min, max, initial); 
        } else { 
            this.setMinimumSize(new Dimension(50, 100)); 
            this.setMaximumSize(new Dimension(200, 4000)); 
            this.setPreferredSize(new Dimension(100, 500)); 
            this.slider = new JSlider(JSlider.VERTICAL, min, max, initial); 
        } 
        this.slider.setUI(new SlickerSliderUI(this.slider)); 
        this.slider.setOpaque(false); 
        this.slider.addChangeListener(new ChangeListener() { 
            public void stateChanged(ChangeEvent evt) { 
            	label.setText(formatValue(slider.getValue())); 
            } 
        }); 
        this.slider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); 
        this.label = new JLabel(formatValue(initial)); 
		font = this.label.getFont().deriveFont(11f);
        this.label.setMinimumSize(new Dimension(30, 20));
        this.label.setPreferredSize(new Dimension(30, 20)); 
        this.label.setFont(font); 
        this.label.setHorizontalAlignment(JLabel.LEFT); 
        this.label.setHorizontalTextPosition(JLabel.LEFT); 
        this.label.setAlignmentX(JLabel.LEFT_ALIGNMENT); 
        this.label.setVerticalAlignment(JLabel.CENTER); 
        this.label.setAlignmentY(JLabel.CENTER_ALIGNMENT); 
        this.label.setVerticalTextPosition(JLabel.CENTER); 
        this.label.setOpaque(false); 
        this.title = new JLabel(title + ":"); 
        this.title.setPreferredSize(sliderGroup.getRequiredTitleDimension()); 
        this.title.setFont(font); 
        this.title.setOpaque(false); 
        this.title.setHorizontalAlignment(JLabel.LEFT); 
        this.title.setHorizontalTextPosition(JLabel.LEFT); 
        this.title.setAlignmentX(JLabel.LEFT_ALIGNMENT); 
        this.title.setVerticalAlignment(JLabel.CENTER); 
        this.title.setAlignmentY(JLabel.CENTER_ALIGNMENT); 
        this.title.setVerticalTextPosition(JLabel.CENTER); 
        this.setBorder(BorderFactory.createEmptyBorder()); 
        this.setOpaque(false); 
        this.setLayout(new BorderLayout()); 
        if(orientation.equals(Orientation.HORIZONTAL)) { 
            this.add(this.title, BorderLayout.WEST); 
            this.add(this.label, BorderLayout.EAST); 
        } else { 
            this.add(this.title, BorderLayout.NORTH); 
            this.add(this.label, BorderLayout.SOUTH); 
        } 
        this.add(this.slider, BorderLayout.CENTER);
        sliderGroup.add(this);
    } 
    
    public String getTitleText(){
    	return title.getText();
    }
    
    public Font getFont(){
    	return font;
    }
    
    public void addChangeListener(ChangeListener listener) { 
        this.slider.addChangeListener(listener); 
    } 
    
    public JSlider getSlider() { 
        return this.slider; 
    } 
    
    public void setEnabled(boolean enabled) { 
        this.slider.setEnabled(enabled); 
    } 
    
    public boolean getEnabled() { 
        return this.slider.isEnabled(); 
    } 
    
    protected abstract String formatValue(int value);  
    
	public void updateTitleSize() {
        this.title.setPreferredSize(sliderGroup.getRequiredTitleDimension()); 
	}
	
	public void stateChanged(ChangeEvent e) {
		if(manualChange && sliderGroup.isKeepConstant()){
			manualChange = false;
			double sum = 0d;
			for(NiceGroupedSlider slider : sliderGroup){
				if(slider instanceof NiceGroupedDoubleSlider)
					sum += ((NiceGroupedDoubleSlider) slider).getValue();
				if(slider instanceof NiceGroupedIntegerSlider)
					sum += ((NiceGroupedIntegerSlider) slider).getValue();
			}
			double scalingFactor = sum / sliderGroup.getConstantSum();
			for(NiceGroupedSlider slider : sliderGroup){
				if(slider instanceof NiceGroupedDoubleSlider){
					NiceGroupedDoubleSlider sliderAsDoubleSlider = (NiceGroupedDoubleSlider) slider;
					sliderAsDoubleSlider.setValue(sliderAsDoubleSlider.getValue()/scalingFactor);
				}
				if(slider instanceof NiceGroupedIntegerSlider){
					NiceGroupedIntegerSlider sliderAsIntegerSlider = (NiceGroupedIntegerSlider) slider;
					sliderAsIntegerSlider.setValue((int) Math.round(( (sliderAsIntegerSlider.getValue() ) / scalingFactor)));
				}
			}
			manualChange = true;
		}
	}
}