package jrds.webapp;
import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscoverAgentAnnotation {
    Class<? extends DiscoverAgent> value();
}
