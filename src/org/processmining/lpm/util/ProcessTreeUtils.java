package org.processmining.lpm.util;

import java.util.Map;

import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Task;

public final class ProcessTreeUtils {
	private ProcessTreeUtils(){};
	
	public static ProcessTree decodeTransitionLabels(final ProcessTree pt, final Map<Character, String> eventDecoding){
		for(Node node : pt.getNodes()){
			if(node instanceof Task.Manual){
				Task.Manual manualNode = (Task.Manual) node;
				manualNode.setName(eventDecoding.get((manualNode.getName().charAt(0))));
			}
		}
			
		return pt;
	}
}