package org.processmining.lpm.util;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.lpm.dialogs.LocalProcessModelParameters;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class LocalProcessModelRanking extends SerializableObjectArrayImpl<LocalProcessModel> {
	private static final long serialVersionUID = -7929477664157144235L;

	private boolean rescoringOngoing;

	public LocalProcessModelRanking() {
		rescoringOngoing = false;
	}

	public boolean acceptableLPM(LocalProcessModel net) {
		int numNonSilentTransitions = 0;
		for (Transition t : net.getAcceptingPetriNet().getNet().getTransitions()) {
			if (!t.isInvisible())
				numNonSilentTransitions++;
		}
		return numNonSilentTransitions > 1;
	}

	public int addNet(LocalProcessModel net) {
		if (acceptableLPM(net))
			return addElement(net);
		else
			return -1;
	}

	public int removeNet(LocalProcessModel net) {
		return removeElement(net);
	}

	public void addNet(int index, LocalProcessModel net) {
		if (acceptableLPM(net))
			addElement(index, net);
	}

	public void removeNet(int index) {
		removeElement(index);
	}

	public LocalProcessModel getNet(int index) {
		return getElement(index);
	}

	public void rescoreWithNewWeights(LocalProcessModelParameters lpmp) {
		while (rescoringOngoing) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		rescoringOngoing = true;
		for (LocalProcessModel lpm : this.list) {
			lpm.setParameters(lpmp);
		}
		Collections.sort(this.list);
		rescoringOngoing = false;
	}

	public void importFromStream(PluginContext context, InputStream input, String parent) throws Exception {
		throw new Exception("new yet implemented");
	}

	public void exportToFile(PluginContext context, File file) throws Exception {
		throw new Exception("new yet implemented");
	}

	public boolean isRescoringOngoing() {
		return rescoringOngoing;
	}

	public void setRescoringUngoing(boolean rescoringUngoing) {
		this.rescoringOngoing = rescoringUngoing;
	}
}