package org.processmining.lpm.util;

import java.io.InputStream;
import java.io.ObjectInputStream;

import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

@Plugin(name = "Import High-Utility Local Process Model Ranking from ULPMR file", parameterLabels = { "Filename" }, returnLabels = { "Local Process Model Ranking" }, returnTypes = { UtilityLocalProcessModelRanking.class })
@UIImportPlugin(description = "Import High-Utility Local Process Model Ranking from ULPMR file", extensions = { "ulpmr" })
public class ImportUtilityLocalProcessModelRanking extends AbstractImportPlugin {

	protected Object importFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
			throws Exception {
		ObjectInputStream ois = new ObjectInputStream(input);
		Object object = ois.readObject();
		ois.close();
		if(object instanceof UtilityLocalProcessModelRanking){
			return object;
		}else{
			System.err.println("File could not be parsed as valid Local Process Model Ranking object");
		}
		return null;
	}
}
