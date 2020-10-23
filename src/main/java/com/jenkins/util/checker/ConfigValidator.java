package com.jenkins.util.checker;

import com.jenkins.util.checker.helper.DividerHelper;
import com.jenkins.util.checker.helper.config.ConfigHelperHTML;
import com.jenkins.util.checker.helper.deployment.DeploymentHelperHTML;

public class ConfigValidator {

    public static void main(String[] args) {
        System.out.println("Config Validator Status: Running!");
        String reportModel = System.getProperty("reportModel");

        if (reportModel != null) {
            if (reportModel.equalsIgnoreCase("configs")) {
                System.out.println("Report Model: Configs Selected!");
                ConfigHelperHTML helperHTML = new ConfigHelperHTML(args);
                helperHTML.initFiles();
            } else if (reportModel.equalsIgnoreCase("deployment")) {
                System.out.println("Report Model: Deployment Selected!");
                DeploymentHelperHTML helperHTML = new DeploymentHelperHTML(args);
                helperHTML.initFiles();
            }
        }

        String[] argsTest = {
                "klikBCAIndividu",
                "PILOT",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/"
        };

        DividerHelper dividerHelper = new DividerHelper(argsTest);
        dividerHelper.initFiles();

        //ConfigHelperHTML helperHTML = new ConfigHelperHTML(args);
        //helperHTML.initFiles();

        //WEB CONFIG
            /*helperHTML.initFiles(
                    "klikBCAIndividu",
                    "PILOT-WEB",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT/WEB",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/changes-config-web.txt",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/");
*/
        //APP CONFIG
            /*helperHTML.initFiles(
                    "klikBCAIndividu",
                    "PILOT-APP",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT/APP",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/changes-config-app.txt",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/");*/

        /*DeploymentHelperHTML deploymentHelperHTML = new DeploymentHelperHTML(args);
        deploymentHelperHTML.initFiles(
                "klikBCAIndividu",
                "PILOT-APP-DEPLOY",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/changes-deployment.txt",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/");*/
    }
}
