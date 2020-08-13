package com.ly.common.util;

import java.io.IOException;
import java.nio.channels.Channel;

import org.springframework.lang.Nullable;

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
