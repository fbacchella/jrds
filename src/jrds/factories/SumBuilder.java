package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.probe.SumProbe;

public class SumBuilder extends ObjectBuilder {

	@Override
	Object build(JrdsNode n) throws InvocationTargetException {
		try {
			return  makeSum(n);
		} catch (SecurityException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (IllegalArgumentException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (NoSuchMethodException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (IllegalAccessException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (InvocationTargetException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		}
	}

	public SumProbe makeSum(JrdsNode n) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		String name = n.evaluate(CompiledXPath.get("/sum/@name"));
		if(name != null && ! "".equals(name)) {
			ArrayList<String> elements = new ArrayList<String>();
			for(JrdsNode elemNode: n.iterate(CompiledXPath.get("/sum/element/@name"))) {
				String elemName = elemNode.getTextContent();
				elements.add(elemName);
			}
			SumProbe sp = new SumProbe(name, elements);
			n.setMethod(sp, CompiledXPath.get("/sum/role"), "addRole", false);
			return sp;
		}
		return null;
	}
}
