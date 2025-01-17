package org.bonitasoft.livingapps;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.console.common.server.page.CustomPageService;
import org.bonitasoft.console.common.server.page.PageRenderer;
import org.bonitasoft.console.common.server.page.ResourceRenderer;
import org.bonitasoft.console.common.server.page.extension.PageResourceProviderImpl;
import org.bonitasoft.console.common.server.utils.BonitaHomeFolderAccessor;
import org.bonitasoft.engine.session.APISession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationRouterTest {

    public static final String LAYOUT_PAGE_NAME = "layoutPageName";
    @Mock(answer = Answers.RETURNS_MOCKS)
    HttpServletRequest hsRequest;

    @Mock
    HttpServletResponse hsResponse;

    @Mock
    APISession apiSession;

    @Mock
    ApplicationModelFactory applicationModelFactory;

    @Mock
    ApplicationModel applicationModel;

    @Mock
    PageRenderer pageRenderer;

    @Mock
    PageResourceProviderImpl pageResourceProvider;

    @Mock
    CustomPageService customPageService;

    @Spy
    @InjectMocks
    ResourceRenderer resourceRenderer;

    @Mock
    BonitaHomeFolderAccessor bonitaHomeFolderAccessor;

    @InjectMocks
    ApplicationRouter applicationRouter;

    @Before
    public void beforeEach() throws Exception {
        given(apiSession.getTenantId()).willReturn(1L);
        given(hsRequest.getMethod()).willReturn("GET");
        given(hsRequest.getContextPath()).willReturn("/bonita");
    }

    @Test
    public void should_redirect_to_home_page_when_accessing_living_application_root() throws Exception {

        given(applicationModel.getApplicationHomePage()).willReturn("home/");
        given(applicationModel.getApplicationLayoutName()).willReturn(LAYOUT_PAGE_NAME);
        given(applicationModelFactory.createApplicationModel("HumanResources")).willReturn(applicationModel);
        given(hsRequest.getRequestURI()).willReturn("/bonita/apps/HumanResources");
        given(hsRequest.getPathInfo()).willReturn("HumanResources");
        given(resourceRenderer.getPathSegments("HumanResources")).willReturn(Arrays.asList("HumanResources"));

        applicationRouter.route(hsRequest, hsResponse, apiSession, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor);
        verify(hsResponse).sendRedirect("home/");
    }

    @Test(expected = RuntimeException.class)
    public void should_throw_an_error_when_the_uri_is_malformed() throws Exception {
        given(hsRequest.getRequestURI()).willReturn("/bonita/apps");

        applicationRouter.route(hsRequest, hsResponse, apiSession, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor);
    }

    @Test
    public void should_display_layout_page() throws Exception {
        accessAuthorizedPage("HumanResources", "leavingRequests");

        applicationRouter.route(hsRequest, hsResponse, apiSession, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor);

        verify(pageRenderer).displayCustomPage(hsRequest, hsResponse, apiSession, applicationModel.getApplicationLayoutName());
    }

    @Test
    public void should_access_Layout_resource() throws Exception {
        accessAuthorizedPage("HumanResources", "layout/css/file.css");
        final File layoutFolder = new File("layout");
        final String customPageLayoutName = "custompage_layout";
        given(applicationModel.getApplicationLayoutName()).willReturn(customPageLayoutName);
        given(pageRenderer.getPageResourceProvider(customPageLayoutName, 1L)).willReturn(pageResourceProvider);
        given(pageResourceProvider.getPageDirectory()).willReturn(layoutFolder);
        given(bonitaHomeFolderAccessor.isInFolder(any(File.class), any(File.class))).willReturn(true);

        applicationRouter.route(hsRequest, hsResponse, apiSession, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor);

        verify(pageRenderer).ensurePageFolderIsPresent(apiSession, pageResourceProvider);
        verify(resourceRenderer).renderFile(hsRequest, hsResponse, new File("layout/resources/css/file.css"), apiSession);
    }

    @Test
    public void should_access_Theme_resource() throws Exception {
        accessAuthorizedPage("HumanResources", "theme/css/file.css");
        final File themeFolder = new File("theme");
        final String customPageThemeName = "custompage_theme";
        given(applicationModel.getApplicationThemeName()).willReturn(customPageThemeName);

        given(pageRenderer.getPageResourceProvider(customPageThemeName, 1L)).willReturn(pageResourceProvider);
        given(pageResourceProvider.getPageDirectory()).willReturn(themeFolder);
        given(bonitaHomeFolderAccessor.isInFolder(any(File.class), any(File.class))).willReturn(true);

        applicationRouter.route(hsRequest, hsResponse, apiSession, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor);

        verify(pageRenderer).ensurePageFolderIsPresent(apiSession, pageResourceProvider);
        verify(resourceRenderer).renderFile(hsRequest, hsResponse, new File("theme/resources/css/file.css"), apiSession);
    }

    @Test
    public void should_not_forward_to_the_application_page_template_when_the_page_is_not_in_the_application() throws Exception {
        accessUnknownPage("HumanResources", "leavingRequests");

        applicationRouter.route(hsRequest, hsResponse, apiSession, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor);

        verify(hsResponse).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Unauthorized access for the page " + "leavingRequests" + " of the application " + "HumanResources");
        verify(pageRenderer, never()).displayCustomPage(hsRequest, hsResponse, apiSession, LAYOUT_PAGE_NAME);
    }

    @Test
    public void should_not_forward_to_the_application_page_template_when_user_is_not_authorized() throws Exception {
        accessUnauthorizedPage("HumanResources", "leavingRequests");

        applicationRouter.route(hsRequest, hsResponse, apiSession, pageRenderer, resourceRenderer, bonitaHomeFolderAccessor);

        verify(hsResponse).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Unauthorized access for the page " + "leavingRequests" + " of the application " + "HumanResources");
        verify(pageRenderer, never()).displayCustomPage(hsRequest, hsResponse, apiSession, LAYOUT_PAGE_NAME);
    }

    private void accessAuthorizedPage(final String applicationToken, final String pageToken) throws Exception {
        accessPage(applicationToken, pageToken, true, true);
    }

    private void accessUnauthorizedPage(final String applicationToken, final String pageToken) throws Exception {
        accessPage(applicationToken, pageToken, true, false);
    }

    private void accessUnknownPage(final String applicationToken, final String pageToken) throws Exception {
        accessPage(applicationToken, pageToken, true, false);
    }

    private void accessPage(final String applicationToken, final String pageToken, final boolean hasPage, final boolean isAuthorized) throws Exception {
        given(applicationModel.hasPage(pageToken)).willReturn(hasPage);
        given(applicationModel.authorize(apiSession)).willReturn(isAuthorized);
        given(applicationModelFactory.createApplicationModel(applicationToken)).willReturn(applicationModel);
        given(hsRequest.getRequestURI()).willReturn("/bonita/apps/" + applicationToken + "/" + pageToken + "/");
        given(hsRequest.getPathInfo()).willReturn("/" + applicationToken + "/" + pageToken + "/");
    }
}
