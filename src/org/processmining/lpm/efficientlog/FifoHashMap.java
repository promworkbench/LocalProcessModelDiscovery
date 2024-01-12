package org.processmining.lpm.efficientlog;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class FifoHashMap<E,G> extends LinkedHashMap<E, G> {

    int max;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public FifoHashMap (int max){
        super(max + 1);
        this.max = max;

    }

    @Override
    public G put (E key, G value) {
        G forReturn =  super.put(key, value);
        if (super.size() > max){
            removeEldest();
        }

        return forReturn;
    }

    private void removeEldest() {
        Iterator <E> iterator = this.keySet().iterator();
        if (iterator.hasNext()){
            this.remove(iterator.next());
        }
    }

}