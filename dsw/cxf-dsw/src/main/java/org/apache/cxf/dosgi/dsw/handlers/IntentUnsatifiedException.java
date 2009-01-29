package org.apache.cxf.dosgi.dsw.handlers;

public class IntentUnsatifiedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private String intent;
    
    public IntentUnsatifiedException(String theIntent) {
        super(theIntent);
        intent = theIntent;
    }
    
    public String getIntent() {
        return intent;
    }
}
