package org.processmining.lpm.plugins;

import java.io.InputStream;
import java.util.Scanner;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.log.utils.XLogBuilder;

@Plugin(name = "Load SPMF data file", parameterLabels = { "Filename" }, returnLabels = { "Log from SPMF File" }, returnTypes = { XLog.class })
@UIImportPlugin(description = "Load SPMF data file", extensions = { "spmf" })
public class SpmfLoader extends AbstractImportPlugin{
	
	public XLog parse(InputStream file){
	    XLogBuilder xlb = XLogBuilder.newInstance();
	    xlb.startLog("Log from SPMF File");
	    Scanner sc = new Scanner(file);
	    int traceNumber = 1;
	    String eventName = "";
		xlb.addTrace(""+traceNumber);
		traceNumber++;
		while (sc.hasNextLine()) {
		    String line = sc.nextLine();
		    String[] lineParts = line.split(" ");
		    for(String linePart : lineParts){
		    	if(linePart.equals("-2")){
		    		xlb.addTrace(""+traceNumber);
		    		traceNumber++;
		    	}else if(linePart.equals("-1")){
		    		xlb.addEvent(eventName);
		    	}else{
		    		eventName = linePart;
		    	}
		    }
		}
		sc.close();
	    return xlb.build();
	}

	protected Object importFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
			throws Exception {
		return parse(input);
	}
}
