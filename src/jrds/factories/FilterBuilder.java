package jrds.factories;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

import jrds.Filter;
import jrds.FilterXml;
import jrds.factories.xml.JrdsNode;

public class FilterBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(FilterBuilder.class);

	@Override
	Object build(JrdsNode n) {
		try {
			return makeFilter(n);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Filter makeFilter(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		FilterXml f = new FilterXml();
		n.setMethod(f, CompiledXPath.get("/filter/name"), "setName");
		n.setMethod(f, CompiledXPath.get("/filter/path"), "addPath", false);
		n.setMethod(f, CompiledXPath.get("/filter/tag"), "addTag", false);

		logger.trace("Filter loaded: " + f.getName());
		return f;
	}

}
