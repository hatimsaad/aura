/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.integration.test;

import org.auraframework.test.util.WebDriverTestCase;
import org.auraframework.util.test.annotation.ThreadHostileTest;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

@ThreadHostileTest("Tests modify what namespaces are privileged or not")
public class AccessChecksUITest extends WebDriverTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // TODO: remove when $A.createComponent is exposed in the locker
        getMockConfigAdapter().setLockerServiceEnabled(false);
    }
    
    @Test
    public void testGlobalComponentAccessibleFromUnprivilegedNamespace() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessGlobalComponent");
        clickCreateComponentButton();
        verifyComponentCreated("auratest:accessGlobalComponent");
    }

    /**
     * Cannot create a component with PUBLIC access from an unprivileged namespace.
     *
     * Note that since auratest:accessPublicComponent IS included as a dependency on accessUnprivilegedNamespace, this
     * will try to the component on the client and fail.
     */
    @Test
    public void testPublicComponentInaccessibleFromUnprivilegedNamespace() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessPublicComponent");
        clickCreateComponentButton();

        // Component create will fail on the client due to access checks so wait for error dialog to be displayed
        // and then assert no new component on page.
        waitForElementTextContains(
                getDriver().findElement(By.id("auraErrorMessage")), "componentDef is required");
        assertEquals("No new component should be present", "", getDriver().findElement(By.className("output"))
                .getText());
    }

    /**
     * Cannot create a component with INTERNAL access from an unprivileged namespace.
     *
     * Note that since we did not include auratest:accessInternalComponent as a dependency on
     * accessUnprivilegedNamespace, this will attempt to get the component from the server.
     */
    @Test
    public void testInternalComponentInaccessibleFromUnprivilegedNamespace() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessInternalComponent");
        clickCreateComponentButton();
        verifyComponentNotCreated();
    }

    /**
     * Component in a privileged namespace can extend a non-privileged namespace component marked PUBLIC
     */
    @Test
    public void testPrivilegedComponentExtendsUnprivilegedComponent() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("auratest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=componentTest:accessExtendsPublic");
        clickCreateComponentButton();
        waitForElementTextContains(getDriver().findElement(By.className("output")),
                "componentTest:accessExtendsPublic");
    }

    /**
     * Component in a unprivileged namespace can _not_ extend a privileged namespace component marked PUBLIC
     */
    @Test
    public void testUnprivilegedComponentExtendsPrivilegedComponent() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=componentTest:accessExtendsPublic");

        // Error dialog will be displayed, this is expected. Just verify inaccessible component isn't created.
        clickCreateComponentButton();
        verifyComponentNotCreated();
    }

    /**
     * Unprivileged component cannot access public attribute of privileged namespace
     */
    @Test
    public void testAccessPublicMarkupOnPrivilegedNamespaceFromUnprivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessPublicAttribute");
        doAttributeAccessTest("undefined");

    }

    @Test
    public void testAccessPublicMarkupOnUnprivilegedNamespaceFromPrivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("auratest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessPublicAttribute");
        doAttributeAccessTest("PUBLIC");
    }

    @Test
    public void testAccessGlobalMarkupOnPrivilegedNamespaceFromUnprivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessGlobalAttribute");
        doAttributeAccessTest("GLOBAL");
    }

    @Test
    public void testAccessGlobalMarkupOnUnprivilegedNamespaceFromPrivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("auratest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessGlobalAttribute");
        doAttributeAccessTest("GLOBAL");
    }

    @Test
    public void testAccessGlobalMarkupOnUnprivilegedNamespaceFromUnprivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("auratest");
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessGlobalAttribute");
        doAttributeAccessTest("GLOBAL");
    }

    @Test
    public void testAccessPrivateMarkupOnPrivilegedNamespaceFromUnprivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessPrivateAttribute");
        doAttributeAccessTest("undefined");
    }

    @Test
    public void testAccessPrivateMarkupOnUnprivilegedNamespaceFromPrivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("auratest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessPrivateAttribute");
        doAttributeAccessTest("undefined");
    }

    @Test
    public void testAccessInternalMarkupOnPrivilegedNamespaceFromUnprivileged() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessUnprivilegedNamespace.cmp?cmpToCreate=auratest:accessInternalAttribute");
        doAttributeAccessTest("undefined");
    }

    /**
     * Setting an attribute to have access level INTERNAL in a non-privileged namespace will error.
     */
    @Test
    public void testAccessInternalMarkupOnUnprivilegedNamespace() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("auratest");
        openNoAura("/auratest/accessInternalAttribute.cmp");

        String errorMsg = "org.auraframework.throwable.quickfix.InvalidAccessValueException: Invalid access attribute value \"INTERNAL\"";
        waitForElementTextContains(
                getDriver().findElement(By.id("auraErrorMessage")), errorMsg);
    }

    /**
     * An unprivileged component contains a global component in markup that provides an internal component. This is to
     * verify the internal component access is checked against the global providing component, not the top level
     * unprivileged component.
     */
    @Test
    public void testAccessGlobalProvidesInternalComponent() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("componentTest");
        open("/componentTest/accessGlobalProvidesInternal.cmp");
        waitForElementTextContains(
                getDriver().findElement(By.className("accessInternalComponent")), "auratest:accessInternalComponent");
    }

    @Ignore("TODO(W-2769151): Not sure if this should work, but if it shouldn't we need a better error message")
    @Test
    public void testAccessUnprivilegedProvidesInternalComponent() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("provider");
        open("/componentTest/accessGlobalProvidesInternal.cmp");
        waitForElementTextContains(
                getDriver().findElement(By.className("accessInternalComponent")), "auratest:accessInternalComponent");
    }

    @Ignore("TODO(W-2769151): Not sure if this should work, but if it shouldn't we need a better error message")
    @Test
    public void testAccessUnprivilegedProvidesPublicComponent() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("provider");
        open("/componentTest/accessGlobalProvidesPublic.cmp");
        waitForElementTextContains(
                getDriver().findElement(By.className("output")), "auratest:accessPublicComponent");
    }

    @Ignore("TODO(W-2769151): This case should work since the provided cmp is GLOBAL")
    @Test
    public void testAccessUnprivilegedProvidesGlobalComponent() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("provider");
        open("/componentTest/accessGlobalProvidesGlobal.cmp");
        waitForElementTextContains(
                getDriver().findElement(By.className("accessGlobalComponent")), "auratest:accessGlobalComponent");
    }

    @Test
    public void testAccessPrivilegedProvidesPublicComponent() throws Exception {
        getMockConfigAdapter().setUnprivilegedNamespace("auratest");
        open("/componentTest/accessGlobalProvidesPublic.cmp");
        waitForElementTextContains(
                getDriver().findElement(By.className("output")), "auratest:accessPublicComponent");
    }

    private void doAttributeAccessTest(String expected) {
        clickCreateComponentButton();
        waitForElementTextPresent(
                getDriver().findElement(By.className("completed")), "true");
        getDriver().findElement(By.className("getAttribute")).click();
        waitForElementTextPresent(
                getDriver().findElement(By.className("attrValue")), expected);
    }

    private void clickCreateComponentButton() {
        waitForElementAppear(By.className("testComponentAccess"));
        // Workaround for Webdriver tests run on Firefox. Calling WebElement.click() fails to click the button in some
        // situations but executing a javascript click like so seems to work.
        WebElement webElement = getDriver().findElement(By.className("testComponentAccess"));
        JavascriptExecutor executor = (JavascriptExecutor) getDriver();
        executor.executeScript("arguments[0].click();", webElement);
    }

    private void verifyComponentCreated(String expected) {
        waitForElementTextPresent(getDriver().findElement(By.className("output")), expected);
    }

    private void verifyComponentNotCreated() {
        auraUITestingUtil.waitForElementText(By.className("output"), "null", true, "Expected 'null' to be outputted "
                + "to indicate component could not be created due to access check violations");
    }
}
