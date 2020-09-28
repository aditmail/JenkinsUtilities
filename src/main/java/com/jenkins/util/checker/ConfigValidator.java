package com.jenkins.util.checker;

import com.jenkins.util.checker.helper.ConfigHelperHTML;
import com.jenkins.util.checker.helper.ConfigHelperText;

public class ConfigValidator {

    public static void main(String[] args) {
        System.out.println("Config Validator Status: Running!");
        String reportModel = System.getProperty("reportModel");
        if (reportModel != null) {
            if (reportModel.equalsIgnoreCase("html")) {
                System.out.println("Report Model: HTML Selected!");

                ConfigHelperHTML helperHTML = new ConfigHelperHTML(args);
                helperHTML.initFiles();
            } else if (reportModel.equalsIgnoreCase("text")) {
                System.out.println("Report Model: Text Selected!");

                ConfigHelperText helperText = new ConfigHelperText(args);
                helperText.initFiles();
            }
        } else {
            System.out.println("Report Model: Undefined, Defaulting as HTML");

            ConfigHelperHTML helperHTML = new ConfigHelperHTML(args);
            helperHTML.initFiles();

            /*helperHTML.initFiles(
                    "klikBCAIndividu",
                    "INTER-WEB",
                    "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestWorkBCA_Foldering/null/TestWorkBCA_Foldering/INTER/CONFIG/WEB",
                    //"C:/WORK_BCA/generate local config/BUILD_APP_INTER_KBI/CONFIG/APP",
                    "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestWorkBCA_Foldering/null/TestWorkBCA_Foldering/var/changes-config-web.txt",
                    "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestWorkBCA_Foldering/null/TestWorkBCA_Foldering/var");*/
        }
    }
}
