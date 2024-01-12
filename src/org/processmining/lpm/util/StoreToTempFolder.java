package org.processmining.lpm.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.plugins.pnml.base.FullPnmlElementFactory;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.base.Pnml.PnmlType;
import org.processmining.plugins.pnml.base.PnmlElementFactory;

@Plugin(
		name = "Store LPMs to Temp Folder", 
		parameterLabels = {"Local Process Model Ranking"}, 
		returnLabels = {"None"}, 
		returnTypes = { XLog.class },
		userAccessible=true
		)
public class StoreToTempFolder {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Store LPMs to Temp Folder", requiredParameterLabels = {0})
	public void run(PluginContext context, LocalProcessModelRanking ranking){
		for(int i=0 ; i< ranking.getSize(); i++){
			LocalProcessModel lpm = ranking.getNet(i);
			AcceptingPetriNet apn = lpm.getAcceptingPetriNet();
			
			PnmlElementFactory factory = new FullPnmlElementFactory();
			Pnml pnml = new Pnml();
			synchronized (factory) {
				pnml.setFactory(factory);
				pnml = new Pnml().convertFromNet(apn.getNet(), apn.getInitialMarking(), apn.getFinalMarkings(), new GraphLayoutConnection(apn.getNet()));
				pnml.setType(PnmlType.PNML);
			}
			String text = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + pnml.exportElement(pnml);
	
			try{
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(System.getProperty("java.io.tmpdir"), "LPM "+(i+1)+".apnml"))));
				bw.write(text);
				bw.close();
			}catch(IOException e){
				System.err.println("Problem writing LPM "+(i+1));
				e.printStackTrace();
			}
		}	
	}

}