package org.processmining.lpm.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class BoundedTreeSet<E> extends TreeSet<E> {

	private static final long serialVersionUID = -4887586595099912355L;
	private final int limit;

    public BoundedTreeSet(final int limit) {
        super();
        this.limit = limit;
    }

    public BoundedTreeSet(final int limit, final Collection<? extends E> c) {
        super(c);
        this.limit = limit;
    }

    public BoundedTreeSet(final int limit, final Comparator<? super E> comparator) {
        super(comparator);
        this.limit = limit;
    }

    public BoundedTreeSet(final int limit, final SortedSet<E> s) {
        super(s);
        this.limit = limit;
    }

    @Override
    public boolean add(final E e) {
    	if(super.size()<limit)
    		return super.add(e);
    	else{
	    	if(!Comparable.class.isAssignableFrom(e.getClass()))
	    		return false;
	    	Comparable<E> ce = (Comparable<E>) e;
	    	boolean value = false;
	    	if(ce.compareTo((super.last()))<0){
	            super.remove(super.last());
	            value = super.add(e);
	    	}
	        return value;
    	}
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();//new NotImplementedException("addAll is not supported for BoundedTreeSet");
    }

}