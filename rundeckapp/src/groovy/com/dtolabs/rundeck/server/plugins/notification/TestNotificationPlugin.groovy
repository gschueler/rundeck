package com.dtolabs.rundeck.server.plugins.notification

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin

/**
 * Created with IntelliJ IDEA.
 * User: greg
 * Date: 4/11/13
 * Time: 6:48 PM
 * To change this template use File | Settings | File Templates.
 */
@Plugin(name = "test2",service = "Notification")
@PluginDescription(title = "Test2 Plugin",description = "Does a test")
public class TestNotificationPlugin implements NotificationPlugin{
    boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.println("test2: Trigger ${trigger} fired for ${executionData}, configuration: ${config}")
        true
    }

    def Map configurationProperties=[test:[type:'String',title:'Test String']]

    public Map validateForm(Map config) {
        return null
    }
}
