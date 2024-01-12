package org.processmining.lpm.dialogs;

import com.fluxicon.slickerbox.components.NiceSlider.Orientation;

public abstract class NiceGroupedIntegerSlider extends NiceGroupedSlider{

	private static final long serialVersionUID = 2349521989482489896L;

	protected NiceGroupedIntegerSlider(String title, int min, int max, int initial, NiceSliderGroup sliderGroup, Orientation orientation) {
		super(title, min, max, initial, sliderGroup, orientation);
	}

	public abstract int getValue();

	public abstract void setValue(int value);
}
