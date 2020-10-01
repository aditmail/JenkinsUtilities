package com.jenkins.util.checker;

import com.jenkins.util.checker.helper.config.ConfigHelperHTML;
import com.jenkins.util.checker.helper.config.ConfigHelperText;
import com.jenkins.util.checker.helper.deployment.DeploymentHelperHTML;

public class ConfigValidator {

    public static void main(String[] args) {
        System.out.println("Config Validator Status: Running!");
        String reportModel = System.getProperty("reportModel");
        if (reportModel != null) {
            if (reportModel.equalsIgnoreCase("html")) {
                System.out.println("Report Model: HTML Selected!");

                ConfigHelperHTML helperHTML = new ConfigHelperHTML(args);
                //helperHTML.initFiles();
            } else if (reportModel.equalsIgnoreCase("text")) {
                System.out.println("Report Model: Text Selected!");

                ConfigHelperText helperText = new ConfigHelperText(args);
                helperText.initFiles();
            }
        } else {
            System.out.println("Report Models: Undefined, Defaulting as HTML");

            ConfigHelperHTML helperHTML = new ConfigHelperHTML(args);
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

            DeploymentHelperHTML deploymentHelperHTML = new DeploymentHelperHTML(args);
            deploymentHelperHTML.initFiles(
                    "klikBCAIndividu",
                    "PILOT-APP-DEPLOY",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/changes-deployment.txt",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/");
        }
    }
}
