package com.ly.common.util;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.channels.Channel;

public class ToolUtils {
    public static void closeChannel(@Nullable Channel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
