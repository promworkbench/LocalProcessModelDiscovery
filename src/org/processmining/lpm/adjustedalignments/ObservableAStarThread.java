package org.processmining.lpm.adjustedalignments;

import nl.tue.astar.AStarObserver;
import nl.tue.astar.Head;
import nl.tue.astar.Tail;

public interface ObservableAStarThread<H extends Head, T extends Tail> extends AStarThread<H, T> {

	/**
	 * constant which is used to signal that an estimate is irrelevant. This is
	 * set to the estimated remaining cost of a record, if the record was
	 * already considered and is reached again with higher cost.
	 */
	public static final double ESTIMATEIRRELEVANT = -1;

	public void addObserver(AStarObserver observer);

	public void removeObserver(AStarObserver observer);

}
