package com.start.notOfUse;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class SprinklrProject {
    private static HashMap<String , Vector> classCollection = new HashMap<>();

    public static void storeFields(String className){
        try {
            Class<?> clazz = Class.forName(className);
            Field[] fieldValues = clazz.getDeclaredFields();
            String[] allFields = Arrays.toString(fieldValues).split(",");
            Vector<String> fields = new Vector<>(List.of(allFields));
            classCollection.put(className, fields);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void  printClassCollection(){
        for (String S : classCollection.keySet()) {
            System.out.println(S + " : " + classCollection.get(S));
        }
    }
    public static void findClazz(){
        ClassFinder.findClasses(new Visitor<String>() {
            @Override
            public boolean visit(String clazz) {
                storeFields(clazz);
                return true;
            }
        });
    }
}
