package com.pinecni.pinegw;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pine-gw")
public class PineGwProperties {

    private String url;
    private String serviceId;
    private String host;
    private Integer port;
    private RouteConfig route;

    public String getUrl()                  { return url; }
    public void   setUrl(String url)        { this.url = url; }

    public String getServiceId()            { return serviceId; }
    public void   setServiceId(String s)    { this.serviceId = s; }

    public String getHost()                 { return host; }
    public void   setHost(String host)      { this.host = host; }

    public Integer getPort()                { return port; }
    public void    setPort(Integer port)    { this.port = port; }

    public RouteConfig getRoute()                  { return route; }
    public void        setRoute(RouteConfig route) { this.route = route; }

    public static class RouteConfig {
        private String  path;
        private String  method   = "ALL";
        private int     priority = 0;
        private boolean enabled  = true;

        public String  getPath()                { return path; }
        public void    setPath(String path)     { this.path = path; }
        public String  getMethod()              { return method; }
        public void    setMethod(String method) { this.method = method; }
        public int     getPriority()            { return priority; }
        public void    setPriority(int p)       { this.priority = p; }
        public boolean isEnabled()              { return enabled; }
        public void    setEnabled(boolean e)    { this.enabled = e; }
    }
}
