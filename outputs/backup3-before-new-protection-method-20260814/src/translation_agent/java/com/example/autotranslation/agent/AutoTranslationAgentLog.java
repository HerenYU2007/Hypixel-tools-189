package com.example.autotranslation.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

final class AutoTranslationAgentLog {
    private AutoTranslationAgentLog() {
    }

    static void write(String message) {
        File file = new File(System.getProperty("user.home"),
                "Documents/1.8.9/outputs/auto-translation-agent-load.log");
        PrintWriter writer = null;
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            writer = new PrintWriter(new FileWriter(file, true));
            writer.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + message);
        } catch (IOException ignored) {
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
