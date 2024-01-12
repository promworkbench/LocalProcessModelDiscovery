package org.processmining.lpm.util;

import java.util.List;

public class LocalProcessModelRankingFactory {

	public static LocalProcessModelRanking createCountedAcceptingPetriNetArray() {
		return new LocalProcessModelRanking();
	}

	public static LocalProcessModelRanking createCountedAcceptingPetriNetArray(List<LocalProcessModel> nets) {
		LocalProcessModelRanking impl = createCountedAcceptingPetriNetArray();
		for (LocalProcessModel net : nets) {
			impl.addNet(net);
		}
		return impl;
	}

}
