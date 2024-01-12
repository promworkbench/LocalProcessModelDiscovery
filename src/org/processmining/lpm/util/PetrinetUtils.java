package org.processmining.lpm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public final class PetrinetUtils {
	private PetrinetUtils(){};
	
	public static Petrinet decodeTransitionLabels(final Petrinet net, final Map<Character, String> eventDecoding){
		Petrinet net2 = PetrinetFactory.clonePetrinet(net);
		
		for(Transition t : net2.getTransitions()){
			t.getLabel();
			if(!t.isInvisible()){
				t.getLabel();
				AttributeMap am = t.getAttributeMap();
				Object o = am.get(AttributeMap.LABEL);
				String s = ((String) o).toString();
				Character a = s.charAt(0);
				String replaceString = eventDecoding.get(a);
				if(replaceString==null)
					System.err.println("char 'a' not found in "+eventDecoding);
				am.remove(AttributeMap.LABEL);
				am.put(AttributeMap.LABEL, replaceString);
			}
		}
		return net2;
	}
	
	public static int getNumNonSilentTransitions(final Petrinet net){
		int num = 0;
		for(Transition t : net.getTransitions())
			if(!t.isInvisible())
				num++;
		return num;
	}
	
	public static String prettyPrintPetrinet(final Petrinet net){
		StringBuilder sb = new StringBuilder();
		sb.append("Transitions: ");
		sb.append(net.getTransitions());
		sb.append('\n');
		sb.append("Places:      ");
		sb.append(net.getPlaces());
		sb.append('\n');
		sb.append("Edges:        [");
		for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getEdges()){
			sb.append(e.getSource());
			sb.append(" -> ");
			sb.append(e.getTarget());
			sb.append(',');
		}
		sb.append('\n');
		return sb.toString();
	}
	
	public static Multiset<Place> getMarkingAfterFiring(final Petrinet net, final Multiset<Place> markingBefore, final Transition toFire){
		Multiset<Place> markingAfter = HashMultiset.create();
		markingAfter.addAll(markingBefore);
		if(isTransitionEnabled(net, markingBefore, toFire)){
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getInEdges(toFire)){
				markingAfter.remove(e.getSource());
			}
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getOutEdges(toFire))
				markingAfter.add((Place) e.getTarget());
		}else{
			System.err.println("The provided transition "+toFire+" is  not enabled in given Petri net from given marking "+markingBefore+", marking allows "+getEnabledTransitions(net, markingBefore)+", net: "+prettyPrintPetrinet(net));
		}
		return markingAfter;
	}
	
	public static Set<Place> getMarkingAfterFiring(final Petrinet net, final Set<Place> markingBefore, final Transition toFire){
		Set<Place> markingAfter = new HashSet<Place>();
		markingAfter.addAll(markingBefore);
		if(isTransitionEnabled(net, markingBefore, toFire)){
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getInEdges(toFire))
				markingAfter.remove(e.getSource());
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getOutEdges(toFire))
				markingAfter.add((Place) e.getTarget());
		}else{
			System.err.println("The provided transition "+toFire+" is  not enabled in given Petri net from given marking "+markingBefore+", marking allows "+getEnabledTransitions(net, markingBefore)+", net: "+prettyPrintPetrinet(net));
		}
		return markingAfter;
	}
	
	public static boolean isTransitionEnabled(final Petrinet net, final Multiset<Place> marking, final Transition toFire){
		if(!net.getTransitions().contains(toFire))
			return false;
		for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getInEdges(toFire)){
			Place sourcePlace = (Place) e.getSource();
			if(!marking.contains(sourcePlace))
				return false;
		}
		return true;
	}
	
	public static boolean isTransitionEnabled(final Petrinet net, final Set<Place> marking, final Transition toFire){
		if(!net.getTransitions().contains(toFire))
			return false;
		for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : net.getInEdges(toFire)){
			Place sourcePlace = (Place) e.getSource();
			if(!marking.contains(sourcePlace))
				return false;
		}
		return true;
	}
	
	public static Set<Transition> getEnabledNonSilentTransitions(final Petrinet net, final Multiset<Place> marking){
		Set<Transition> enabledTransitions = new HashSet<Transition>();
		for(Transition t : net.getTransitions()){
			if(!t.isInvisible()&&isTransitionEnabled(net, marking, t))
				enabledTransitions.add(t);
		}
		return enabledTransitions;
	}
	
	public static Set<Transition> getEnabledTransitions(final Petrinet net, final Multiset<Place> marking){
		Set<Transition> enabledTransitions = new HashSet<Transition>();
		for(Transition t : net.getTransitions()){
			if(isTransitionEnabled(net, marking, t))
				enabledTransitions.add(t);
		}
		return enabledTransitions;
	}
	
	public static Set<Transition> getEnabledTransitions(final Petrinet net, final Set<Place> marking){
		Set<Transition> enabledTransitions = new HashSet<Transition>();
		for(Transition t : net.getTransitions()){
			if(isTransitionEnabled(net, marking, t))
				enabledTransitions.add(t);
		}
		return enabledTransitions;
	}
	
	// dummy event class (for backloop transitions)
	public static final XEventClass BACKLOOP = new XEventClass("BACKLOOP", -2) {
		public boolean equals(Object o) {
			return this == o;
		}

		public int hashCode() {
			return System.identityHashCode(this);
		}		
	};
	
	// dummy event class (for unmapped transitions)
	public static final XEventClass DUMMY = new XEventClass("DUMMY", -1) {
		public boolean equals(Object o) {
			return this == o;
		}

		public int hashCode() {
			return System.identityHashCode(this);
		}		
	};
	
	
	public static TransEvClassMapping transitionToEventClassMapperByLabel(final XLog log, final Petrinet net) {
		TransEvClassMapping map = new TransEvClassMapping(new XEventNameClassifier(), DUMMY);

		List<Transition> listTrans = new ArrayList<Transition>(net.getTransitions());
		Map<String, XEventClass> classes = new HashMap<String, XEventClass>();
		int index = 0;
		for (Transition transition : listTrans) {
			if (transition.isInvisible()) {
				map.put(transition, DUMMY);
			} else {
				XEventClass eClass = null;
				if(classes.containsKey(transition.getLabel()))
					eClass = classes.get(transition.getLabel());
				else{
					eClass = new XEventClass(transition.getLabel(), index);
					classes.put(transition.getLabel(), eClass);
					index++;
				}
				map.put(transition, eClass);
			}
		}
		return map;
	}
	
	public static TransEvClassMapping transitionToEventClassMapperByLabelWithBackloop(final XLog log, final Petrinet net, final Transition backloop) {
		TransEvClassMapping map = new TransEvClassMapping(new XEventNameClassifier(), DUMMY);

		List<Transition> listTrans = new ArrayList<Transition>(net.getTransitions());
		Map<String, XEventClass> classes = new HashMap<String, XEventClass>();
		int index = 0;
		for (Transition transition : listTrans) {
			if (transition.isInvisible()) {
				if(transition.equals(backloop))
					map.put(transition, BACKLOOP);
				else
					map.put(transition, DUMMY);
			} else {
				XEventClass eClass = null;
				if(classes.containsKey(transition.getLabel()))
					eClass = classes.get(transition.getLabel());
				else{
					eClass = new XEventClass(transition.getLabel(), index);
					classes.put(transition.getLabel(), eClass);
					index++;
				}
				map.put(transition, eClass);
			}
		}
		return map;
	}
	
	public static Set<String> calculateAlphabet(final Petrinet net){
		Set<String> alphabet = new HashSet<String>();
		for(Transition t : net.getTransitions())
			if(!t.isInvisible())
				alphabet.add(t.getLabel());
		return alphabet;
	}
	
	/*
	 * return Object[] with:
	 * 	index 0: log projected on petri net transitions
	 *  index 1: ratio of events remaining after projection
	 *  index 2: length of longest trace after projection
	 */
	public static Object[] projectLogOnNetAlphabet(final XLog log, final Petrinet net){
		Set<String> alphabet = calculateAlphabet(net);
		return LogUtils.projectLogOnActivitySet(log, alphabet);
	}
}
