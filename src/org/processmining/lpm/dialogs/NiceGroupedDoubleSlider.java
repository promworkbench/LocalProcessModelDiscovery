package org.processmining.lpm.dialogs;

import com.fluxicon.slickerbox.components.NiceSlider.Orientation;

public abstract class NiceGroupedDoubleSlider extends NiceGroupedSlider{

	private static final long serialVersionUID = 7173214031675006650L;

	protected NiceGroupedDoubleSlider(String title, int min, int max, int initial, NiceSliderGroup sliderGroup, Orientation orientation) {
		super(title, min, max, initial, sliderGroup, orientation);
	}

	public abstract double getValue();

	public abstract void setValue(double value);
	
}
