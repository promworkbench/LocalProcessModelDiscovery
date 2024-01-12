package org.processmining.lpm.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.processtree.ProcessTree;

public class LocalProcessModel implements Comparable<LocalProcessModel>, Serializable{
	private static final long serialVersionUID = -5570330432405039495L;

	private transient AcceptingPetriNet apn;

	private transient Map<Transition, Integer> countsMap;
	
	private int frequency;
	private double alignmentCost;
	private double precision;
	private double remainedRatio;
	private double avgNumFirings;
	private double normAvgNumFirings;
	private double languageRatio;
	private double determinism;
	private double harmonicConfidence;
	private double numTransitionsScore;
	private int numTransitions;
	private Set<List<String>> language;
	private Set<List<String>> languageSeen;
	private Map<String, Integer> logActivityCountMap;
	private LocalProcessModelParameters params;
	private transient ProcessTree processTree;
	private transient PNMatchInstancesRepResult alignments;
	
	public LocalProcessModel(AcceptingPetriNet apn, double harmonicConfidence, int frequency, double alignmentCost, double precision, double remainedRatio, double avgNumFirings, double languageRatio, double avgEnabledTransitions, Set<List<String>> language, Set<List<String>> languageSeen, Map<Transition, Integer> countsMap, LocalProcessModelParameters params){
		this.apn = apn;
		this.harmonicConfidence = harmonicConfidence;
		this.frequency = frequency;
		this.alignmentCost = alignmentCost;
		this.precision = precision;
		this.remainedRatio = remainedRatio;
		this.avgNumFirings = avgNumFirings;
		this.normAvgNumFirings = avgNumFirings/(avgNumFirings+1);
		this.languageRatio = languageRatio;
		this.language = language;
		this.languageSeen = languageSeen;
		this.determinism = avgEnabledTransitions;
		this.numTransitionsScore = (((double)apn.getNet().getTransitions().size())/params.getNumTransitions());
		this.countsMap = countsMap;
		this.numTransitions = countsMap.keySet().size();
		this.params = params;
	}
	
	public LocalProcessModel(AcceptingPetriNet apn){
		this.apn = apn;
	}
	
	public Map<String, Integer> getLogActivityCountMap(){
		return logActivityCountMap;
	}
	
	public void setLogActivityCountMap(Map<String, Integer> logActivityCountsMap){
		this.logActivityCountMap = logActivityCountsMap;
	}
	
	public AcceptingPetriNet getAcceptingPetriNet(){
		return apn;
	}
	
	public double getLanguageFit(){
		return languageRatio;
	}
	
	public double getSupport(){
		return frequency ==0 ? 0 : (Math.log(frequency)/Math.log(10))/((Math.log(frequency)/Math.log(10))+1);
	}
	
	public int getFrequency(){
		return frequency;
	}
	
	public double getAlignmentCost(){
		return alignmentCost;
	}
	
	public double getPrecision(){
		return precision;
	}
	
	public double getCoverage(){
		return remainedRatio;
	}
	
	public double getDeterminism(){
		return determinism;
	}
	
	public double getAvgNumFirings(){
		return avgNumFirings;
	}
	
	public double getConfidence(){
		return harmonicConfidence;
	}
	
	public double getWeightedScore(){
		double weightedExpression = 0;
		
		weightedExpression += params.getSupportWeight()*getSupport();
		weightedExpression += params.getDeterminismWeight()*determinism;
		//if(Double.isNaN(alignmentCost))
		//	alignmentCost=0d;
		weightedExpression += params.getConfidenceWeight()*harmonicConfidence;
		//weightedExpression += 3*alignmentCost;
		//weightedExpression += precision;
		weightedExpression += params.getAvgNumFiringsWeight()*normAvgNumFirings;
		weightedExpression += params.getCoverageWeight()*remainedRatio;
		weightedExpression += params.getLanguageFitWeight()*languageRatio;
		weightedExpression += params.getNumTransitionsWeight()*numTransitionsScore;
		weightedExpression /= params.getTotalWeight();
		if(numTransitions<=1)
			weightedExpression = 0;
		return weightedExpression;
	}
	
	public int compareTo(LocalProcessModel o) {
		int hashComparison = Integer.compare(hashCode(), o.hashCode());
		if(hashComparison==0) // equal nets
			return 0;
		double weightedExpression = getWeightedScore()-o.getWeightedScore();
		int result = weightedExpression > 0 ? -1 : weightedExpression < 0 ? 1 : 0;
		if(result==0)
			return hashComparison; // Fallback option to make sure that we don't label Petri nets with equal quality as identical
		return result;
	}
	
	@Override
	public int hashCode(){
		return language.hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if(!(other instanceof LocalProcessModel))
			return false;
		LocalProcessModel apnOther = (LocalProcessModel) other;
		return language.equals(apnOther.getLanguage());
	}
	
	public Set<String> getAlphabet(){
		Set<String> alphabet = new HashSet<String>();
		for(List<String> lanAlphabet : language){
			alphabet.addAll(lanAlphabet);
		}
		return alphabet;
	}
	
	public Set<List<String>> getLanguage(){
		return language;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Transitions:             "+apn.getNet().getTransitions());
		sb.append(System.lineSeparator());
		sb.append("Weighted value:          "+getWeightedScore());
		sb.append(System.lineSeparator());
		sb.append("Freq alignment:          "+getSupport());
		sb.append(System.lineSeparator());
		sb.append("Remained ratio:          "+getCoverage());
		sb.append(System.lineSeparator());
		sb.append("Avg enabled transitions: "+getDeterminism());
		sb.append(System.lineSeparator());
		sb.append("Avg num firings:         "+normAvgNumFirings);
		sb.append(System.lineSeparator());
		sb.append("Language ratio:          "+getLanguageFit());
		sb.append(System.lineSeparator());
		sb.append("Harmonic confidence:     "+getConfidence());
		sb.append(System.lineSeparator());
		sb.append("Fitness:                 "+getAlignmentCost());
		sb.append(System.lineSeparator());
		sb.append("Precision:               "+getPrecision());
		sb.append(System.lineSeparator());
		sb.append("Language:                "+language);
		sb.append(System.lineSeparator());
		sb.append("Language seen:           "+languageSeen);
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	public Integer getCount(Transition t) {
		return countsMap.containsKey(t) ? countsMap.get(t) : 0;
	}
	
	public JPanel getScorePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(new JLabel("Score:        "+getWeightedScore()));
		panel.add(new JLabel("Frequency:    "+getFrequency()));
		panel.add(new JLabel("Confidence:   "+getConfidence()));
		panel.add(new JLabel("Determinism:  "+getDeterminism()));
		panel.add(new JLabel("Language fit: "+getLanguageFit()));
		panel.add(new JLabel("Coverage:     "+getCoverage()));
		return panel;
	}

	public Set<List<String>> getLanguageSeen() {
		return languageSeen;
	}

	public Map<Transition, Integer> getCountsMap() {
		return countsMap;
	}

	public LocalProcessModelParameters getParameters() {
		return params;
	}
	
	public void setParameters(LocalProcessModelParameters lpmp){
		this.params = lpmp;
	}
	
	public double getRemainedRatio() {
		return remainedRatio;
	}

	public void setRemainedRatio(double remainedRatio) {
		this.remainedRatio = remainedRatio;
	}

	public double getNormAvgNumFirings() {
		return normAvgNumFirings;
	}

	public void setNormAvgNumFirings(double normAvgNumFirings) {
		this.normAvgNumFirings = normAvgNumFirings;
	}

	public double getLanguageRatio() {
		return languageRatio;
	}

	public void setLanguageRatio(double languageRatio) {
		this.languageRatio = languageRatio;
	}

	public void setDeterminism(double determinism) {
		this.determinism = determinism;
	}

	public double getHarmonicConfidence() {
		return harmonicConfidence;
	}

	public void setHarmonicConfidence(double harmonicConfidence) {
		this.harmonicConfidence = harmonicConfidence;
	}

	public double getNumTransitionsScore() {
		return numTransitionsScore;
	}

	public void setNumTransitionsScore(double numTransitionsScore) {
		this.numTransitionsScore = numTransitionsScore;
	}

	public LocalProcessModelParameters getParams() {
		return params;
	}

	public void setParams(LocalProcessModelParameters params) {
		this.params = params;
	}

	public void setCountsMap(Map<Transition, Integer> countsMap) {
		this.countsMap = countsMap;
		this.numTransitions = countsMap.keySet().size();
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public void setAlignmentCost(double alignmentCost) {
		this.alignmentCost = alignmentCost;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public void setAvgNumFirings(double avgNumFirings) {
		this.avgNumFirings = avgNumFirings;
	}

	public void setLanguage(Set<List<String>> language) {
		this.language = language;
	}

	public void setLanguageSeen(Set<List<String>> languageSeen) {
		this.languageSeen = languageSeen;
	}

	public ProcessTree getProcessTree() {
		return processTree;
	}

	public void setProcessTree(ProcessTree processTree) {
		this.processTree = processTree;
	}

	public PNMatchInstancesRepResult getAlignments() {
		return alignments;
	}

	public void setAlignments(PNMatchInstancesRepResult alignments) {
		this.alignments = alignments;
	}
}
