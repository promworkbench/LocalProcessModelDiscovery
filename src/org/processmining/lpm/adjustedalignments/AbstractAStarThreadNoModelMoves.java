package org.processmining.lpm.adjustedalignments;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import nl.tue.astar.AStarException;
import nl.tue.astar.AStarObserver;
import nl.tue.astar.Delegate;
import nl.tue.astar.Head;
import nl.tue.astar.Record;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.State;
import nl.tue.astar.util.BreadthFirstFastLookupPriorityQueue;
import nl.tue.astar.util.DepthFirstFastLookupPriorityQueue;
import nl.tue.astar.util.FastLookupPriorityQueue;
import nl.tue.astar.util.RandomFastLookupPriorityQueue;

public abstract class AbstractAStarThreadNoModelMoves<H extends Head, T extends Tail> implements ObservableAStarThread<H, T> {

	public static enum QueueingModel {
		DEPTHFIRST, BREADTHFIRST, RANDOM
	};

	/**
	 * The storageHandler handles the storing and retrieving of elements from
	 * the statespace searched by the AStar algorithm
	 * 
	 * @author bfvdonge
	 * 
	 * @param <H>
	 * @param <T>
	 */
	public static interface StorageHandler<H extends Head, T extends Tail> {

		/**
		 * return the estimate of the tail for the given head. The head is
		 * stored at the given index.
		 * 
		 * @param head
		 * @param index
		 * @return
		 * @throws AStarException
		 */
		public void storeStateForRecord(State<H, T> state, Record newRec) throws AStarException;

		public long getIndexOf(H head) throws AStarException;

		public State<H, T> getStoredState(Record rec) throws AStarException;
	}

	protected FastLookupPriorityQueue queue;
	protected final Trace trace;
	protected final int maxStates;
	protected int queuedStateCount = 0;
	protected int traversedArcCount = 0;
	protected int computedEstimateCount = 0;
	protected final Delegate<H, T> delegate;
	protected final AbstractPDelegate<T> pDelegate;
	protected int poll = 0;

	protected static int i = 0;
	protected ASynchronousMoveSorting sorting;
	protected boolean reliable;

	protected final TLongSet considered;

	protected List<AStarObserver> observers = new ArrayList<AStarObserver>(4);
	protected final StorageHandler<H, T> storageHandler;
	protected Type type = Type.PLAIN;
	protected double epsilon = 0;
	protected double expectedLength = 10.;

	/**
	 * any implementation should, after calling this constructor, call
	 * initializeQueue(initialHead);
	 * 
	 * @param delegate
	 * @param trace
	 * @param maxStates
	 * @param storageHandler
	 */
	@SuppressWarnings("unchecked")
	public AbstractAStarThreadNoModelMoves(Delegate<H, T> delegate, Trace trace, int maxStates, StorageHandler<H, T> storageHandler) {
		this.delegate = delegate;
		this.trace = trace;
		this.maxStates = maxStates;
		this.storageHandler = storageHandler;
		this.queue = new BreadthFirstFastLookupPriorityQueue(1000); // TODO: check effect of tuning this
		this.sorting = ASynchronousMoveSorting.LOGMOVEFIRST;
		this.considered = new TLongHashSet(1000, 0.5f, -2l); // TODO: check effect of tuning this
		this.pDelegate = (AbstractPDelegate<T>) delegate;
	}

	public Trace getTrace() {
		return trace;
	}

	public Delegate<H, T> getDelegate() {
		return delegate;
	}

	public void setQueueingModel(QueueingModel model) {
		switch (model) {
		case BREADTHFIRST:
			setQueue(new BreadthFirstFastLookupPriorityQueue(1000));
			break;
		case DEPTHFIRST:
			setQueue(new DepthFirstFastLookupPriorityQueue(1000));
			break;
		case RANDOM:
			setQueue(new RandomFastLookupPriorityQueue(1000, 0.5));
			break;
		}
	}

	public void setQueue(FastLookupPriorityQueue newQueue) {
		if (newQueue.size() > 1) 
			throw new UnsupportedOperationException("Cannot change the queue after elements have been inserted.");
		FastLookupPriorityQueue oldQueue = this.queue;

		this.queue = newQueue;

		if (oldQueue.size() == 1) {
			Record d = oldQueue.peek();
			newQueue.add(d);
		}
	}

	public void addObserver(AStarObserver observer) {
		observers.add(observer);
		observer.initialNodeCreated(queue.peek());
	}

	public void removeObserver(AStarObserver observer) {
		observers.remove(observer);
	}

	public boolean getPreferBreadth() {
		return this.queue instanceof BreadthFirstFastLookupPriorityQueue;
	}

	public void setASynchronousMoveSorting(ASynchronousMoveSorting sorting) {
		this.sorting = sorting;
	}

	public ASynchronousMoveSorting getSorting() {
		return sorting;
	}

	public Record getOptimalRecord(Canceller c) throws AStarException {
		return getOptimalRecord(c, Integer.MAX_VALUE, -1.0);
	}

	public Record getOptimalRecord(Canceller c, int stopAt) throws AStarException {
		return getOptimalRecord(c, stopAt, -1.0);
	}

	public Record getOptimalRecord(Canceller c, double timeLimit) throws AStarException {
		return getOptimalRecord(c, Integer.MAX_VALUE, timeLimit);
	}

	protected Record poll() {
		Record node = queue.poll();
		return node;
	}

	/**
	 * gets the optimal record. The search stops and returns the best prefix so
	 * far if either the stopAt value is reached in terms of cost, or if the
	 * timeLimit in seconds is reached. If the timelimit is negative, then time
	 * is unlimited.
	 * 
	 * @param c
	 * @param stopAt
	 * @param timeLimit
	 * @return
	 * @throws AStarException
	 */
	public Record getOptimalRecord(final Canceller c, final int stopAt, final double timeLimit) throws AStarException {
		State<H, T> state;
		Record rec = null;
		H head = null;
		T tail = null;
		final long endTime = System.currentTimeMillis() + (int) (1000 * timeLimit);

		queue.setMaxCost(stopAt);

		while (!queue.isEmpty() && !c.isCancelled()
				&& (timeLimit < 0 || System.currentTimeMillis() < endTime)) {

			rec = poll();
			poll++;
			try {
				state = storageHandler.getStoredState(rec);
			} catch (Exception e) {
				throw new AStarException(e);
			}
			head = state.getHead();
			tail = state.getTail();

			if (head.isFinal(delegate)) {
				this.reliable = true;
				for (AStarObserver observer : observers) {
					observer.finalNodeFound(rec);
				}
				return rec;
			}

			if (poll >= maxStates || rec.getTotalCost() > stopAt
					|| (timeLimit > 0 && System.currentTimeMillis() > endTime)) {
				// unreliable, best guess:
				this.reliable = false;
				for (AStarObserver observer : observers) {
					observer.stoppedUnreliablyAt(rec);
				}
				return rec;
			}

			processMovesForRecord(rec, head, tail, stopAt, timeLimit, endTime);

		}

		this.reliable = false;
		for (AStarObserver observer : observers) {
			observer.stoppedUnreliablyAt(rec);
		}
		return rec;
	}

	protected void processMovesForRecord(Record rec, H head, T tail, int stopAt, double timeLimit, long endTime) throws AStarException {
		processMovesForRecordWithUpToDateTail(rec, head, tail, stopAt, timeLimit, endTime);
	}

	protected void processMovesForRecordWithUpToDateTail(Record rec, H head, T tail, int stopAt, double timeLimit, long endTime) throws AStarException {
		setConsidered(rec);

		// move model only
		TIntList enabled = head.getModelMoves(rec, delegate);

		TIntCollection nextEvents = rec.getNextEvents(delegate, trace);
		TIntIterator evtIt = nextEvents.iterator();
		int activity = NOMOVE;

		while (evtIt.hasNext()) {
			int nextEvent = evtIt.next();
			TIntList ml = null;

			// move both log and model synchronously;
			activity = trace.get(nextEvent);
			ml = head.getSynchronousMoves(rec, delegate, enabled, activity);
			TIntIterator it = ml.iterator();
			while (it.hasNext()) 
				processMove(head, tail, rec, it.next(), nextEvent, activity);

			// sorting == ASynchronousMoveSorting.LOGMOVEFIRST implies
			// logMove only after initial move, sync move or log move.
			if (isValidMoveOnLog(rec, nextEvent, activity, enabled, ml))
				processMove(head, tail, rec, NOMOVE, nextEvent, activity);
		}

		// sorting == ASynchronousMoveSorting.MODELMOVEFIRST implies
		// modelMove only after initial move, sync move or model move.
		if (isValidMoveOnModel(rec, nextEvents, activity, enabled)) {
			// allow move on model only if previous move was:
			// 1) the initial move (rec.getPredecessor() == null
			// 2) a synchronous move
			// 3) a move on model.
			TIntIterator it = enabled.iterator();
			while (it.hasNext()) {
				// move model
				int modelMove = it.next();
				if(!pDelegate.getTransition((short) modelMove).isInvisible())
					continue;
				processMove(head, tail, rec, modelMove, NOMOVE, NOMOVE);
			}
		}

	}

	protected void setConsidered(Record record) {
		considered.add(record.getState());
		for (AStarObserver observer : observers) {
			observer.nodeVisited(record);
		}
	}

	protected boolean isValidMoveOnModel(Record rec, TIntCollection nextEvents, int activity, TIntList modelMoves) {
		return sorting != ASynchronousMoveSorting.MODELMOVEFIRST || (rec.getPredecessor() == null || rec.getModelMove() != NOMOVE);
	}

	protected boolean isValidMoveOnLog(Record rec, int nextEvent, int activity, TIntList modelMoves, TIntList syncMoves) {
		return sorting != ASynchronousMoveSorting.LOGMOVEFIRST || (rec.getPredecessor() == null || rec.getMovedEvent() != NOMOVE);
	}

	public boolean wasReliable() {
		return reliable;
	}

	protected void processMove(H head, T tail, Record rec, int modelMove, int movedEvent, int activity) throws AStarException {
		// First, construct the next head from the old head
		final H newHead = computeNextHead(rec, head, modelMove, movedEvent, activity);
		final long index;
		try {
			index = storageHandler.getIndexOf(newHead);
		} catch (Exception e) {
			throw new AStarException(e);
		}

		// create a record for this new head
		final Record newRec = rec.getNextRecord(delegate, trace, newHead, index, modelMove, movedEvent, activity);

		Record r = queue.contains(newRec);
		if (r != null && r.getCostSoFar() <= newRec.getCostSoFar()) {

			// Either we visisted this state before,
			// or a record with the same state and lower (or equal) cost so far
			// exists.
			// this implies that newState was already fully present in the
			// statespace

			assert index >= 0;

			// edge from oldRec to newRec traversed.
			for (AStarObserver observer : observers) 
				observer.edgeTraversed(rec, newRec);

			return;
		}

		final T newTail;
		if (index >= 0) {
			if (newRec.getTotalCost() > queue.getMaxCost()) {
				// new record has guaranteed higher cost than the queue's
				// maxcost,
				// this state needs no further investigation.
				// However, it cannot be marked as considered, as it may be
				// reached again
				// through a shorter path.
			} else if (queue.add(newRec)) 
				queuedStateCount++;

			assert newRec.getState() == index;

			// edge from oldRec to newRec traversed.
			for (AStarObserver observer : observers) 
				observer.edgeTraversed(rec, newRec);
			
			return;

		}

		// the statespace doesn't contain a corresponding state, hence we need
		// to compute the tail.
		newTail = computeNewTail(newRec, tail, newHead, modelMove, movedEvent,
				activity);

		if (!newTail.canComplete()) {
			return;
		}

		// Check if the head is in the store and add if it isn't.
		final State<H, T> newState = new State<H, T>(newHead, newTail);

		try {
			storageHandler.storeStateForRecord(newState, newRec);
		} catch (Exception e) {
			throw new AStarException(e);
		}

		// State<H, T> ret = store.getObject(r.index);
		// if (!ret.equals(newState)) {
		// System.err.println("Retrieval error");
		// }

		// assert (r.isNew);
		if (newRec.getTotalCost() > queue.getMaxCost()) {
			// new record has guaranteed higher cost than the queue's
			// maxcost,
			// this state needs no further investigation.
			// However, it cannot be marked as considered, as it may be
			// reached again
			// through a shorter path.
		} else if (queue.add(newRec)) {
			queuedStateCount++;
		}

		// edge from oldRec to newRec traversed.
		for (AStarObserver observer : observers) 
			observer.edgeTraversed(rec, newRec);
	}

	@SuppressWarnings("unchecked")
	protected H computeNextHead(Record rec, H head, int modelMove, int movedEvent, int activity) {
		return (H) head.getNextHead(rec, delegate, modelMove, movedEvent, activity);
	}

	@SuppressWarnings("unchecked")
	protected T computeNewTail(Record newRec, T tail, H newHead, int modelMove, int movedEvent, int activity) {
		return (T) tail.getNextTail(delegate, newHead, modelMove, movedEvent, activity);
	}

	protected void initializeQueue(H head) throws AStarException {
		// time = System.currentTimeMillis();
		// First, find the location of head
		final long index = storageHandler.getIndexOf(head);
		// note that the contains method may give false negatives. However,
		// it is assumed to be more expensive to synchronize on (algorithm) than
		// to
		// just recompute the tail.

		// create a record for this new head
		final Record newRec = delegate.createInitialRecord(head, trace);

		if (index < 0) {
			// the statespace doesn't contain a corresponding state
			final T tail = delegate.createInitialTail(head);
			final State<H, T> newState = new State<H, T>(head, tail);

			storageHandler.storeStateForRecord(newState, newRec);

		} else {
			newRec.setState(index);
		}
		if (newRec.getTotalCost() > queue.getMaxCost()) {
			// new record has guaranteed higher cost than the queue's
			// maxcost,
			// this state needs no further investigation.
			setConsidered(newRec);
		} else if (queue.add(newRec)) {
			queuedStateCount++;
		}

	}

	public int getVisitedStateCount() {
		return poll;
	}

	public int getQueuedStateCount() {
		return queuedStateCount;
	}

	public String toString() {
		return queue.size() + ":" + queue.toString();
	}

	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * Returns the type of AStar used
	 * 
	 * @return
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Set epsilon for the weighted variants of A Star
	 * 
	 * @param epsilon
	 */
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;

	}
}