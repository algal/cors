package org.jenkinsci.plugins.cors;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.util.PluginServletFilter;


/**
 * Entry point of the plugin.
 * 
 * <p>
 * There must be one {@link Plugin} class in each plugin. See javadoc of
 * {@link Plugin} for more about what can be done on this class.
 * 
 * @author Alexis Gallagher
 */
public class PluginImpl extends Plugin {
    private final static Logger LOG = Logger.getLogger(PluginImpl.class.getName());

    private ServletContext context;
    private CrossOriginFilter filter;

    /** {@inheritDoc} */
    @Override
    public void setServletContext(ServletContext context) {
        super.setServletContext(context);
        this.context = context;
    }

    /** {@inheritDoc} */
    @Override
    public void start() throws Exception {
        super.start();
        LOG.info("starting CORS plugin");

        // get values from plugin configuration panel

        // wrap them in a FilterConfig object
        Map<String,String> paramMap = new HashMap<String,String>() {{
                put(CrossOriginFilter.ALLOWED_ORIGINS_PARAM     , "");
                put(CrossOriginFilter.ALLOWED_METHODS_PARAM     , "");
                put(CrossOriginFilter.ALLOWED_HEADERS_PARAM     , "");
                put(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM   , "5");
                put(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM   , "");
                put(CrossOriginFilter.EXPOSED_HEADERS_PARAM     , "");
                put(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM     , "");
            }};
        FilterConfigWrapper configWrapper = new FilterConfigWrapper("filterName",this.context,paramMap);

        // pass the config object to initialize the plugin
        CrossOriginFilter myFilter = new CrossOriginFilter();
        PluginServletFilter.addFilter(myFilter);
        myFilter.init(configWrapper);
        this.filter = myFilter;
    }

    /** {@inheritDoc} */
    @Override
    public void stop() throws Exception {
        filter.destroy();
        super.stop();
        LOG.info("stop CORS plugin");
    }

    @Override
    public void configure(org.kohsuke.stapler.StaplerRequest req,
                          net.sf.json.JSONObject formData)
        throws java.io.IOException,
               javax.servlet.ServletException,
               hudson.model.Descriptor.FormException
    {
        LOG.config("configure called with formData.getString(\"name\") = " + formData.getString("name"));
        return ;
    }
}
