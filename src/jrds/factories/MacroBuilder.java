package jrds.factories;

import jrds.Macro;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;
import org.w3c.dom.DocumentFragment;

public class MacroBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(MacroBuilder.class);

	@Override
	Object build(JrdsNode n) {
		return makeMacro(n);
	}
	
	public Macro makeMacro(JrdsNode n) {
		Macro m = new Macro();
		String name = n.evaluate(CompiledXPath.get("/macrodef/@name"));
		if(logger.isDebugEnabled())
			logger.debug("Building macro " + name);
		if(name != null && ! "".equals(name)) {
			m.setName(name);
		}

		JrdsNode macrodefnode = n.getChild(CompiledXPath.get("/macrodef"));
		DocumentFragment df = n.getOwnerDocument().createDocumentFragment();
		df.appendChild(macrodefnode.getParentNode().removeChild(macrodefnode.getParent()));
		m.setDf(df);

		return m;
	}

}
