package org.processmining.lpm.dialogs;

import com.fluxicon.slickerbox.components.NiceSlider.Orientation;

public class NiceGroupedIntegerSliderImpl extends NiceGroupedIntegerSlider{

	private static final long serialVersionUID = 2349521989482489896L;

	public NiceGroupedIntegerSliderImpl(String title, int min, int max, int initial, NiceSliderGroup sliderGroup, Orientation orientation) {
		super(title, min, max, initial, sliderGroup, orientation);
	}

	protected String formatValue(int value) {
		return ""+value;
	}

	public int getValue() {
		return slider.getValue();
	}

	public void setValue(int value) {
		slider.setValue(value);
	}

}
