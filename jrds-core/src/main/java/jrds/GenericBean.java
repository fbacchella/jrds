package jrds;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import jrds.factories.ArgFactory;

public interface GenericBean {

    class JavaBean implements GenericBean {
        final private PropertyDescriptor bean;

        public JavaBean(PropertyDescriptor bean) {
            this.bean = bean;
        }

        public void set(Object o, String in) {
            try {
                Object value = ArgFactory.ConstructFromString(bean.getPropertyType(), in);
                bean.getWriteMethod().invoke(o, value);
            } catch (SecurityException | IllegalAccessException| IllegalArgumentException e) {
                throw new IllegalStateException("Invalid bean " + bean.getName(), e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Invalid bean " + bean.getName(), e.getCause());
            }
        }

        public Object get(Object o) {
            try {
                return bean.getReadMethod().invoke(o);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalStateException("Invalid bean " + bean.getName(), e);
            }
        }

        public String getName() {
            return bean.getName();
        }

    }

    class CustomBean implements GenericBean {

        private final String name;

        public CustomBean(String name) {
            this.name = name;
        }

        public void set(Object o, String in) {
            Probe<?, ?> p = (Probe<?, ?>) o;
            p.setBean(name, in);
        }

        public Object get(Object o) {
            Probe<?, ?> p = (Probe<?, ?>) o;
            return p.getBean(name);
        }

        public String getName() {
            return name;
        }

    }

    void set(Object o, String in);
    Object get(Object o);
    String getName();

}
