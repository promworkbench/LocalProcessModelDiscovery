package org.processmining.lpm.adjustedalignments;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import nl.tue.astar.AStarException;
import nl.tue.astar.Head;
import nl.tue.astar.Record;
import nl.tue.astar.Tail;
import nl.tue.astar.impl.State;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;
import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.StorageException;
import nl.tue.storage.impl.CompressedStoreHashSetImpl.Result;

public class MemoryEfficientStorageHandlerNoModelMoves<H extends Head, T extends Tail> implements AbstractAStarThreadNoModelMoves.StorageHandler<H, T> {

	protected final MemoryEfficientAStarAlgorithm<H, T> algorithm;
	protected final CompressedStore<State<H, T>> store;
	protected final CompressedHashSet<State<H, T>> statespace;
	protected final StorageAwareDelegate<H, T> delegate;

	protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public MemoryEfficientStorageHandlerNoModelMoves(MemoryEfficientAStarAlgorithm<H, T> algorithm) {
		this.algorithm = algorithm;
		this.statespace = algorithm.getStatespace();
		this.store = algorithm.getStore();
		this.delegate = algorithm.getDelegate();

	}

	public void storeStateForRecord(State<H, T> state, Record newRec) throws AStarException {
		final Result<State<H, T>> r;
		// synchronized (statespace) {
		try {
			lock.writeLock().lock();
			r = statespace.add(state);
		} catch (StorageException e) {
			throw new AStarException(e);
		} finally {
			lock.writeLock().unlock();
		}

		// }
		newRec.setState(r.index);
	}

	public long getIndexOf(H head) throws AStarException {
		// synchronized (statespace) {
		try {
			lock.readLock().lock();
			return statespace.contains(new State<H, T>(head, null));
		} catch (StorageException e) {
			throw new AStarException(e);
		} finally {
			lock.readLock().unlock();
		}
		// }

	}

	// AA: change visibility to public so that marking can be identified
	// directly from record
	@Override
	public State<H, T> getStoredState(Record rec) throws AStarException {
		try {
			return rec.getState(store);
		} catch (StorageException e) {
			throw new AStarException(e);
		} catch (AssertionError e){
			return null;
		}
	}
}