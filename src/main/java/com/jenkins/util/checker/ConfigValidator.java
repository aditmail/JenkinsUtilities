package com.jenkins.util.checker;

import com.jenkins.util.checker.helper.DividerHelper;

public class ConfigValidator {

    public static void main(String[] args) {
        System.out.println("Config-Deployment Validator Status: Running!");

        String[] argsTest = {
                "klikBCAIndividu",
                "PILOT",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/"
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
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/");*/

        //APP CONFIG
            /*helperHTML.initFiles(
                    "klikBCAIndividu",
                    "PILOT-APP",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT/APP",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/changes-config-app.txt",
                    "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/");*/

        //DEPLOYMENT CONFIG
        /*DeploymentHelperHTML deploymentHelperHTML = new DeploymentHelperHTML(args);
        deploymentHelperHTML.initFiles(
                "klikBCAIndividu",
                "PILOT-APP-DEPLOY",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/DEPLOY/PILOT",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/var/changes-deployment.txt",
                "D:/TEST CASE/KBI-PILOT-CONFIG-VALIDATOR/");*/
    }
}
