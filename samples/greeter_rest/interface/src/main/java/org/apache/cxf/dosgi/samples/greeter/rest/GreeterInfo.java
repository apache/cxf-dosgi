package org.apache.cxf.dosgi.samples.greeter.rest;

import java.util.ArrayList;
import java.util.List;

public class GreeterInfo {
    private List<GreetingPhrase> greetings = new ArrayList<GreetingPhrase>();
    
    public void setGreetings(List<GreetingPhrase> list) {
    	greetings = list;
    }
    
    public List<GreetingPhrase> getGreetings() {
    	return greetings;
    }
    
    
}
