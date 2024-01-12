package org.processmining.lpm.dialogs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

public class NiceSliderGroup extends HashSet<NiceGroupedSlider> {
	private boolean keepConstant;
	private double constantSum;
	
	private static final long serialVersionUID = 4396237755554185214L;
	
	public NiceSliderGroup(){
		super();
		this.setKeepConstant(false);
		this.setConstantSum(-1);
	}
	
	public NiceSliderGroup(boolean keepConstant, double constantSum){
		super();
		this.setKeepConstant(keepConstant);
		this.setConstantSum(constantSum);
	}
	
	public NiceSliderGroup(Set<NiceGroupedSlider> sliders){
		super();
		this.addAll(sliders);
	}

	public Dimension getRequiredTitleDimension() {
		int maxWidth = 0;
		int maxHeight = 0;
		for(NiceGroupedSlider slider : this){
			FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, false);    
			Font font = slider.getFont();
			Rectangle2D rect = font.getStringBounds(slider.getTitleText(), frc);
			maxWidth = Math.max(maxWidth, (int) rect.getWidth());
			maxHeight = Math.max(maxHeight, (int) rect.getHeight());
		}
		return new Dimension(maxWidth, maxHeight);
	}

	public boolean isKeepConstant() {
		return keepConstant;
	}

	public void setKeepConstant(boolean keepConstant) {
		this.keepConstant = keepConstant;
	}

	public double getConstantSum() {
		return constantSum;
	}

	public void setConstantSum(double constantSum) {
		this.constantSum = constantSum;
	}
	
	@Override
	public boolean add(NiceGroupedSlider e){
		Dimension oldDimension = getRequiredTitleDimension();
		boolean value = super.add(e);
		Dimension newDimension = getRequiredTitleDimension();
		if(!oldDimension.equals(newDimension)){
			for(NiceGroupedSlider slider : this){
				slider.updateTitleSize();
			}
		}
		return value;
	}
}
