package org.processmining.lpm.adjustedalignments;

import nl.tue.astar.Head;
import nl.tue.astar.Tail;
import nl.tue.astar.impl.State;
//import nl.tue.astar.impl.StateCompressor;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;
import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.impl.CompressedStoreHashSetImpl;

public class MemoryEfficientAStarAlgorithm<H extends Head, T extends Tail> {

	private final CompressedHashSet<State<H, T>> statespace;
	private final CompressedStore<State<H, T>> store;
	private final StorageAwareDelegate<H, T> delegate;
	private final StateCompressor<H, T> compressor;

	public MemoryEfficientAStarAlgorithm(StorageAwareDelegate<H, T> delegate) {
		this(delegate, 16 * 1024 , 32 * 1024, 8);
	}

	public MemoryEfficientAStarAlgorithm(StorageAwareDelegate<H, T> delegate, int blocksize, int initialCapacity, int alignment) {
		this.compressor = new StateCompressor<H, T>(delegate);
		this.delegate = delegate;
		this.statespace = new CompressedStoreHashSetImpl.IntCustomAlignment<State<H, T>>(alignment, compressor, compressor, blocksize, compressor, compressor, initialCapacity);
		this.store = statespace.getBackingStore();
		delegate.setStateSpace(statespace);
	}

	public CompressedHashSet<State<H, T>> getStatespace() {
		return statespace;
	}

	public CompressedStore<State<H, T>> getStore() {
		return store;
	}

	public StorageAwareDelegate<H, T> getDelegate() {
		return delegate;
	}

}