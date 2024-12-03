package me.unariginal.compoundraids.managers;

import java.util.Map;

public class Messages {
    private final String prefix;
    private final Map<String, String> messagesMap;

    public Messages(String prefix, Map<String, String> messagesMap) {
        this.prefix = prefix;
        this.messagesMap = messagesMap;
    }

    public String getPrefix() {
        return prefix;
    }

    public Map<String, String> getMessagesMap() {
        return messagesMap;
    }

    public String getRawMessage(String key) {
        return messagesMap.get(key);
    }
}
