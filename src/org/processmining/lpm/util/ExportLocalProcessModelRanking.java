package org.processmining.lpm.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Export Local Process Model Ranking", returnLabels = {}, returnTypes = {}, parameterLabels = {
		"Local Process Model Ranking", "File" }, userAccessible = true)
@UIExportPlugin(description = "Local Process Model Ranking", extension = "lpmr")
public class ExportLocalProcessModelRanking {

	@PluginVariant(variantLabel = "Export Local Process Model Ranking", requiredParameterLabels = { 0, 1 })
	public void export(PluginContext context, LocalProcessModelRanking ranking, File file) {
		FileOutputStream fout = null;
		ObjectOutputStream oos = null;
		try {
			fout = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			if(fout==null)
				return;
			oos = new ObjectOutputStream(fout);
			oos.writeObject(ranking);
			oos.close();
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
