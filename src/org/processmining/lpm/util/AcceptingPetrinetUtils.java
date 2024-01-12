package org.processmining.lpm.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public final class AcceptingPetrinetUtils {
	private AcceptingPetrinetUtils(){};
	
	public static AcceptingPetriNet decodeTransitionLabels(final AcceptingPetriNet apn, final Map<Character, String> eventDecoding){
		AcceptingPetriNet apn2 = new AcceptingPetriNetImpl(apn.getNet(), apn.getInitialMarking(), apn.getFinalMarkings());

		// WARNING: MODIFIES THE ORIGINAL NET!
		for(Transition t : apn2.getNet().getTransitions()){
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
			
		return apn2;
	}
	
	public static Set<List<String>> calculateLanguage(final AcceptingPetriNet apn, final int maxLoop, final boolean onlyAccepting){
		Multiset<Place> marking = HashMultiset.create();
		for(Place p : apn.getInitialMarking()){
			marking.add(p);
		}
		Set<List<String>> language = new HashSet<List<String>>();
		if(!onlyAccepting || apn.getFinalMarkings().contains(marking))
			language.add(new LinkedList<String>());
		return recurseOptions(apn.getNet(), marking, apn.getFinalMarkings(), new LinkedList<String>(), language, new HashMap<Transition, Integer>(), maxLoop, onlyAccepting);
	}
	
	private static Set<List<String>> recurseOptions(final Petrinet ptnet, final Multiset<Place> marking, final Set<Marking> fMarkings, final List<String> currentTrace, final Set<List<String>> language, final Map<Transition, Integer> firingsPerTransition, final int maxLoop, final boolean onlyAccepting){
		Collection<Transition> netTransitions = ptnet.getTransitions();
		
		// test stopping criteria
		boolean exceededFiringThreshold = false;
		for(Integer firings : firingsPerTransition.values())
			if(firings>(maxLoop+1)){
				exceededFiringThreshold = true;
				break;
			}
		if(exceededFiringThreshold)
			return new HashSet<List<String>>();
		
		// identify enabled transitions
		Set<Transition> enabledTransitions = new HashSet<Transition>();
		for(Transition t : netTransitions){
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inflow = ptnet.getInEdges(t);
			boolean enabled = true;
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inflow){
				if(!marking.contains((edge.getSource()))){
					enabled = false;
					break;
				}
			}
			if(enabled)
				enabledTransitions.add(t);
		}
		if(enabledTransitions.isEmpty())
			return language;
		else{
			for(Transition enabledTransition : enabledTransitions){
				String label = enabledTransition.getLabel();
				Multiset<Place> newMarking = HashMultiset.create();
				newMarking.addAll(marking);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inflow = ptnet.getInEdges(enabledTransition);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outflow = ptnet.getOutEdges(enabledTransition);
				for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inflow)
					newMarking.remove(edge.getSource());
				for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outflow)
					newMarking.add((Place) edge.getTarget());
				List<String> newCurrentTrace = new LinkedList<String>(currentTrace);
				if(!enabledTransition.isInvisible())
					newCurrentTrace.add(label);
				Set<List<String>> newLanguage = new HashSet<List<String>>(language);
				if(!onlyAccepting || fMarkings.contains(new Marking(newMarking)))
					newLanguage.add(newCurrentTrace);
				Integer currentFirings = firingsPerTransition.get(enabledTransition);
				if(currentFirings==null)
					currentFirings=0;
				currentFirings++;
				Map<Transition, Integer> newFiringsPerTransition = new HashMap<Transition, Integer>(firingsPerTransition);
				newFiringsPerTransition.put(enabledTransition, currentFirings);
				language.addAll(recurseOptions(ptnet, newMarking, fMarkings, newCurrentTrace, newLanguage, newFiringsPerTransition, maxLoop, onlyAccepting));
			}
		}
		return language;
	}
	
	public static Set<String> calculateStartingActivities(final AcceptingPetriNet apn, final int maxLoop){
		Multiset<Place> marking = HashMultiset.create();
		for(Place p : apn.getInitialMarking()){
			marking.add(p);
		}
		return recurseOptions(apn.getNet(), marking, new HashSet<String>(), new HashMap<Transition, Integer>(), maxLoop);
	}
	
	private static Set<String> recurseOptions(final Petrinet ptnet, final Multiset<Place> marking, final Set<String> currentStartActivities, final Map<Transition, Integer> firingsPerTransition, final int maxLoop){
		Collection<Transition> netTransitions = ptnet.getTransitions();
		
		// test stopping criteria
		boolean exceededFiringThreshold = false;
		for(Integer firings : firingsPerTransition.values())
			if(firings>(maxLoop+1)){
				exceededFiringThreshold = true;
				break;
			}
		if(exceededFiringThreshold)
			return new HashSet<String>();
		
		// identify enabled transitions
		Set<Transition> enabledTransitions = new HashSet<Transition>();
		for(Transition t : netTransitions){
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inflow = ptnet.getInEdges(t);
			boolean enabled = true;
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inflow){
				if(!marking.contains((edge.getSource()))){
					enabled = false;
					break;
				}
			}
			if(enabled)
				enabledTransitions.add(t);
		}
		Set<String> newStartActivities = new HashSet<String>(currentStartActivities);
		if(enabledTransitions.isEmpty())
			return currentStartActivities;
		else{
			for(Transition enabledTransition : enabledTransitions){
				String label = enabledTransition.getLabel();
				Multiset<Place> newMarking = HashMultiset.create();
				newMarking.addAll(marking);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inflow = ptnet.getInEdges(enabledTransition);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outflow = ptnet.getOutEdges(enabledTransition);
				for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inflow)
					newMarking.remove(edge.getSource());
				for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outflow)
					newMarking.add((Place) edge.getTarget());
				Integer currentFirings = firingsPerTransition.get(enabledTransition);
				if(currentFirings==null)
					currentFirings=0;
				currentFirings++;
				Map<Transition, Integer> newFiringsPerTransition = new HashMap<Transition, Integer>(firingsPerTransition);
				newFiringsPerTransition.put(enabledTransition, currentFirings);
				if(!enabledTransition.isInvisible())
					newStartActivities.add(label);
				else
					newStartActivities.addAll(recurseOptions(ptnet, newMarking, newStartActivities, newFiringsPerTransition, maxLoop));
			}
		}
		return newStartActivities;
	}
		
	public static String prettyPrintLanguage(final Set<List<String>> language){
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<body>");
		for(List<String> trace : language){
			sb.append("<p>"+trace+"</p>");
			sb.append("</br>");
		}
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}

}
