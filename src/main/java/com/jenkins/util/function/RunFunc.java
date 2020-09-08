package com.jenkins.util.function;

import com.jenkins.util.function.helper.ReadFileProperties;

import java.io.File;

public class RunFunc {

    public static void main(String[] args) {
        try {
            String function = args[0].trim();

            if (function.equals("ReadFileProperties")) {
                ReadFileProperties properties = new ReadFileProperties();
                File getFile = properties.getFile(args[1].trim());
                File getTempFile = properties.getFile(args[2].trim());
                String sourceTo = args[3].trim();
                properties.readFromFiles(getFile, getTempFile, sourceTo);

                /*
                //If File in Resources folder..
                File getFile = properties.getFileFromRes("printEnv.txt");
                File getTempFile = properties.getFileFromRes("temp-changes-deployment.txt");
                properties.readFromFiles(getFile, getTempFile); */
            } else {
                System.out.println("Function Not Founds!!");
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Error:: " + e.getMessage());
        }
    }
}
