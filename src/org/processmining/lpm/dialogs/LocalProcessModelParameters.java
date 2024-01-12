package org.processmining.lpm.dialogs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.lpm.efficientlog.extractors.EfficientLogExtractor;
import org.processmining.lpm.efficientlog.extractors.EventAndTimeGapExtractor;
import org.processmining.lpm.efficientlog.extractors.EventGapExtractor;
import org.processmining.lpm.efficientlog.extractors.GapAndTimeBasedExtractor;
import org.processmining.lpm.efficientlog.extractors.GapAndTimeBasedExtractor2;
import org.processmining.lpm.efficientlog.extractors.TimeGapExtractor;

public class LocalProcessModelParameters extends Observable implements Serializable{

	private static final long serialVersionUID = -3453143814286502264L;
	
	protected boolean logHasTime;
	protected boolean useEfficientLog;
	protected transient XLog discoveryLog;
	protected transient XLog evaluationLog;
	protected transient XEventClassifier classifier;
	protected int numTransitions;
	protected int top_k;
	protected int alignmentMaxNumStatesPerTransition;
	
	protected int frequencyMinimum;
	protected double languageFitMinimum;
	protected double confidenceMinimum;
	protected double coverageMinimum;
	protected double determinismMinimum;
	protected boolean duplicateTransitions;
	
	protected double supportWeight;
	protected double languageFitWeight;
	protected double confidenceWeight;
	protected double coverageWeight;
	protected double determinismWeight;
	protected double avgNumFiringsWeight;
	protected double numTransitionsWeight;
	
	protected transient EfficientLogExtractor efficientLogExtractor;
	protected int efficientLogCacheSize = Integer.MAX_VALUE-1;
	
	protected boolean useSeq;
	protected boolean useAnd;
	protected boolean useOr;
	protected boolean useXor;
	protected boolean useXorloop;
	
	protected int maxActivityFrequencyInLog;
	
	protected boolean returnMurataOrdered;
	
	protected transient ProjectionMethods projectionMethod;
	
	protected transient Map<String, Character> encodingScheme;
	protected transient Map<Character, String> decodingScheme;
	protected transient Set<String> attributesToKeep;
	
	protected AtomicInteger numberOfExploredLpms;
	
	// TODO: check, can this be removed?
	protected transient JComponent progressUpdateContainer;
	protected transient JLabel progressUpdateLabel;
	
	protected int max_consecutive_nonfitting = 0;
	protected int max_total_nonfitting = 0;
	protected long max_consecutive_timedif_millis = 60*60*1000/3;
	protected long max_total_timedif_millis = 2*60*60*1000;
	
	protected boolean storeProcessTree;
	protected boolean isVerbose;
	
	protected Long startTime;
	protected Long lastNotificationTime;
	protected int notificationPeriod = 10000;
	
	public enum ProjectionMethods {
	    None ("None"),
	    Markov ("Markov clustering"),
	    MRIG ("Maximal Relative Information Gain"),
	    Entropy ("Entropy");

	    private final String name;       

	    private ProjectionMethods(String s) {
	        name = s;
	    }

	    public boolean equalsName(String otherName) {
	        return (otherName == null) ? false : name.equals(otherName);
	    }

	    public String toString() {
	       return this.name;
	    }
	}
	
	public LocalProcessModelParameters copy(){
		LocalProcessModelParameters copy = new LocalProcessModelParameters(this);
		return copy;
	}
	
	public LocalProcessModelParameters(){
		setProjectionMethod(ProjectionMethods.None);
		setClassifier(new XEventNameClassifier());
		setNumTransitions(4);
		setTop_k(100);
		setAlignmentMaxNumStatesPerTransition(400);		
		
		setFrequencyMinimum(1000);
		setLanguageFitMinimum(0.49);
		setCoverageMinimum(0.0);
		setDeterminismMinimum(0.49);
		setConfidenceMinimum(0.0);
		
		setAvgNumFiringsWeight(0.1);
		setNumTransitionsWeight(0);
		setSupportWeight(0.1);
		setLanguageFitWeight(0.1);
		setConfidenceWeight(0.4);
		setCoverageWeight(0);
		setDeterminismWeight(0.3);
		
		setUseSeq(true);
		setUseAnd(true);
		setUseOr(false);
		setUseXor(true);
		setUseXorloop(true);
		
		setDuplicateTransitions(false);
		setReturnMurataOrdered(true);
		
		//efficientLogExtractor = new GapAndTimeBasedExtractor2(max_consecutive_nonfitting, max_total_nonfitting, max_consecutive_timedif_millis, max_total_timedif_millis);
		efficientLogExtractor = new EventAndTimeGapExtractor(max_consecutive_nonfitting, max_consecutive_timedif_millis);
		setUseEfficientLog(false);
		setAttributesToKeep(efficientLogExtractor.requiredAttributes());
		//setAttributesToKeep(new HashSet<String>());
		numberOfExploredLpms = new AtomicInteger();
		progressUpdateLabel = new JLabel(numberOfExploredLpms+" Local Process Models explored");
		setStoreProcessTree(false);
		logHasTime = false;
		setVerbose(true);
		setStartTime(System.currentTimeMillis());
		setLastNotificationTime(System.currentTimeMillis()-((int) 0.5*notificationPeriod));
	}
			
	public LocalProcessModelParameters(LocalProcessModelParameters other){ // makes a copy of an other LPM parameter object
		setProjectionMethod(other.getProjectionMethod());

		setDiscoveryLog(other.getDiscoveryLog());
		setEvaluationLog(other.getEvaluationLog());
		
		setClassifier(other.getClassifier());
		setNumTransitions(other.getNumTransitions());
		setTop_k(other.getTop_k());
		setAlignmentMaxNumStatesPerTransition(other.getAlignmentMaxNumStatesPerTransition());		
		
		setFrequencyMinimum(other.getFrequencyMinimum());
		setLanguageFitMinimum(other.getLanguageFitMinimum());
		setCoverageMinimum(other.getCoverageMinimum());
		setDeterminismMinimum(other.getDeterminismMinimum());
		setConfidenceMinimum(other.getConfidenceMinimum());
		setDuplicateTransitions(other.isDuplicateTransitions());

		setAvgNumFiringsWeight(other.getAvgNumFiringsWeight());
		setNumTransitionsWeight(other.getNumTransitionsWeight());
		setSupportWeight(other.getSupportWeight());
		setLanguageFitWeight(other.getLanguageFitWeight());
		setConfidenceWeight(other.getConfidenceWeight());
		setCoverageWeight(other.getCoverageWeight());
		setDeterminismWeight(other.getDeterminismWeight());
		
		setUseSeq(other.isUseSeq());
		setUseAnd(other.isUseAnd());
		setUseOr(other.isUseOr());
		setUseXor(other.isUseXor());
		setUseXorloop(other.isUseXorloop());
		
		setReturnMurataOrdered(other.isReturnMurataOrdered());
		
		setUseEfficientLog(other.isUseEfficientLog());
		setEfficientLogExtractor(other.getEfficientLogExtractor());
		setEfficientLogCacheSize(other.getEfficientLogCacheSize());
		setMaxActivityFrequencyInLog(other.getMaxActivityFrequencyInLog());
		setEncodingScheme(other.getEncodingScheme());
		setDecodingScheme(other.getDecodingScheme());
		setAttributesToKeep(other.getAttributesToKeep());
		setNumberOfExploredLpms(other.getNumberOfExploredLpms());
		setStoreProcessTree(other.isStoreProcessTree());
		logHasTime = other.logHasTime();
		other.setChanged();
		other.notifyObservers(this);
		setVerbose(other.isVerbose());
		setStartTime(other.getStartTime());
		setLastNotificationTime(other.getLastNotificationTime());
	}
	
	public XEventClassifier getClassifier() {
		return classifier;
	}
	
	public void setClassifier(XEventClassifier classifier) {
		this.classifier = classifier;
	}
	
	public int getNumTransitions() {
		return numTransitions;
	}
	
	public void setNumTransitions(int numTransitions) {
		this.numTransitions = numTransitions;
	}
	
	public int getTop_k() {
		return top_k;
	}
	
	public void setTop_k(int top_k) {
		this.top_k = top_k;
	}
	
	public boolean isDuplicateTransitions() {
		return duplicateTransitions;
	}
	
	public void setDuplicateTransitions(boolean duplicateTransitions) {
		this.duplicateTransitions = duplicateTransitions;
	}
	
	public int getFrequencyMinimum() {
		return frequencyMinimum;
	}
	
	public void setFrequencyMinimum(int frequencyMinimum) {
		this.frequencyMinimum = frequencyMinimum;
	}
	
	public double getLanguageFitMinimum() {
		return languageFitMinimum;
	}
	
	public void setLanguageFitMinimum(double languageRatioMinimum) {
		this.languageFitMinimum = languageRatioMinimum;
	}
	
	public double getConfidenceMinimum() {
		return confidenceMinimum;
	}
	
	public Map<String, Character> getEncodingScheme() {
		return this.encodingScheme;
	}
	
	public Map<Character, String> getDecodingScheme() {
		return this.decodingScheme;
	}
	
	public void setDecodingScheme(Map<Character, String> decodingScheme) {
		 this.decodingScheme = decodingScheme;
	}
	
	public void setEncodingScheme(Map<String, Character> encodingScheme) {
		 this.encodingScheme = encodingScheme;
	}
	
	public void setConfidenceMinimum(double confidenceMinimum) {
		this.confidenceMinimum = confidenceMinimum;
	}
	
	public double getDeterminismMinimum() {
		return determinismMinimum;
	}
	
	public void setDeterminismMinimum(double determinismMinimum) {
		this.determinismMinimum = determinismMinimum;
	}
	
	public double getCoverageMinimum() {
		return coverageMinimum;
	}
	
	public void setCoverageMinimum(double coverageMinimum) {
		this.coverageMinimum = coverageMinimum;
	}
	
	public double getSupportWeight() {
		return supportWeight;
	}
	
	public void setSupportWeight(double frequencyWeight) {
		this.supportWeight = frequencyWeight;
	}
	
	public double getLanguageFitWeight() {
		return languageFitWeight;
	}
	
	public void setLanguageFitWeight(double languageRatioWeight) {
		this.languageFitWeight = languageRatioWeight;
	}
	
	public double getConfidenceWeight() {
		return confidenceWeight;
	}
	
	public void setConfidenceWeight(double confidenceWeight) {
		this.confidenceWeight = confidenceWeight;
	}
	
	public double getCoverageWeight() {
		return coverageWeight;
	}
	
	public void setCoverageWeight(double coverageWeight) {
		this.coverageWeight = coverageWeight;
	}
	
	public double getDeterminismWeight() {
		return determinismWeight;
	}
	
	public void setDeterminismWeight(double determinismWeight) {
		this.determinismWeight = determinismWeight;
	}
	
	public double getTotalWeight(){
		return supportWeight + languageFitWeight + confidenceWeight + coverageWeight + determinismWeight + avgNumFiringsWeight + numTransitionsWeight;
	}
	
	public XLog getDiscoveryLog() {
		return discoveryLog;
	}
	
	public void setDiscoveryLog(XLog discoveryLog) {
		this.discoveryLog = discoveryLog;
	}
	
	public void setSmartParameterDefaultsForLog(XLog log){
		// TODO: let user select classifier
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, new XEventNameClassifier());
		ArrayList<XEventClass> eventClasses = sortEventClassesByOccurrence(logInfo.getEventClasses());
		this.logHasTime = logInfo.getEventAttributeInfo().getAttributeKeys().contains("time:timestamp");

		// set smart defaults for log
		if(eventClasses.size()>14)
			projectionMethod = ProjectionMethods.Markov;
		else
			projectionMethod = ProjectionMethods.None;
		if(eventClasses.size()>0){
			this.maxActivityFrequencyInLog = eventClasses.get(0).size();
			int medianFrequency = eventClasses.get((int)Math.ceil( ((double)(eventClasses.size()-1)) /2 )).size();
			this.frequencyMinimum = (int) Math.sqrt(medianFrequency); // arbitrary heuristic for default pruning parameter, explore smarter/better heuristics
		}else{
			this.maxActivityFrequencyInLog = 0;
			this.frequencyMinimum = 0;
		}
	}
	
	public void setSmartParameterDefaultsForEvaluationLog(){
		setSmartParameterDefaultsForLog(getEvaluationLog());
	}
	
	public void setSmartParameterDefaultsForDiscoveryLog(){
		setSmartParameterDefaultsForLog(getDiscoveryLog());
	}
	
	public XLog getEvaluationLog() {
		return evaluationLog;
	}
	
	public void setEvaluationLog(XLog evaluationLog) {
		this.evaluationLog = evaluationLog;
	}
	
	public double getAvgNumFiringsWeight() {
		return avgNumFiringsWeight;
	}
	
	public void setAvgNumFiringsWeight(double avgNumFiringsWeight) {
		this.avgNumFiringsWeight = avgNumFiringsWeight;
	}
	
	public double getNumTransitionsWeight() {
		return numTransitionsWeight;
	}
	
	public void setNumTransitionsWeight(double numTransitionsWeight) {
		this.numTransitionsWeight = numTransitionsWeight;
	}

	public int getAlignmentMaxNumStatesPerTransition() {
		return alignmentMaxNumStatesPerTransition;
	}

	public void setAlignmentMaxNumStatesPerTransition(int alignment_max_num_states) {
		this.alignmentMaxNumStatesPerTransition = alignment_max_num_states;
	}

	public boolean isUseSeq() {
		return useSeq;
	}

	public void setUseSeq(boolean useSeq) {
		this.useSeq = useSeq;
	}

	public boolean isUseAnd() {
		return useAnd;
	}

	public void setUseAnd(boolean useAnd) {
		this.useAnd = useAnd;
	}

	public boolean isUseOr() {
		return useOr;
	}

	public void setUseOr(boolean useOr) {
		this.useOr = useOr;
	}

	public boolean isUseXor() {
		return useXor;
	}

	public void setUseXor(boolean useXor) {
		this.useXor = useXor;
	}

	public boolean isUseXorloop() {
		return useXorloop;
	}

	public void setUseXorloop(boolean useXorloop) {
		this.useXorloop = useXorloop;
	}

	public boolean isReturnMurataOrdered() {
		return returnMurataOrdered;
	}

	public void setReturnMurataOrdered(boolean returnMurataOrdered) {
		this.returnMurataOrdered = returnMurataOrdered;
	}

	public ProjectionMethods getProjectionMethod() {
		return projectionMethod;
	}

	public void setProjectionMethod(ProjectionMethods projectionMethod) {
		this.projectionMethod = projectionMethod;
	}

	public int getMaxActivityFrequencyInLog() {
		return maxActivityFrequencyInLog;
	}

	public void setMaxActivityFrequencyInLog(int maxActivityFrequencyInLog) {
		this.maxActivityFrequencyInLog = maxActivityFrequencyInLog;
	}
	
	/**
	 * Sorts the given event classes from high to low occurrence.
	 * 
	 * @param eventClasses
	 *            The given event classes.
	 * @return The sorted event classes.
	 */
	private ArrayList<XEventClass> sortEventClassesByOccurrence(XEventClasses eventClasses) {
		ArrayList<XEventClass> sortedEvents = new ArrayList<XEventClass>();
		for (XEventClass event : eventClasses.getClasses()) {
			boolean inserted = false;
			XEventClass current = null;
			for (int i = 0; i < sortedEvents.size(); i++) {
				current = sortedEvents.get(i);
				if (current.size() <= event.size()) {
					// insert at correct position and set marker
					sortedEvents.add(i, event);
					inserted = true;
					break;
				}
			}
			if (inserted == false) {
				// append to end of list
				sortedEvents.add(event);
			}
		}
		return sortedEvents;
	}

	public boolean isUseEfficientLog() {
		return useEfficientLog;
	}

	public void setUseEfficientLog(boolean useEfficientLog) {
		this.useEfficientLog = useEfficientLog;
	}

	public EfficientLogExtractor getEfficientLogExtractor() {
		return efficientLogExtractor;
	}

	public void setEfficientLogExtractor(EfficientLogExtractor extractor) {
		this.efficientLogExtractor = extractor;
	}

	public int getEfficientLogCacheSize() {
		return efficientLogCacheSize;
	}

	public void setEfficientLogCacheSize(int efficientLogCacheSize) {
		this.efficientLogCacheSize = efficientLogCacheSize;
	}

	public Set<String> getAttributesToKeep() {
		return attributesToKeep;
	}

	public void setAttributesToKeep(Set<String> attributesToKeep) {
		this.attributesToKeep = attributesToKeep;
	}

	public AtomicInteger getNumberOfExploredLpms() {
		return numberOfExploredLpms;
	}

	public void notifyNumExplored(){
	    setChanged();
	    notifyObservers(numberOfExploredLpms.get());
	}
	
	public void setNumberOfExploredLpms(AtomicInteger numberOfExploredLpms) {
		this.numberOfExploredLpms = numberOfExploredLpms;
	    setChanged();
	    notifyObservers(numberOfExploredLpms.get());
	}
	
	public int getMax_consecutive_nonfitting() {
		return max_consecutive_nonfitting;
	}

	public void setMax_consecutive_nonfitting(int max_consecutive_nonfitting) {
		this.max_consecutive_nonfitting = max_consecutive_nonfitting;
		
		if(efficientLogExtractor instanceof GapAndTimeBasedExtractor2){
			((GapAndTimeBasedExtractor2) efficientLogExtractor).setMax_consecutive_nonfitting(getMax_consecutive_nonfitting());
		}else{
			if(efficientLogExtractor instanceof GapAndTimeBasedExtractor){
				((GapAndTimeBasedExtractor) efficientLogExtractor).setMax_consecutive_nonfitting(getMax_consecutive_nonfitting());
			}else{
				if(efficientLogExtractor instanceof EventAndTimeGapExtractor){
					((EventAndTimeGapExtractor) efficientLogExtractor).setMax_consecutive_nonfitting(getMax_consecutive_nonfitting());
				}else{
					if(efficientLogExtractor instanceof EventGapExtractor){
						((EventGapExtractor) efficientLogExtractor).setMax_consecutive_nonfitting(getMax_consecutive_nonfitting());
					}
				}
			}
		}
	}

	public int getMax_total_nonfitting() {
		return max_total_nonfitting;
	}

	public void setMax_total_nonfitting(int max_total_nonfitting) {
		this.max_total_nonfitting = max_total_nonfitting;
		
		if(efficientLogExtractor instanceof GapAndTimeBasedExtractor2){
			((GapAndTimeBasedExtractor2) efficientLogExtractor).setMax_total_nonfitting(getMax_total_nonfitting());
		}else{
			if(efficientLogExtractor instanceof GapAndTimeBasedExtractor){
				((GapAndTimeBasedExtractor) efficientLogExtractor).setMax_total_nonfitting(getMax_total_nonfitting());
			}
		}
	}

	public long getMax_consecutive_timedif_millis() {
		return max_consecutive_timedif_millis;
	}

	public void setMax_consecutive_timedif_millis(long max_consecutive_timedif_millis) {
		this.max_consecutive_timedif_millis = max_consecutive_timedif_millis;
		
		if(efficientLogExtractor instanceof GapAndTimeBasedExtractor2){
			((GapAndTimeBasedExtractor2) efficientLogExtractor).setMax_consecutive_timedif_millis(getMax_consecutive_timedif_millis());
		}else{
			if(efficientLogExtractor instanceof GapAndTimeBasedExtractor){
				((GapAndTimeBasedExtractor) efficientLogExtractor).setMax_consecutive_timedif_millis(getMax_consecutive_timedif_millis());
			}else{
				if(efficientLogExtractor instanceof EventAndTimeGapExtractor){
					((EventAndTimeGapExtractor) efficientLogExtractor).setMax_consecutive_timedif_millis(getMax_consecutive_timedif_millis());
				}else{
					if(efficientLogExtractor instanceof TimeGapExtractor){
						((EventGapExtractor) efficientLogExtractor).setMax_consecutive_timedif_millis(getMax_consecutive_timedif_millis());
					}
				}
			}
		}
	}

	public long getMax_total_timedif_millis() {
		return max_total_timedif_millis;
	}

	public void setMax_total_timedif_millis(long max_total_timedif_millis) {
		this.max_total_timedif_millis = max_total_timedif_millis;
		
		if(efficientLogExtractor instanceof GapAndTimeBasedExtractor2){
			((GapAndTimeBasedExtractor2) efficientLogExtractor).setMax_total_timedif_millis(getMax_total_timedif_millis());
		}else{
			if(efficientLogExtractor instanceof GapAndTimeBasedExtractor){
				((GapAndTimeBasedExtractor) efficientLogExtractor).setMax_total_timedif_millis(getMax_total_timedif_millis());
			}
		}
	}

	public boolean isStoreProcessTree() {
		return storeProcessTree;
	}

	public void setStoreProcessTree(boolean storeProcessTree) {
		this.storeProcessTree = storeProcessTree;
	}

	public boolean logHasTime() {
		return logHasTime;
	}
	
	public boolean isVerbose(){
		return isVerbose;
	}
	
	public void setVerbose(boolean verbose){
		isVerbose = verbose;
	}
	
	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
	
	public Long getLastNotificationTime() {
		return lastNotificationTime;
	}

	public void setLastNotificationTime(Long lastNotificationTime) {
		this.lastNotificationTime = lastNotificationTime;
	}
	
	public int getNotificationPeriod() {
		return notificationPeriod;
	}

	public void setNotificationPeriod(int notificationPeriod) {
		this.notificationPeriod = notificationPeriod;
	}
}