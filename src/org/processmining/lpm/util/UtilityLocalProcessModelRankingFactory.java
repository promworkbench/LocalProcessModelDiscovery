package org.processmining.lpm.util;

import java.util.List;

public class UtilityLocalProcessModelRankingFactory {

	public static UtilityLocalProcessModelRanking createCountedAcceptingPetriNetArray() {
		return new UtilityLocalProcessModelRanking();
	}

	public static UtilityLocalProcessModelRanking createCountedAcceptingPetriNetArray(List<UtilityLocalProcessModel> nets) {
		UtilityLocalProcessModelRanking impl = createCountedAcceptingPetriNetArray();
		for (UtilityLocalProcessModel net : nets) {
			impl.addNet(net);
		}
		return impl;
	}

}
