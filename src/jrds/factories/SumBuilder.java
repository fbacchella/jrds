package jrds.factories;

import java.util.ArrayList;

import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.probe.SumProbe;

public class SumBuilder extends ObjectBuilder {

	@Override
	Object build(JrdsNode n) {
		return makeSum(n);
	}

	public SumProbe makeSum(JrdsNode n) {
		String name = n.evaluate(CompiledXPath.get("/sum/@name"));
		if(name != null && ! "".equals(name)) {
			ArrayList<String> elements = new ArrayList<String>();
			for(JrdsNode elemNode: n.iterate(CompiledXPath.get("/sum/element/@name"))) {
				String elemName = elemNode.getTextContent();
				elements.add(elemName);
			}
			SumProbe sp = new SumProbe(name, elements);
			return sp;
		}
		return null;
	}
}
