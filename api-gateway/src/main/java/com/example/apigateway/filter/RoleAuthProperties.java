package com.example.apigateway.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "role-auth")
public class RoleAuthProperties {

    private boolean enabled = true;
    private List<RoleRule> rules = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<RoleRule> getRules() { return rules; }
    public void setRules(List<RoleRule> rules) { this.rules = rules; }

    public static class RoleRule {
        private String path;
        private List<String> roles = new ArrayList<>();

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}
