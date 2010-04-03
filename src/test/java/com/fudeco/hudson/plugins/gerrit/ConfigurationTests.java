package com.fudeco.hudson.plugins.gerrit;

import com.gargoylesoftware.htmlunit.html.*;
import hudson.model.FreeStyleProject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

public class ConfigurationTests extends GerritNotifierTestCase {

    static Logger logger = Logger.getLogger(ConfigurationTests.class.getName());
    
    @Override
    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
    }

    private void testSettingExists(String settingName, HtmlPage page) {
        HtmlElement e = page.getElementByName(settingName);
        assertNotNull(e);
        
    }

    private void testHelpExists(String settingName, HtmlPage page) {
        HtmlElement e = page.getElementByName(settingName);
        
        DomNode parent = e.getParentNode().getParentNode();
        boolean found = false;
        
        for(HtmlElement element : parent.getAllHtmlChildElements()){

            if(element.getAttribute("class").equals("setting-help")) {
                found = true;
            }
        }
        assertTrue(settingName, found);
        
    }

    private void testHelpAndSetting(String settingName, HtmlPage page) {
        testSettingExists(settingName, page);
        testHelpExists(settingName, page);
    }
    @Test
    public void testConfigurationOptionsExists() throws IOException, SAXException {
        FreeStyleProject project = createProject();
        HtmlPage configPage = new WebClient().goTo("/job/" + project.getName() + "/configure");
        testSettingExists("com-fudeco-hudson-plugins-gerrit-GerritNotifier", configPage);
        testHelpAndSetting("_.gerrit_username", configPage);

        testHelpAndSetting("_.gerrit_host", configPage);
        testHelpAndSetting("_.private_key_file_path", configPage);
        testHelpAndSetting("_.passPhrase", configPage);
        testHelpAndSetting("_.reject_value", configPage);
        testHelpAndSetting("_.approve_value", configPage);
        testHelpAndSetting("_.unstable_value", configPage);
        testHelpAndSetting("_.git_home", configPage);

    }

    private void setValur(String settingName, String settingValue, HtmlPage configPage) {

    }
    
    @Test
    public void testSetValues() throws IOException, SAXException {
        FreeStyleProject project = createProject();
        WebClient wc = new WebClient();
        HtmlPage configPage = wc.goTo("/job/" + project.getName() + "/configure");
        setSettingValue(configPage, "_.gerrit_host", "example.com");
        setSettingValue(configPage, "_.gerrit_username", "new_user");
        setSettingValue(configPage, "_.private_key_file_path", "/home/user/.ssh/id_rsa");
        setSettingValue(configPage, "_.passPhrase", "passphrase");
        setSettingValue(configPage, "_.reject_value", "-2");
        setSettingValue(configPage, "_.approve_value", "+2");
        setSettingValue(configPage, "_.unstable_value", "-1");
        setSettingValue(configPage, "_.git_home", "/ws/.git");


        HtmlForm form = configPage.getFormByName("config");
        form.submit((HtmlButton)last(form.getHtmlElementsByTagName("button")));

        GerritNotifier newNotifier = project.getPublishersList().get(GerritNotifier.class);
        assertEquals("example.com", newNotifier.getGerrit_host());
        assertEquals("new_user", newNotifier.getGerrit_username());
        assertEquals("/home/user/.ssh/id_rsa", newNotifier.getPrivate_key_file_path());
        assertEquals("passphrase", newNotifier.getPassPhrase());
        assertEquals("-2", newNotifier.getReject_value());
        assertEquals("+2", newNotifier.getApprove_value());
        assertEquals("-1", newNotifier.getUnstable_value());
        assertEquals("/ws/.git", newNotifier.getGit_home());


    }

    private void setSettingValue(HtmlPage configPage, final String settingName, final String settingValue) {
        HtmlElement e = configPage.getElementByName(settingName);
        e.setAttribute("value", settingValue);
    }

}

