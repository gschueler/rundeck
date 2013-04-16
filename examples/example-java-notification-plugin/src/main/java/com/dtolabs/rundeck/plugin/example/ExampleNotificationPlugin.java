package com.dtolabs.rundeck.plugin.example;

import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import java.util.*;

@Plugin(service="Notification",name="example")
@PluginDescription(title="Example Plugin", description="An example Plugin for Rundeck Notifications.")
public class ExampleNotificationPlugin implements NotificationPlugin{

    public ExampleNotificationPlugin(){

    }

    public boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.printf("Trigger %s fired for %s, configuration: %s",trigger,executionData,config);
        return true;
    }

    public Map getConfigurationProperties() {
        HashMap<String,Object> map = new HashMap<String,Object>(){{
            put("test",new HashMap<String,String>(){{
                put("type","String");
                put("title","Test String");
            }});

        }};
        return map;
    }

    public Map validateForm(Map config) {
        return null;
    }
}