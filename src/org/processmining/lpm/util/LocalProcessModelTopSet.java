package org.processmining.lpm.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


public class LocalProcessModelTopSet implements Iterable<LocalProcessModel>{
			
	private int maxSize;
	
	private Set<LocalProcessModel> inner;
	
	public LocalProcessModelTopSet(int maxSize){
		//System.out.println("Made a container of size: "+maxSize);
		inner = Collections.synchronizedSet(new BoundedTreeSet<LocalProcessModel>(maxSize));
		this.maxSize = maxSize;
	}
	
	public int getMaxSize(){
		return maxSize;
	}
	
	public void remove(final LocalProcessModel elemToRemove){
		if(elemToRemove!=null)
			inner.remove(elemToRemove);
	}
	
    public boolean add(final LocalProcessModel newElem) {
		return inner.add(newElem);
    }
    
    public boolean addMultiple(Collection<LocalProcessModel> c) {
    	boolean insertedOne = false;
    	for(LocalProcessModel sapn : c){
    		if(add(sapn))
    			insertedOne = true;
    	}
    	return insertedOne;
    }

	public Iterator<LocalProcessModel> iterator() {
		return inner.iterator();
	}

}
