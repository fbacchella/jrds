package jrds;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public interface GenericBean {

    public class JavaBean implements GenericBean {
        final private PropertyDescriptor bean;

        public JavaBean(PropertyDescriptor bean) {
            this.bean = bean;
        }

        public void set(Object o, String in) {
            Constructor<?> c;
            try {
                c = bean.getPropertyType().getConstructor(String.class);
                Object value = c.newInstance(in);
                bean.getWriteMethod().invoke(o, value);
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException| IllegalArgumentException e) {
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

    public class CustomBean implements GenericBean {

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

    public void set(Object o, String in);
    public Object get(Object o);
    public String getName();

}
