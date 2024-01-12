package org.processmining.lpm.projection.util;

public class LPNorm implements VectorNorm {
	private int p;
	
	protected LPNorm(int p){
		this.p = p;
	}
	
	public double aggregate(double[] vector){
		double total = 0d;
		for(double d : vector){
			if(p!=Integer.MIN_VALUE)
				total += Math.pow(d, p);
			else
				total = Math.max(total, d);
		}
		return total;
	}
}
