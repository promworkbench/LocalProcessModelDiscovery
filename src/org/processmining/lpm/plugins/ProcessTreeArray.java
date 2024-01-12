package org.processmining.lpm.plugins;

import java.io.File;
import java.io.InputStream;

import org.processmining.basicutils.models.impl.ObjectArrayImpl;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.NotYetImplementedException;

public class ProcessTreeArray extends ObjectArrayImpl<ProcessTree> {

	public int addTree(ProcessTree net) {
		return addElement(net);
	}

	public int removeTree(ProcessTree net) {
		return removeElement(net);
	}

	public void addTree(int index, ProcessTree net) {
		addElement(index, net);
	}

	public void removeTree(int index) {
		removeElement(index);
	}

	public ProcessTree getTree(int index) {
		return getElement(index);
	}

	public void importFromStream(PluginContext context, InputStream input, String parent) throws Exception {
		// TODO Auto-generated method stub
		throw new NotYetImplementedException();
	}

	public void exportToFile(PluginContext context, File file) throws Exception {
		// TODO Auto-generated method stub
		throw new NotYetImplementedException();
	}
}
