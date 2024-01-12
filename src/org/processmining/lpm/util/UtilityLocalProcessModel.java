package org.processmining.lpm.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class UtilityLocalProcessModel extends LocalProcessModel {
	private UtilityLocalProcessModel previousLPM;
	private double utility;
	private List<Double> utilityList;
	transient private Map<Transition, Double> achievedUtilityMap;
	private Map<String, Double> potentialUtilityMap;

	public UtilityLocalProcessModel(AcceptingPetriNet apn, double harmonicConfidence, int frequency, double alignmentCost, double precision, double remainedRatio, double avgNumFirings, double languageRatio, double avgEnabledTransitions, Set<List<String>> language, Set<List<String>> languageSeen, Map<Transition, Integer> countsMap, double utility, Map<Transition, Double> achievedUtilityMap, Map<String, Double> potentialUtilityMap, UtilityLocalProcessModelParameters params){
		super(apn, harmonicConfidence, frequency, alignmentCost, precision, remainedRatio, avgNumFirings, languageRatio, avgEnabledTransitions, language, languageSeen, countsMap, params);		
		this.utilityList = new ArrayList<Double>();
		this.utility = utility;
		this.achievedUtilityMap = achievedUtilityMap;
		this.potentialUtilityMap = potentialUtilityMap;
	}
	
	public UtilityLocalProcessModel(AcceptingPetriNet apn, Set<List<String>> language, Set<List<String>> languageSeen, Map<Transition, Integer> countsMap, UtilityLocalProcessModelParameters params){
		super(apn, 0, 0, 0, 0, 0, 0, 0, 0, null, null, null, params);		
		this.utilityList = new ArrayList<Double>();
		this.utility = 0d;
	}
	
	public UtilityLocalProcessModel(LocalProcessModel lpm)
	{
		super(lpm.getAcceptingPetriNet(), lpm.getConfidence(), lpm.getFrequency(), lpm.getAlignmentCost(), lpm.getPrecision(), lpm.getCoverage(), lpm.getAvgNumFirings(), lpm.getLanguageFit(), lpm.getDeterminism(), lpm.getLanguage(), lpm.getLanguageSeen(), lpm.getCountsMap(), lpm.getParameters());
		this.utilityList = new ArrayList<Double>();
		this.utility = 0d;
	}
	
	public Double getPotentialUtility(String activity){
		return potentialUtilityMap.get(activity);
	}
	
	public Double getAchievedUtility(Transition t){
		return achievedUtilityMap.get(t);
	}

	public List<Double> getUtilityList(){
		return utilityList;
	}
	
	public void copyUtilityList(List<Double> utilityList){
		this.utilityList = new ArrayList<Double>(utilityList);
	}
	
	public void addToUtilityList(double newUtility){
		utilityList.add(newUtility);
	}
	
	public double getUtility(){
		return utility;
	}
	
	public void setUtility(double utility){
		this.utility = utility;
	}
	
	@Override
	public int compareTo(LocalProcessModel o) {
		if(o instanceof UtilityLocalProcessModel){
			UtilityLocalProcessModel uo = (UtilityLocalProcessModel) o;
			if(uo.getUtility()==this.getUtility()){
				if(uo.getLanguage().equals(this.getLanguage()))
					return 0;
				else{
					return uo.getWeightedScore() > this.getWeightedScore() ? 1 : -1;
				}
			}else 
				return uo.getUtility() > this.getUtility() ? 1 : -1;
		}else{
			return -1;
		}
	}

	@Override
	public String toString(){
		String baseString = super.toString();
		baseString += System.lineSeparator();
		baseString += "Utility:                 "+this.getUtility();
		return baseString;
	}

	@Override
	public JPanel getScorePanel(){
		JPanel panel = super.getScorePanel();
		panel.add(new JLabel("Utility:      "+getUtility()));
		return panel;
	}

	public void addToUtility(double i) {
		this.utility += i;
	}
}
