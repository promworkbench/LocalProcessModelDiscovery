package org.processmining.lpm.efficientlog;

import java.util.Set;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.id.XID;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.XVisitor;
import org.deckfour.xes.util.XAttributeUtils;
import org.xeslite.common.XSeqIDFactory;
import org.xeslite.common.XUtils;
import org.xeslite.lite.factory.XAttributeMapLiteImpl;

import com.google.common.primitives.Longs;

public class EfficientEvent implements XEvent {

	private long id;

	private XAttributeMap attributes;
	private EfficientEvent nextEvent;

	public EfficientEvent() {
		this(XSeqIDFactory.instance().nextId(), new XAttributeMapLiteImpl());
	}

	public EfficientEvent(long id) {
		this(id, new XAttributeMapLiteImpl());
	}

	public EfficientEvent(XAttributeMap attributes) {
		this(XSeqIDFactory.instance().nextId(), attributes);
	}

	public EfficientEvent(long id, XAttributeMap attributes) {
		this.id = id;
		this.attributes = attributes;
	}

	public void setNext(EfficientEvent nextEvent){
		this.nextEvent = nextEvent;
	}
	
	public EfficientEvent getNext(){
		return nextEvent;
	}
	
	public XAttributeMap getAttributes() {
		return attributes;
	}

	public void setAttributes(XAttributeMap attributes) {
		this.attributes = attributes;
	}

	public boolean hasAttributes() {
		return !attributes.isEmpty();
	}

	public Set<XExtension> getExtensions() {
		return XAttributeUtils.extractExtensions(attributes);
	}

	@Override
	public Object clone() {
		EfficientEvent clone = new EfficientEvent();
		clone.id = XSeqIDFactory.instance().nextId();
		clone.attributes = (XAttributeMap) attributes.clone();
		return clone;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof EfficientEvent) {
			return ((EfficientEvent) o).id == id;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Longs.hashCode(id);
	}

	public XID getID() {
		return new XID(id, id);
	}

	public void accept(XVisitor visitor, XTrace trace) {
		// Taken from OpenXES
		visitor.visitEventPre(this, trace);
		for (XAttribute attribute : getAttributes().values()) {
			attribute.accept(visitor, this);
		}
		visitor.visitEventPost(this, trace);
	}

	@Override
	public String toString() {
		String name = XUtils.getConceptName(this);
		return name == null ? super.toString() : name;
	}

}