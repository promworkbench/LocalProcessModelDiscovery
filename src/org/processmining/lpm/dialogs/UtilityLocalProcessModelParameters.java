package org.processmining.lpm.dialogs;

import org.deckfour.xes.model.XLog;

public class UtilityLocalProcessModelParameters extends LocalProcessModelParameters{	
	private static final long serialVersionUID = -7759011251214453323L;

	public enum PruningStrategies {PREVIOUS_LEQ, PREVIOUS_L, BEST_LEQ, BEST_L, NONE};
	
	private String utilityAttributeName;
	private double utilityMinimum;
	private PruningStrategies pruningStrategy;
	private int pruningK;
	
	public UtilityLocalProcessModelParameters(){
		super();
		setUtilityAttributeName("Costs");
		setUtilityMinimum(5000);
		setPruningStrategy(PruningStrategies.NONE);
		setPruningK(this.getNumTransitions());
	}
	
	public UtilityLocalProcessModelParameters(UtilityLocalProcessModelParameters other){ // makes a copy of an other LPM parameter object
		super(other);
		setUtilityAttributeName(other.getUtilityAttributeName());
		setUtilityMinimum(other.getUtilityMinimum());
		setPruningStrategy(other.getPruningStrategy());
		setPruningK(other.getPruningK());
	}

	public String getUtilityAttributeName() {
		return utilityAttributeName;
	}

	public void setUtilityAttributeName(String utilityAttributeName) {
		this.utilityAttributeName = utilityAttributeName;
	}
	
	@Override
	public LocalProcessModelParameters copy(){
		LocalProcessModelParameters copy = new UtilityLocalProcessModelParameters(this);
		return copy;
	}
	
	@Override
	public void setSmartParameterDefaultsForLog(XLog log)
	{
		setFrequencyMinimum(0);
		setDeterminismMinimum(0);
		setConfidenceMinimum(0);
		setLanguageFitMinimum(0);
		setCoverageMinimum(0);
		setUtilityMinimum(0);
		setTop_k(500);
	}

	public double getUtilityMinimum() {
		return utilityMinimum;
	}

	public void setUtilityMinimum(double utilityMinimum) {
		this.utilityMinimum = utilityMinimum;
	}

	public PruningStrategies getPruningStrategy() {
		return pruningStrategy;
	}

	public void setPruningStrategy(PruningStrategies strategy) {
		this.pruningStrategy = strategy;
	}

	public int getPruningK() {
		return pruningK;
	}

	public void setPruningK(int k) {
		this.pruningK = k;
	}
}