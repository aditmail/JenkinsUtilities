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
        }

        /*ConfigHelper helper = new ConfigHelper(args);
        helper.initFiles();*/

        //TEST HTML
        //ConfigHelperHTML helper = new ConfigHelperHTML(args);

        /*CheckSumHelper checkSumHelper = new CheckSumHelper(
                "C:\\Users\\Adit\\Documents\\CI-CD\\jenkins\\JenkinsNode_Example\\workspace\\TestKBI_Services\\null\\TestKBI_Services\\INTER\\CONFIG\\SVC\\10.0.51.196_oln2appconsie07\\bcaibank\\app\\ibank_inter1_csmr\\ibconsumer_resp_approval\\config\\ibank_inter1_csmr_apprv_01\\Debug.properties",
                "C:\\Users\\Adit\\Documents\\CI-CD\\jenkins\\JenkinsNode_Example\\workspace\\TestKBI_Services\\null\\TestKBI_Services\\INTER\\CONFIG\\SVC\\10.0.51.197_oln2appconsie08\\bcaibank\\app\\ibank_inter1_csmr\\ibconsumer_resp_approval\\config\\ibank_inter1_csmr_apprv_02\\Debug.properties"
        );
        checkSumHelper.compareFiles();*/

        /*helper.initFiles(
                "klikBCAIndividu",
                "INTER-WEB",
                "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestWorkBCA_Foldering/null/TestWorkBCA_Foldering/INTER/CONFIG/WEB",
                //"C:/WORK_BCA/generate local config/BUILD_APP_INTER_KBI/CONFIG/APP",
                "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestWorkBCA_Foldering/null/TestWorkBCA_Foldering/var/changes-config-web.txt",
                "C:/Users/Adit/Documents/CI-CD/jenkins/JenkinsNode_Example/workspace/TestWorkBCA_Foldering/null/TestWorkBCA_Foldering/var");*/
    }
}
