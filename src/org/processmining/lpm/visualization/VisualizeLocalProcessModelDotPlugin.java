package org.processmining.lpm.visualization;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionID;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ExportInteractionPanel;
import org.processmining.framework.util.ui.scalableview.interaction.PIPInteractionPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ZoomInteractionPanel;
import org.processmining.graphvisualizers.parameters.GraphVisualizerParameters;
import org.processmining.lpm.util.LocalProcessModel;
import org.processmining.lpm.util.UtilityLocalProcessModel;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.AttributeMap.ArrowType;
import org.processmining.models.graphbased.AttributeMapOwner;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraph;
import org.processmining.models.graphbased.directed.DirectedGraphEdge;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMGraphModel;
import org.processmining.models.jgraph.ProMJGraph;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.shapes.Diamond;
import org.processmining.models.shapes.Ellipse;
import org.processmining.models.shapes.Hexagon;
import org.processmining.models.shapes.Octagon;
import org.processmining.models.shapes.Polygon;
import org.processmining.models.shapes.Rectangle;
import org.processmining.models.shapes.RoundedRect;
import org.processmining.models.shapes.Shape;
import org.processmining.plugins.graphviz.colourMaps.ColourMap;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

@Plugin(name = "@0 Visualize Local Process Model (Dot)", returnLabels = { "Visualized Local Process Model" }, returnTypes = {
		JComponent.class }, parameterLabels = { "Local Process Model" }, userAccessible = true)
@Visualizer
public class VisualizeLocalProcessModelDotPlugin {
	private String overlayAttribute;
	private XLog log;
	private Map<String, Color> attributeValueToColorMap;
	private JPanel legendPanel;
	
	public VisualizeLocalProcessModelDotPlugin(){
	}
	
	public VisualizeLocalProcessModelDotPlugin(XLog log){
		this.log = log;
	}
	
	public JComponent runUI(PluginContext context, LocalProcessModel lpm, String overlayAttribute) {
		this.overlayAttribute = overlayAttribute;
		if(attributeValueToColorMap==null)
			attributeValueToColorMap = new HashMap<String,Color>();
		else
			attributeValueToColorMap.clear();
		Set<String> uniqueAttributeValues = new HashSet<String>();
		for(XTrace trace : log){
			for(XEvent event : trace){
				if(event.getAttributes().containsKey(overlayAttribute))
					uniqueAttributeValues.add(event.getAttributes().get(overlayAttribute).toString());
			}
		}
		
		Color[] colorPalette = generateColors(uniqueAttributeValues.size());
		int i=0;
		for(String uniqueAttributeValue : uniqueAttributeValues){
			attributeValueToColorMap.put(uniqueAttributeValue, colorPalette[i]);
			i++;
		}
		legendPanel = null;
		
		ProMJGraphPanel panel = (ProMJGraphPanel) visualize(context, lpm);
		ProMJGraph jGraph = panel.getGraph();
		ViewSpecificAttributeMap map = jGraph.getViewSpecificAttributes();
		
		return apply(context, map, lpm);
	}
	
	@Plugin(name = "Visualize Local Process Model (Dot)", returnLabels = { "Visualized Local Process Model" }, returnTypes = { JComponent.class }, parameterLabels = { "Local Process Model" }, userAccessible = true)
	@Visualizer
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent runUI(PluginContext context, LocalProcessModel lpm) {
		if(attributeValueToColorMap==null)
			attributeValueToColorMap = new HashMap<String,Color>();
		else
			attributeValueToColorMap.clear();
		
		ProMJGraphPanel panel = (ProMJGraphPanel) visualize(context, lpm);
		ProMJGraph jGraph = panel.getGraph();
		ViewSpecificAttributeMap map = jGraph.getViewSpecificAttributes();

		return apply(context, map, lpm);
	}
	
	public JComponent visualize(PluginContext context, LocalProcessModel lpm) {
		AcceptingPetriNet net = lpm.getAcceptingPetriNet();
		ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();
		for (Place place : net.getInitialMarking().baseSet()) {
			map.putViewSpecific(place, AttributeMap.FILLCOLOR, Color.WHITE);
			map.putViewSpecific(place, "initialPlace", true);
		}
		for (Marking marking : net.getFinalMarkings()) {
			for (Place place : marking.baseSet()) {
				if (net.getInitialMarking().baseSet().contains(place)) {
					map.putViewSpecific(place, "initialPlace", true);
					map.putViewSpecific(place, "finalPlace", true);
					map.putViewSpecific(place, AttributeMap.FILLCOLOR, Color.WHITE);
				} else {
					map.putViewSpecific(place, "finalPlace", true);
					map.putViewSpecific(place, AttributeMap.FILLCOLOR, Color.WHITE);
				}
			}
		}
		for(Transition t : net.getNet().getTransitions()){
			if(!t.isInvisible()){
				double n1 = 0d;
				String s1 = "";
				String s2 = "";
				double n2 = 0d;
				if(lpm instanceof UtilityLocalProcessModel){
					UtilityLocalProcessModel ulpm = (UtilityLocalProcessModel) lpm;
					n1 = ulpm.getAchievedUtility(t) == null ? 0d : ulpm.getAchievedUtility(t);
					s1 = ""+ n1;
					n2 = ulpm.getPotentialUtility(t.getLabel());
					s2 = ""+ n2;

				}
				else{
					n1 = lpm.getCount(t);
					s1 = ""+((int) n1);
					if(lpm.getLogActivityCountMap()!=null && lpm.getLogActivityCountMap().containsKey(t.getLabel()))
						n2 = lpm.getLogActivityCountMap().get(t.getLabel());
					s2 = ""+((int) n2);
				}
				String label = t.getLabel()+": "+System.lineSeparator()+s1+"/"+s2;

				map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel());
				map.putViewSpecific(t, AttributeMap.LABEL, label);
				map.putViewSpecific(t, AttributeMap.EXTRALABELS, new String[]{""+n1});
				map.putViewSpecific(t, AttributeMap.EXTRALABELPOSITIONS, new Point2D[]{new Point2D.Double(10, 10)});
			}
		}
	
		return visualizeGraph(context, net.getNet(), map);
	}

	public ProMJGraphPanel visualizeGraph(PluginContext context, DirectedGraph<?, ?> graph, ViewSpecificAttributeMap map) {
		return visualizeGraph(findConnection(context, graph), context, graph, map);
	}
	private GraphLayoutConnection createLayoutConnection(DirectedGraph<?, ?> graph) {
		GraphLayoutConnection c = new GraphLayoutConnection(graph);
		return c;
	}
	protected JGraphLayout getLayout(int orientation) {
		JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();
		layout.setDeterministic(true);
		layout.setCompactLayout(true);
		layout.setFineTuning(true);
		layout.setParallelEdgeSpacing(15);
		layout.setFixRoots(false);
		
	
		layout.setOrientation(orientation);

		return layout;
	}
	
	private ProMJGraphPanel visualizeGraph(GraphLayoutConnection layoutConnection, PluginContext context,
			DirectedGraph<?, ?> graph, ViewSpecificAttributeMap map) {
		boolean newConnection = false;
		if (layoutConnection == null) {
			layoutConnection = createLayoutConnection(graph);
			newConnection = true;
		}

		if (!layoutConnection.isLayedOut()) {
			// shown for the first time.
			layoutConnection.expandAll();
		}
		//		graph.signalViews();

		ProMGraphModel model = new ProMGraphModel(graph);
		ProMJGraph jgraph;
		/*
		 * Make sure that only a single ProMJGraph is created at every time.
		 * The underlying JGrpah code cannot handle creating multiple creations at the same time.
		 */
		jgraph = new ProMJGraph(model, map, layoutConnection);
		

		JGraphLayout layout = getLayout(map.get(graph, AttributeMap.PREF_ORIENTATION, SwingConstants.EAST));

		if (!layoutConnection.isLayedOut()) {

			JGraphFacade facade = new JGraphFacade(jgraph);

			facade.setOrdered(false);
			facade.setEdgePromotion(true);
			facade.setIgnoresCellsInGroups(false);
			facade.setIgnoresHiddenCells(false);
			facade.setIgnoresUnconnectedCells(false);
			facade.setDirected(true);
			facade.resetControlPoints();
			if (layout instanceof JGraphHierarchicalLayout) {
				facade.run((JGraphHierarchicalLayout) layout, true);
			} else {
				facade.run(layout, true);
			}

			Map<?, ?> nested = facade.createNestedMap(true, true);

			jgraph.getGraphLayoutCache().edit(nested);
//			jgraph.repositionToOrigin();
			layoutConnection.setLayedOut(true);

		}

		jgraph.setUpdateLayout(layout);

		ProMJGraphPanel panel = new ProMJGraphPanel(jgraph);

		panel.addViewInteractionPanel(new PIPInteractionPanel(panel), SwingConstants.NORTH);
		panel.addViewInteractionPanel(new ZoomInteractionPanel(panel, ScalableViewPanel.MAX_ZOOM), SwingConstants.WEST);
		panel.addViewInteractionPanel(new ExportInteractionPanel(panel), SwingConstants.SOUTH);

		layoutConnection.updated();

		if (newConnection) {
			context.getConnectionManager().addConnection(layoutConnection);
		}

		return panel;

	}

	protected static GraphLayoutConnection findConnection(PluginContext context, DirectedGraph<?, ?> graph) {
		return findConnection(context.getConnectionManager(), graph);
	}
	protected static GraphLayoutConnection findConnection(ConnectionManager manager, DirectedGraph<?, ?> graph) {
		Collection<ConnectionID> cids = manager.getConnectionIDs();
		for (ConnectionID id : cids) {
			Connection c;
			try {
				c = manager.getConnection(id);
			} catch (ConnectionCannotBeObtained e) {
				continue;
			}
			if (c != null && !c.isRemoved() && c instanceof GraphLayoutConnection
					&& c.getObjectWithRole(GraphLayoutConnection.GRAPH) == graph) {
				return (GraphLayoutConnection) c;
			}
		}
		return null;
	}
	public JComponent apply(PluginContext context, ViewSpecificAttributeMap map, LocalProcessModel lpm) {
		return apply(context, map, new GraphVisualizerParameters(), lpm);
	}
	public JComponent apply(PluginContext context,
			ViewSpecificAttributeMap map, GraphVisualizerParameters parameters, LocalProcessModel lpm) {
		Petrinet graph = lpm.getAcceptingPetriNet().getNet();
		Dot dot = new Dot();
		dot.setDirection(GraphDirection.leftRight);
		Map<DirectedGraphNode, DotNode> nodeMap = new HashMap<DirectedGraphNode, DotNode>();
		Map<String, String> labelToMajorityValueMap = getAttributeValuePerTransition(lpm);
		
		for (DirectedGraphNode node : graph.getNodes()) {
			DotNode dotNode = null;
			if(overlayAttribute!=null && node instanceof Transition && !((Transition) node).isInvisible()){
				String label = ((Transition) node).getLabel();
				String frequentAttribute = labelToMajorityValueMap.get(label);
				Color currentColor = attributeValueToColorMap.get(frequentAttribute);		
				dotNode = dot.addNode(node.getLabel());
				map.putViewSpecific(node, AttributeMap.FILLCOLOR, currentColor);
			}else{
				dotNode = dot.addNode(node.getLabel());
			}
			nodeMap.put(node, dotNode);
			apply(node, dotNode, map, parameters);
		}
		for (DirectedGraphEdge<? extends DirectedGraphNode, ? extends DirectedGraphNode> edge : graph.getEdges()) {
			DotEdge dotEdge = dot.addEdge(nodeMap.get(edge.getSource()), nodeMap.get(edge.getTarget()));
			apply(edge, dotEdge, map, parameters);
		}
		//		NavigableSVGPanel panel = new AnimatableSVGPanel(DotPanel.dot2svg(dot));
		
		JComponent component = new JPanel();
		component.setLayout(new BoxLayout(component, BoxLayout.Y_AXIS));
		DotPanel dPanel = new DotPanel(dot);
		
		if(overlayAttribute!=null){
			renderLegendPanel(component);
			component.add(legendPanel);
		} 
		
		component.add(dPanel);
		JPanel scorePanel = lpm.getScorePanel();
		for(Component c : scorePanel.getComponents()){
			c.setFont(new Font(null, Font.PLAIN, 20));
		}
		component.add(scorePanel);
		return component;
	}

	private void renderLegendPanel(JComponent component) {
		if(legendPanel!=null)
			return;
		legendPanel = new JPanel();
		legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.PAGE_AXIS));
		JLabel legendLabel = new JLabel("Legend");
		legendLabel.setFont(new Font(null, Font.BOLD, 20));
		legendPanel.add(legendLabel);
		legendPanel.setPreferredSize(new Dimension( 800,100));
		legendPanel.setMaximumSize(new Dimension( 800,100));
		
		JPanel innerLegendPanel = new JPanel();
		innerLegendPanel.setPreferredSize(new Dimension( 800,10));
		JScrollPane scrollPanel = new JScrollPane(innerLegendPanel);
		scrollPanel.setAutoscrolls(true);
		scrollPanel.setPreferredSize(new Dimension( 800,10));
		List<String> orderedAttributeValues = new ArrayList<String>(attributeValueToColorMap.keySet());
		Collections.sort(orderedAttributeValues);
		for(String key : orderedAttributeValues){
			JLabel valueLabel = new JLabel(key);
			valueLabel.setBackground(attributeValueToColorMap.get(key));
			valueLabel.setOpaque(true);
			valueLabel.setFont(new Font(null, Font.BOLD, 18));
			innerLegendPanel.add(valueLabel);
		}
		
		legendPanel.add(innerLegendPanel);
	}

	private Map<String, String> getAttributeValuePerTransition(LocalProcessModel lpm) {
		Map<String, String> labelToMajorityValueMap = new HashMap<String, String>();
		if(overlayAttribute!=null){
			Map<String, Map<String, Integer>> labelToAttributeValueCountssMap = new HashMap<String, Map<String, Integer>>();
			PNMatchInstancesRepResult alignments = lpm.getAlignments();
			Iterator<AllSyncReplayResult> sync = alignments.iterator();
			while(sync.hasNext()){
				AllSyncReplayResult srr = sync.next();
				for(Integer traceIndex : srr.getTraceIndex()){
					if(traceIndex>=log.size()) // TODO: this will not be needed anymore if we handle LPMs mined with constraints properly
						continue;
					XTrace trace = log.get(traceIndex);
					int eventIndex = 0;
					List<Object> nodeInstances = srr.getNodeInstanceLst().get(0);
					List<StepTypes> stepTypes = srr.getStepTypesLst().get(0);
					for(int i=0; i<nodeInstances.size(); i++){
						if(stepTypes.get(i)==StepTypes.MINVI){
							continue;
						}else if(stepTypes.get(i)==StepTypes.LMGOOD){
							String cname = "";
							XEvent event = null;
							while(!lpm.getAlphabet().contains(cname) && eventIndex<trace.size()){
								event = trace.get(eventIndex);
								cname = event.getAttributes().get("concept:name").toString();
								eventIndex++;
							}
							eventIndex++;
							if(event==null || !event.getAttributes().containsKey(overlayAttribute))
								continue;
							String value = event.getAttributes().get(overlayAttribute).toString();
							Map<String, Integer> currentAttributeValueCountsMap = labelToAttributeValueCountssMap.get(cname);
							if(currentAttributeValueCountsMap==null)
								currentAttributeValueCountsMap = new HashMap<String, Integer>();
							Integer currentCount = currentAttributeValueCountsMap.get(value);
							if(currentCount==null)
								currentCount = 0;
							currentCount++;
							currentAttributeValueCountsMap.put(value, currentCount);
							labelToAttributeValueCountssMap.put(cname, currentAttributeValueCountsMap);
						}else if(stepTypes.get(i)==StepTypes.L){
								eventIndex++;
						}
					}
				}
			}
			
			for(String key : labelToAttributeValueCountssMap.keySet()){
				int highestInt = 0;
				String highestAttributeValue = null;
				for(Entry<String,Integer> innerEntry : labelToAttributeValueCountssMap.get(key).entrySet()){
					if(innerEntry.getValue()>highestInt){
						highestInt = innerEntry.getValue();
						highestAttributeValue = innerEntry.getKey();
					}
				}
				labelToMajorityValueMap.put(key , highestAttributeValue);
			}
		}
		return labelToMajorityValueMap;
	}
	
	/*
	 * Copy (as much as possible) the attributes from the JGraph node to the dot
	 * node.
	 */
	private void apply(DirectedGraphNode node, DotNode dotNode, ViewSpecificAttributeMap map, GraphVisualizerParameters parameters) {
		AttributeMap attMap = node.getAttributeMap();
		Shape shape = getShape(attMap, AttributeMap.SHAPE, null, node, map);
		String style = "filled";
		if (shape != null) {
			if (shape instanceof RoundedRect) {
				dotNode.setOption("shape", "box");
				style = style + ",rounded";
			} else if (shape instanceof Rectangle) {
				dotNode.setOption("shape", "box");
			} else if (shape instanceof Ellipse) {
				Boolean isSquare = getBoolean(attMap, AttributeMap.SQUAREBB, false, node, map);
				dotNode.setOption("shape", isSquare ? "circle" : "ellipse");
			} else if (shape instanceof Diamond) {
				dotNode.setOption("shape", "diamond");
			} else if (shape instanceof Hexagon) {
				dotNode.setOption("shape", "hexagon");
			} else if (shape instanceof Octagon) {
				dotNode.setOption("shape", "octagon");
			} else if (shape instanceof Polygon) {
				//				attMap.get(AttributeMap.POLYGON_POINTS);
				dotNode.setOption("shape", "polygon");
			}
			dotNode.setOption("style", style);
		}
		Boolean showLabel = getBoolean(attMap, AttributeMap.SHOWLABEL, true, node, map);
		String label = getString(attMap, AttributeMap.LABEL, "", node, map);
		// HV: Setting a tooltip seems to have no effect.
		String tooltip = getString(attMap, AttributeMap.TOOLTIP, "", node, map);
		dotNode.setLabel(showLabel ? label : "");
		dotNode.setOption("tooltip", tooltip);
		Float penWidth = getFloat(attMap, AttributeMap.LINEWIDTH, 1.0F, node, map);
		dotNode.setOption("penwidth", "" + penWidth);
		Color strokeColor = getColor(attMap, AttributeMap.STROKECOLOR, Color.BLACK, node, map);
		dotNode.setOption("color", ColourMap.toHexString(strokeColor));
		Color labelColor = getColor(attMap, AttributeMap.LABELCOLOR, Color.BLACK, node, map);
		dotNode.setOption("fontcolor", ColourMap.toHexString(labelColor));
		
		if(map.get(node, "initialPlace")!=null){
			if(map.get(node, "finalPlace")!=null){
				dotNode.setOption("label", "Start + End");
			}else{
				dotNode.setOption("label", "Start");
			}
		}else{
			if(map.get(node, "finalPlace")!=null){
				dotNode.setOption("label", "End");
			}
		}
		Color fillColor = getColor(attMap, AttributeMap.FILLCOLOR, Color.WHITE, node, map);
		Color gradientColor = getColor(attMap, AttributeMap.GRADIENTCOLOR, fillColor, node, map);
		
		if (gradientColor == null || gradientColor.equals(fillColor)) {
			dotNode.setOption("fillcolor", ColourMap.toHexString(fillColor));
		} else {
			dotNode.setOption("fillcolor", ColourMap.toHexString(fillColor) + ":" + ColourMap.toHexString(gradientColor));
		}
	}
	private Color getColor(AttributeMap map, String key, Color value, AttributeMapOwner owner, ViewSpecificAttributeMap m) {
		Object obj = m.get(owner, key, null);
		if (obj != null && obj instanceof Color) {
			return (Color) obj;
		}
		obj = map.get(key);
		if (obj != null && obj instanceof Color) {
			return (Color) obj;
		}
		return value;
	}
	private Shape getShape(AttributeMap map, String key, Shape value, AttributeMapOwner owner, ViewSpecificAttributeMap m) {
		Object obj = m.get(owner, key, null);
		if (obj != null && obj instanceof Shape) {
			return (Shape) obj;
		}
		obj = map.get(key);
		if (obj != null && obj instanceof Shape) {
			return (Shape) obj;
		}
		return value;
	}
	private Boolean getBoolean(AttributeMap map, String key, Boolean value, AttributeMapOwner owner, ViewSpecificAttributeMap m) {
		Object obj = m.get(owner, key, null);
		if (obj != null && obj instanceof Boolean) {
			return (Boolean) obj;
		}
		obj = map.get(key);
		if (obj != null && obj instanceof Boolean) {
			return (Boolean) obj;
		}
		return value;
	}
	private String getString(AttributeMap map, String key, String value, AttributeMapOwner owner, ViewSpecificAttributeMap m) {
		Object obj = m.get(owner, key, null);
		if (obj != null && obj instanceof String) {
			/*
			 * Some labels contain HTML mark-up. Remove as much as possible.
			 */
			String s1 = ((String) obj).replaceAll("<br>", "\\\\n");
			String s2 = s1.replaceAll("<[^>]*>", "");
			return s2;
		}
		obj = map.get(key);
		if (obj != null && obj instanceof String) {
			/*
			 * Some labels contain HTML mark-up. Remove as much as possible.
			 */
			String s1 = ((String) obj).replaceAll("<br>", "\\\\n");
			String s2 = s1.replaceAll("<[^>]*>", "");
			return s2;
		}
		return value;
	}

	private Float getFloat(AttributeMap map, String key, Float value, AttributeMapOwner owner, ViewSpecificAttributeMap m) {
		Object obj = m.get(owner, key, null);
		if (obj != null && obj instanceof Float) {
			return (Float) obj;
		}
		obj = map.get(key);
		if (obj != null && obj instanceof Float) {
			return (Float) obj;
		}
		return value;
	}
	/*
	 * Copy (as much as possible) the attributes from the JGraph edge to the dot
	 * edge.
	 */
	private void apply(DirectedGraphEdge<?, ?> edge, DotEdge dotEdge, ViewSpecificAttributeMap map, GraphVisualizerParameters parameters) {
		AttributeMap attMap = edge.getAttributeMap();
		Boolean showLabel = getBoolean(attMap, AttributeMap.SHOWLABEL, false, edge, map);
		String label = getString(attMap, AttributeMap.LABEL, "", edge, map);
		dotEdge.setLabel(showLabel ? label : "");
		ArrowType endArrowType = getArrowType(attMap, AttributeMap.EDGEEND, ArrowType.ARROWTYPE_CLASSIC, edge, map);
		Boolean endIsFilled = getBoolean(attMap, AttributeMap.EDGEENDFILLED, false, edge, map);
		switch (endArrowType) {
			case ARROWTYPE_SIMPLE :
			case ARROWTYPE_CLASSIC :
				dotEdge.setOption("arrowhead", "open");
				break;
			case ARROWTYPE_TECHNICAL :
				dotEdge.setOption("arrowhead", endIsFilled ? "normal" : "empty");
				break;
			case ARROWTYPE_CIRCLE :
				dotEdge.setOption("arrowhead", endIsFilled ? "dot" : "odot");
				break;
			case ARROWTYPE_LINE :
				dotEdge.setOption("arrowhead", "tee");
				break;
			case ARROWTYPE_DIAMOND :
				dotEdge.setOption("arrowhead", endIsFilled ? "diamond" : "odiamond");
				break;
			case ARROWTYPE_NONE :
				dotEdge.setOption("arrowhead", "none");
				break;
			default :
				dotEdge.setOption("arrowhead", endIsFilled ? "box" : "obox");
				break;
		}
		ArrowType startArrowType = getArrowType(attMap, AttributeMap.EDGESTART, ArrowType.ARROWTYPE_CLASSIC, edge, map);
		Boolean startIsFilled = getBoolean(attMap, AttributeMap.EDGESTARTFILLED, false, edge, map);
		switch (startArrowType) {
			case ARROWTYPE_SIMPLE :
			case ARROWTYPE_CLASSIC :
				dotEdge.setOption("arrowtail", "open");
				break;
			case ARROWTYPE_TECHNICAL :
				dotEdge.setOption("arrowtail", startIsFilled ? "normal" : "empty");
				break;
			case ARROWTYPE_CIRCLE :
				dotEdge.setOption("arrowtail", startIsFilled ? "dot" : "odot");
				break;
			case ARROWTYPE_LINE :
				dotEdge.setOption("arrowtail", "tee");
				break;
			case ARROWTYPE_DIAMOND :
				dotEdge.setOption("arrowtail", startIsFilled ? "diamond" : "odiamond");
				break;
			default :
				dotEdge.setOption("arrowtail", startIsFilled ? "box" : "obox");
				break;
		}
		Float penWidth = getFloat(attMap, AttributeMap.LINEWIDTH, 1.0F, edge, map);
		dotEdge.setOption("penwidth", "" + penWidth);
		Color edgeColor = getColor(attMap, AttributeMap.EDGECOLOR, Color.BLACK, edge, map);
		dotEdge.setOption("color", ColourMap.toHexString(edgeColor));
		Color labelColor = getColor(attMap, AttributeMap.LABELCOLOR, Color.BLACK, edge, map);
		dotEdge.setOption("fontcolor", ColourMap.toHexString(labelColor));
	}
	private ArrowType getArrowType(AttributeMap map, String key, ArrowType value, AttributeMapOwner owner, ViewSpecificAttributeMap m) {
		Object obj = m.get(owner, key, null);
		if (obj != null && obj instanceof ArrowType) {
			return (ArrowType) obj;
		}
		obj = map.get(key);
		if (obj != null && obj instanceof ArrowType) {
			return (ArrowType) obj;
		}
		return value;
	}
	
	public Color[] generateColors(int n)
	{
	    Color[] cols = new Color[n];
	    for(int i = 0; i < n; i++){
	        cols[i] = Color.getHSBColor((float) i / (float) n, 0.4f, 0.8f);
	    }
	    return cols;
	}
	
}
