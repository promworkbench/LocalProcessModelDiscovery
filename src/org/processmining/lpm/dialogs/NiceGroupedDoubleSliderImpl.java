package org.processmining.lpm.dialogs;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.event.ChangeListener;

import com.fluxicon.slickerbox.components.NiceSlider.Orientation;

public class NiceGroupedDoubleSliderImpl extends NiceGroupedDoubleSlider implements ChangeListener{ 

	private static final long serialVersionUID = -4945418007078721552L;
	protected double min; 
    protected double max; 
    protected static NumberFormat format = new DecimalFormat("#.###");  
    protected NiceSliderGroup sliderGroup;

    /** 
     * @param title 
     * @param min 
     * @param max 
     * @param initial 
     */ 
    public NiceGroupedDoubleSliderImpl(String title, double min, double max, double initial, NiceSliderGroup sliderGroup, Orientation orientation) { 
        super(title, 0, Integer.MAX_VALUE, (int)(Integer.MAX_VALUE * (initial - min) / (max - min)), sliderGroup, orientation); 
        this.min = min; 
        this.max = max;
        this.sliderGroup = sliderGroup;
        label.setText(formatValue(slider.getValue())); 
        this.addChangeListener(this);
        this.manualChange = true;
    } 
    
    public NiceGroupedDoubleSliderImpl(String title, double min, double max, double initial, NiceSliderGroup sliderGroup) { 
        this(title, min, max, initial, sliderGroup, Orientation.HORIZONTAL); 
    } 

    protected String formatValue(int value) { 
        return format.format(min + ((double)value / Integer.MAX_VALUE) * (max - min)); 
    } 
    
    public double getValue() { 
        return min + (((double)slider.getValue() / Integer.MAX_VALUE) * (max - min)); 
    } 
    
    public void setValue(double value) { 
        double relative = (value - min) / (max - min); 
        int intValue = (int)(relative * Integer.MAX_VALUE); 
        slider.setValue(intValue); 
        label.setText(format.format(value)); 
    }
    
}