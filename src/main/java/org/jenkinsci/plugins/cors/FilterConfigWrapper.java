package org.jenkinsci.plugins.cors;

import java.util.Map;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.FilterConfig;

public class FilterConfigWrapper implements FilterConfig
{
    final private String filterName;
    final private ServletContext context;
    final private Map<String,String> map;

    public FilterConfigWrapper(final String filterName,
                               final ServletContext context, 
                               final Map<String,String> map)
    {
        this.filterName = filterName;
        this.context = context;
        this.map = Collections.unmodifiableMap(new HashMap(map));
    }

    // FilterConfig interface
    public String getFilterName() { return filterName; }
    public ServletContext getServletContext() { return context; }
    public Enumeration getInitParameterNames() 
    {
        return (new Hashtable(map)).keys();
    }
    public String getInitParameter(String name)
    {
        return map.get(name);
    }

}
