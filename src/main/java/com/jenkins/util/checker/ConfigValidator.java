package com.jenkins.util.checker;

import com.jenkins.util.checker.helper.ConfigHelper;

public class ConfigValidator {

    public static void main(String[] args) {
        System.out.println("Running Configs Validator!");
        ConfigHelper helper = new ConfigHelper(args);

        helper.initFiles();

        /*helper.initFiles(
                "klikBCAIndividu",
                "PILOT-SVC",
                "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestKBI_Services/null/TestKBI_Services/PILOT/CONFIG/SVC",
                //"C:/WORK_BCA/generate local config/BUILD_APP_INTER_KBI/CONFIG/APP",
                "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestKBI_Services/null/TestKBI_Services/var/changes-config-app.txt",
                "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestKBI_Services/null/TestKBI_Services/var");*/
    }
}
