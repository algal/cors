package org.jenkinsci.plugins.cors;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.util.PluginServletFilter;
import hudson.model.Descriptor.FormException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

/**
 * Entry point of the plugin.
 * 
 * <p>
 * There must be one {@link Plugin} class in each plugin. See javadoc of
 * {@link Plugin} for more about what can be done on this class.
 * 
 * @author Alexis Gallagher
 */
public class PluginImpl
    extends Plugin
{
    // instance variables
    private final static Logger LOG = Logger.getLogger("org.jenkinsci.plugins.cors.PluginImpl");

    // defaults
    private static final String DEFAULT_ALLOWED_ORIGINS   = "*";
    private static final String DEFAULT_ALLOWED_METHODS   = "GET,POST,HEAD";
    private static final String DEFAULT_ALLOWED_HEADERS   = "X-Requested-With,Content-Type,Accept,Origin,Authorization";
    private static final String DEFAULT_PREFLIGHT_MAX_AGE = "1800";
    private static final boolean DEFAULT_ALLOW_CREDENTIALS = true;
    private static final String DEFAULT_EXPOSED_HEADERS   = "";
    private static final boolean DEFAULT_CHAIN_PREFLIGHT   = false;

    private String allowedOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private String preflightMaxAge;
    private boolean allowCredentials;
    private String exposedHeaders;
    private boolean chainPreflight;

    private ServletContext context;
    private CrossOriginFilter filter;

    public PluginImpl()
    {
        this(DEFAULT_ALLOWED_ORIGINS,
             DEFAULT_ALLOWED_METHODS,
             DEFAULT_ALLOWED_HEADERS,
             DEFAULT_PREFLIGHT_MAX_AGE,
             DEFAULT_ALLOW_CREDENTIALS,
             DEFAULT_EXPOSED_HEADERS,
             DEFAULT_CHAIN_PREFLIGHT);
        LOG.finer("entry");
        LOG.entering("","PluginImpl");
    }

    @DataBoundConstructor
    public PluginImpl(final String allowedOrigins,
                      final String allowedMethods,
                      final String allowedHeaders,
                      final String preflightMaxAge,
                      final boolean allowCredentials,
                      final String exposedHeaders,
                      final boolean chainPreflight)
    {
        super();
        LOG.finer("entry");
        // try {
        //     load(); 
        // }
        // catch (java.io.IOException e) {
        //     LOG.severe("error trying to load serialized plugin values");
        // }
        this.allowedOrigins   = allowedOrigins;
        this.allowedMethods   = allowedMethods;
        this.allowedHeaders   = allowedHeaders;
        this.preflightMaxAge  = preflightMaxAge;
        this.allowCredentials = allowCredentials;
        this.exposedHeaders   = exposedHeaders;
        this.chainPreflight   = chainPreflight; 
    }

    public boolean isChainPreflight() { return this.chainPreflight; }
    public void setChainPreflight(final boolean v) { this.chainPreflight = chainPreflight; }
    public boolean getAllowCredentials() {return this.allowCredentials;}
    public void isAllowCredentials(final boolean v) { this.allowCredentials = allowCredentials; }
    public String getAllowedOrigins() { 
        LOG.entering("PluginImpl","getAllowedOrigins");
        return this.allowedOrigins;
    }
    public void setAllowedOrigins(final String allowedOrigins) {
        LOG.entering("PluginImpl","setAllowedOrigins");
        this.allowedOrigins = allowedOrigins;
    }
    public String getAllowedMethods() { return this.allowedMethods; }
    public void setAllowedMethods(final String allowedMethods) { this.allowedMethods = allowedMethods; }
    public String getAllowedHeaders() { return this.allowedHeaders; }
    public void setAllowedHeaders(final String allowedHeaders) { this.allowedHeaders = allowedHeaders; }
    public String getPreflightMaxAge() { return this.preflightMaxAge; }
    public void setPreflightMaxAge(final String preflightMaxAge) { this.preflightMaxAge = preflightMaxAge; }
    public String getExposedHeaders() { return this.exposedHeaders; }
    public void setExposedHeaders(final String exposedHeaders) { this.exposedHeaders = exposedHeaders; }

    /** {@inheritDoc} */
    @Override
    public void setServletContext(ServletContext context) {
        super.setServletContext(context);
        LOG.finer("entry");
        this.context = context;
    }

    /** {@inheritDoc} */
    @Override
    public void start() throws Exception {
        super.start();
        LOG.finer("entry");
        // create and install the filter
        CrossOriginFilter myFilter = new CrossOriginFilter();
        PluginServletFilter.addFilter(myFilter);
        this.filter = myFilter;
    }

    @Override
    public void postInitialize() throws Exception {
        super.postInitialize();

        // log config field values in the instance variables 
        LOG.config("start() called with following state: " +
                   CrossOriginFilter.ALLOWED_ORIGINS_PARAM   + "=" +  allowedOrigins   + ", " +
                   CrossOriginFilter.ALLOWED_METHODS_PARAM   + "=" +  allowedMethods   + ", " +
                   CrossOriginFilter.ALLOWED_HEADERS_PARAM   + "=" +  allowedHeaders   + ", " +
                   CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM + "=" +  preflightMaxAge  + ", " +
                   CrossOriginFilter.ALLOW_CREDENTIALS_PARAM + "=" +  allowCredentials + ", " +
                   CrossOriginFilter.EXPOSED_HEADERS_PARAM   + "=" +  exposedHeaders   + ", " +
                   CrossOriginFilter.CHAIN_PREFLIGHT_PARAM   + "=" +  chainPreflight)   ;

        // wrap them in a FilterConfig object
        Map<String,String> paramMap = new HashMap<String,String>() {{
                put(CrossOriginFilter.ALLOWED_ORIGINS_PARAM   , allowedOrigins );
                put(CrossOriginFilter.ALLOWED_METHODS_PARAM   , allowedMethods );
                put(CrossOriginFilter.ALLOWED_HEADERS_PARAM   , allowedHeaders );
                put(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM , preflightMaxAge );
                put(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM , (allowCredentials ? "true" : "false") );
                put(CrossOriginFilter.EXPOSED_HEADERS_PARAM   , exposedHeaders );
                put(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM   , (chainPreflight ? "true" : "false") );
            }};
        FilterConfigWrapper configWrapper = new FilterConfigWrapper("filterName",this.context,paramMap);
        // pass the config object to initialize the plugin
        filter.init(configWrapper);
    }

    /** {@inheritDoc} */
    @Override
    public void stop() throws Exception {
        super.stop();
        LOG.finer("entry");
        filter.destroy();
    }

    @Override
    public void configure(StaplerRequest req,
                          JSONObject formData)
        throws java.io.IOException,
               javax.servlet.ServletException,
               hudson.model.Descriptor.FormException
    {
        LOG.config("configure() called with form data: " + 
                   CrossOriginFilter.ALLOWED_ORIGINS_PARAM   + "=" + formData.getString(  CrossOriginFilter.ALLOWED_ORIGINS_PARAM) + ", " +
                   CrossOriginFilter.ALLOWED_METHODS_PARAM   + "=" + formData.getString(  CrossOriginFilter.ALLOWED_METHODS_PARAM) + ", " +
                   CrossOriginFilter.ALLOWED_HEADERS_PARAM   + "=" + formData.getString(  CrossOriginFilter.ALLOWED_HEADERS_PARAM) + ", " +
                   CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM + "=" + formData.getString(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM) + ", " +
                   CrossOriginFilter.ALLOW_CREDENTIALS_PARAM + "=" + formData.getString(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM) + ", " +
                   CrossOriginFilter.EXPOSED_HEADERS_PARAM   + "=" + formData.getString(  CrossOriginFilter.EXPOSED_HEADERS_PARAM) + ", " +
                   CrossOriginFilter.CHAIN_PREFLIGHT_PARAM   + "=" + formData.getString(  CrossOriginFilter.CHAIN_PREFLIGHT_PARAM) );
        
        LOG.config("configure called with formData.getString(\"name\") = " + formData.getString("name"));
        req.bindJSON(this, formData);
        //        save();
        return ;
    }
}
