package org.processmining.lpm.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


public class UtilityAlignmentScoredAcceptingPetrinetContainer implements Iterable<UtilityLocalProcessModel>{
			
	private int maxSize;
	
	private Set<UtilityLocalProcessModel> inner;
	
	public UtilityAlignmentScoredAcceptingPetrinetContainer(int maxSize){
		//System.out.println("Made a container of size: "+maxSize);
		inner = Collections.synchronizedSet(new BoundedTreeSet<UtilityLocalProcessModel>(maxSize));
		this.maxSize = maxSize;
	}
	
	public int getMaxSize(){
		return maxSize;
	}
	
	public void remove(final UtilityLocalProcessModel elemToRemove){
		if(elemToRemove!=null)
			inner.remove(elemToRemove);
	}
	
    public boolean add(final UtilityLocalProcessModel newElem) {
		return inner.add(newElem);
    }
    
    public boolean addMultiple(Collection<UtilityLocalProcessModel> c) {
    	boolean insertedOne = false;
    	for(UtilityLocalProcessModel sapn : c){
    		if(add(sapn))
    			insertedOne = true;
    	}
    	return insertedOne;
    }

	public Iterator<UtilityLocalProcessModel> iterator() {
		return inner.iterator();
	}

}
