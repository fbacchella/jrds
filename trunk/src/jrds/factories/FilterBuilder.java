package jrds.factories;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

import jrds.Filter;
import jrds.FilterXml;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

public class FilterBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(FilterBuilder.class);

	@Override
	Object build(JrdsNode n) throws InvocationTargetException {
		try {
			return makeFilter(n);
		} catch (SecurityException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (IllegalArgumentException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (NoSuchMethodException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (IllegalAccessException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		}
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
