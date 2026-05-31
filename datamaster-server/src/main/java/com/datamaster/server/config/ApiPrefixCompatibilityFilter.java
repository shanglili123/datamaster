package com.datamaster.server.config;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the new UI API prefixes compatible with the existing controller mappings.
 */
@Component
public class ApiPrefixCompatibilityFilter extends OncePerRequestFilter {

    private static final Map<String, String> NEW_TO_OLD_PREFIX;
    private static final Set<String> CATALOG_GOVERNANCE_PREFIXES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "dataCategory",
                    "dataCategoryCat",
                    "dataLevel",
                    "standardsDesensitizeList",
                    "desensitizeInterval",
                    "desensitizeRules",
                    "sensitiveLevel",
                    "dataElemCat",
                    "dataElem",
                    "desensitizeUserRel",
                    "desensitizeWhitelist"
            ))
    );

    static {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("ast", "da");
        prefixMap.put("col", "dpp");
        prefixMap.put("svc", "ds");
        prefixMap.put("mdl", "dm");
        prefixMap.put("std", "dp");
        prefixMap.put("tax", "att");
        NEW_TO_OLD_PREFIX = Collections.unmodifiableMap(prefixMap);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String rewrittenPath = rewritePath(path);

        if (rewrittenPath == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String queryString = request.getQueryString();
        String target = queryString == null ? rewrittenPath : rewrittenPath + "?" + queryString;
        RequestDispatcher dispatcher = request.getRequestDispatcher(target);
        dispatcher.forward(request, response);
    }

    private String rewritePath(String path) {
        if (path == null || path.length() < 2 || path.charAt(0) != '/') {
            return null;
        }

        int nextSlashIndex = path.indexOf('/', 1);
        String prefix = nextSlashIndex > 0 ? path.substring(1, nextSlashIndex) : path.substring(1);
        if ("cat".equals(prefix)) {
            return rewriteCatalogPath(path, nextSlashIndex);
        }

        String oldPrefix = NEW_TO_OLD_PREFIX.get(prefix);

        if (oldPrefix == null) {
            return null;
        }

        String restPath = nextSlashIndex > 0 ? path.substring(nextSlashIndex) : "";
        return "/" + oldPrefix + restPath;
    }

    private String rewriteCatalogPath(String path, int nextSlashIndex) {
        String restPath = nextSlashIndex > 0 ? path.substring(nextSlashIndex) : "";
        String modulePath = "";

        if (restPath.length() > 1) {
            int secondSlashIndex = restPath.indexOf('/', 1);
            modulePath = secondSlashIndex > 0 ? restPath.substring(1, secondSlashIndex) : restPath.substring(1);
        }

        String oldPrefix = CATALOG_GOVERNANCE_PREFIXES.contains(modulePath) ? "dg" : "mc";
        return "/" + oldPrefix + restPath;
    }
}
