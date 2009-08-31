package jrds.factories;

import java.util.Map;

import jrds.Macro;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;
import org.w3c.dom.DocumentFragment;

public class MacroBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(MacroBuilder.class);

	private ProbeFactory pf;

	@Override
	Object build(JrdsNode n) {
		return makeMacro(n);
	}
	public Macro makeMacro(JrdsNode n) {
		Macro m = new Macro(pf);
		String name = n.evaluate(CompiledXPath.get("/macrodef/@name"));
		logger.trace("Building macro " + name);
		if(name != null && ! "".equals(name)) {
			m.setName(name);
		}

		for(JrdsNode tagNode: n.iterate(CompiledXPath.get("/macrodef/tag"))) {
			m.addTag(tagNode.getTextContent());
		}

		//Populating default argument vector
		for(JrdsNode probeNode: n.iterate(CompiledXPath.get("/macrodef/probe | /macrodef/rrd"))) {
			Map<String, String> attrMap = probeNode.attrMap();
			DocumentFragment argsFragment = probeNode.getOwnerDocument().createDocumentFragment();
			argsFragment.appendChild(probeNode.getParent());
			JrdsNode args = new JrdsNode(argsFragment.getFirstChild());
			m.put(attrMap, args);
		}
		return m;
	}

	@Override
	public
	void setProperty(ObjectBuilder.properties name, Object o) {
		switch(name) {
		case PROBEFACTORY:
			pf = (ProbeFactory) o;
			break;
		default:
			super.setProperty(name, o);
		}
	}

}
