package org.bonitasoft.livingapps;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.console.common.server.page.CustomPageService;
import org.bonitasoft.console.common.server.page.PageRenderer;
import org.bonitasoft.console.common.server.page.ResourceRenderer;
import org.bonitasoft.console.common.server.page.extension.PageResourceProviderImpl;
import org.bonitasoft.console.common.server.utils.BonitaHomeFolderAccessor;
import org.bonitasoft.engine.business.application.ApplicationPageNotFoundException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.page.PageNotFoundException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.livingapps.exception.CreationException;

public class ApplicationRouter {

    private final ApplicationModelFactory applicationModelFactory;

    protected BonitaHomeFolderAccessor bonitaHomeFolderAccessor = new BonitaHomeFolderAccessor();

    protected final String THEME_TOKEN = "theme";

    public ApplicationRouter(final ApplicationModelFactory applicationModelFactory) {
        this.applicationModelFactory = applicationModelFactory;
    }

    public void route(final HttpServletRequest hsRequest, final HttpServletResponse hsResponse, final APISession session, final PageRenderer pageRenderer,
            final ResourceRenderer resourceRenderer, final BonitaHomeFolderAccessor bonitaHomeFolderAccessor)
            throws CreationException, BonitaException, IOException, ServletException, IllegalAccessException, InstantiationException {

        final ParsedRequest parsedRequest = parse(hsRequest.getContextPath(), hsRequest.getRequestURI());
        //Test if url contain at least application name
        final List<String> pathSegments = resourceRenderer.getPathSegments(hsRequest.getPathInfo());
        if (pathSegments.isEmpty()) {
            hsResponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "The name of the application is required.");
            return;
        }
        if ("API".equals(parsedRequest.getPageToken())) {
            //Support relative calls to the REST API from the application page using ../API/
            hsRequest.getRequestDispatcher("/" + getResourcePathWithoutApplicationToken(hsRequest.getPathInfo(), parsedRequest.getApplicationName())).forward(
                    hsRequest, hsResponse);
        } else if ("GET".equals(hsRequest.getMethod())) {
            displayPageOrResource(hsRequest, hsResponse, session, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor, parsedRequest, pathSegments);
        } else {
            hsResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "http.method_" + hsRequest.getMethod().toLowerCase() + "_not_supported");
        }
    }

    protected void displayPageOrResource(final HttpServletRequest hsRequest, final HttpServletResponse hsResponse, final APISession session,
            final PageRenderer pageRenderer, final ResourceRenderer resourceRenderer, final BonitaHomeFolderAccessor bonitaHomeFolderAccessor,
            final ParsedRequest parsedRequest, final List<String> pathSegments) throws IOException,
            ApplicationPageNotFoundException, InstantiationException, IllegalAccessException, BonitaException, PageNotFoundException, CreationException {
        final ApplicationModel application = applicationModelFactory.createApplicationModel(parsedRequest.getApplicationName());
        //If no page name, redirect to Home page
        if (parsedRequest.getPageToken() == null) {
            hsResponse.sendRedirect(application.getApplicationHomePage());
            return;
        }
        if (isApplicationPageRequest(pathSegments)) {
            //Application page request
            if (application.hasPage(parsedRequest.getPageToken()) && application.authorize(session)) {
                pageRenderer.displayCustomPage(hsRequest, hsResponse, session, application.getApplicationLayoutName());
            } else {
                hsResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Unauthorized access for the page " + parsedRequest.getPageToken() + " of the application " + parsedRequest.getApplicationName());
            }
        } else {
            //Layout or theme resource file request
            final File resourceFile = getResourceFile(pageRenderer, hsRequest.getPathInfo(), pathSegments, application, session, bonitaHomeFolderAccessor);
            pageRenderer
                    .ensurePageFolderIsPresent(session, pageRenderer.getPageResourceProvider(getPageName(pathSegments, application), session.getTenantId()));
            resourceRenderer.renderFile(hsRequest, hsResponse, resourceFile, session);
        }
    }

    private boolean isApplicationPageRequest(final List<String> pathSegments) {
        return pathSegments.size() == 2;
    }

    private File getResourceFile(final PageRenderer pageRenderer, final String resourcePath, final List<String> pathSegments,
            final ApplicationModel application, final APISession apiSession, final BonitaHomeFolderAccessor bonitaHomeFolderAccessor) throws IOException,
            BonitaException {
        final String pageName = getPageName(pathSegments, application);
        final PageResourceProviderImpl pageResourceProvider = pageRenderer.getPageResourceProvider(pageName, apiSession.getTenantId());
        final File resourceFile = new File(pageResourceProvider.getPageDirectory(), CustomPageService.RESOURCES_PROPERTY + File.separator
                + getResourcePath(resourcePath, pathSegments.get(0), pathSegments.get(1)));

        if (!bonitaHomeFolderAccessor.isInFolder(resourceFile, pageResourceProvider.getPageDirectory())) {
            throw new BonitaException("Unauthorized access to the file " + resourcePath);
        }
        return resourceFile;
    }

    private String getPageName(final List<String> pathSegments, final ApplicationModel application) throws PageNotFoundException {
        String pageName;
        if (THEME_TOKEN.equals(pathSegments.get(1))) {
            pageName = application.getApplicationThemeName();
        } else {
            pageName = application.getApplicationLayoutName();
        }
        return pageName;
    }

    private String getResourcePath(final String fullResourcePath, final String applicationName, final String pageToken) {
        //resource path match "/applicationName/pageName/{resourcePath}"
        // or "/applicationName/theme/{resourcePath}"
        String resourcePath = getResourcePathWithoutApplicationToken(fullResourcePath, applicationName);
        resourcePath = getResourcePathWithoutPageToken(resourcePath, pageToken);

        return resourcePath;
    }

    private String getResourcePathWithoutApplicationToken(final String resourcePath, final String applicationName) {
        //resource path match "/applicationName/{resourcePath}"
        return resourcePath.substring(applicationName.length() + 2);
    }

    private String getResourcePathWithoutPageToken(final String resourcePath, final String pageToken) {
        return resourcePath.substring(pageToken.length() + 1);
    }

    private ParsedRequest parse(final String context, final String uri) {
        final Pattern pattern = Pattern.compile("^" + context + "/apps/(.*)$");
        final Matcher matcher = pattern.matcher(uri);
        if (!matcher.find()) {
            throw new RuntimeException("URI badly formed.");
        }
        final String[] fragments = matcher.group(1).split("/");
        String pageToken = null;
        if (fragments.length > 1) {
            pageToken = fragments[1];
        }
        return new ParsedRequest(fragments[0], pageToken);
    }

    private class ParsedRequest {

        private final String applicationToken;

        private final String pageToken;

        public ParsedRequest(final String applicationToken, final String pageToken) {
            this.applicationToken = applicationToken;
            this.pageToken = pageToken;
        }

        public String getApplicationName() {
            return applicationToken;
        }

        public String getPageToken() {
            return pageToken;
        }
    }
}
