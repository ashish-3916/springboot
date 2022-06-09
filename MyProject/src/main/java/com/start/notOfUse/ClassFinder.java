package com.start.notOfUse;

import java.io.File;

public class ClassFinder {
    public static void findClasses(Visitor<String> visitor) {
        String classpath = System.getProperty("java.class.path");
        String[] paths = classpath.split(System.getProperty("path.separator"));
        String javaHome = System.getProperty("java.home");
        File file = new File(javaHome + File.separator + "lib");

        for (String path : paths) {
            file = new File(path);
            if (file.exists()) {
                findClasses(file, file,visitor);
            }
        }
    }

    private static boolean findClasses(File root, File file, Visitor<String> visitor) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (!findClasses(root, child, visitor)) {
                    return false;
                }
            }
        }
        else {
            if (file.getName().toLowerCase().endsWith(".class")) {
                if (!visitor.visit(createClassName(root, file))) {
                    return false;
                }
            }
        }
        return true;
    }
    private static String createClassName(File root, File file) {
        StringBuffer sb = new StringBuffer();
        String fileName = file.getName();
        sb.append(fileName.substring(0, fileName.lastIndexOf(".class")));
        file = file.getParentFile();
        while (file != null && !file.equals(root)) {
            sb.insert(0, '.').insert(0, file.getName());
            file = file.getParentFile();
        }
        return sb.toString();
    }
}
