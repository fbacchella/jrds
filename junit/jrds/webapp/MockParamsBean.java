package jrds.webapp;

import java.util.Map;
import java.util.Set;

public class MockParamsBean extends ParamsBean {

    Map<String, Object> params;
    public MockParamsBean(Map<String, Object> params) {
        this.params = params;
    }

    /* (non-Javadoc)
     * @see jrds.webapp.ParamsBean#getRoles()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getRoles() {
        return (Set<String>) params.get("roles");
    }

}
