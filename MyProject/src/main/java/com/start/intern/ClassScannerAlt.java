package com.start.intern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

import javax.persistence.Entity;


/**
* * README
* -> add the file in your project
* -> add this line to main `ClassScanner.findAllAnnotatedClassesInPackage(base_package_name);`
* -> update ROOT String to the path where you want to create the files
* -> After running this file will create a new package of the Annotated Classes in hierarchy at the ROOT location.
* -> Add this package to new project to run it .
* -> Make sure to add the required dependencies to pom.xml
* BUG :
*/
public class ClassScannerAlt {

    private static final Logger logger = LoggerFactory.getLogger(ClassScanner.class);
    private static TreeSet<String> requiredAnnotation = new TreeSet<>();
    static final String ROOT = "/Users/ashish/Desktop/CreateFile/";

    ClassScannerAlt(){

    }
    public static void findAllAnnotatedClassesInPackage(String packageName) {
        final List<Class<?>> result = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false, new StandardServletEnvironment());
        provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Repository.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        requiredAnnotation.add("Component");
        requiredAnnotation.add("Service");
        requiredAnnotation.add("Repository");
        requiredAnnotation.add("Controller");
        requiredAnnotation.add("Autowired");
//        provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        provider.addExcludeFilter(new AnnotationTypeFilter(SpringBootApplication.class));


        for (BeanDefinition beanDefinition : provider.findCandidateComponents(packageName)) {
            try {
                result.add(Class.forName(beanDefinition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                logger.warn("Could not resolve class object for bean definition", e);
            }
        }
        for(Class<?> obj : result ){
            createClassFile(obj);
        }
    }

    public static void createClassFile(Class<?> clazz){
        String filePath = createFile(clazz);
        populateClassFile(clazz , filePath);
    }
    public static String createFile(Class<?> clazz){
        String fileName = clazz.getSimpleName();
        String packageName = clazz.getPackage().getName();
        String packagePath =  packageName.replace('.', '/');
        String DIRECTORY = ROOT + packagePath ;
        File file= fileWithDirectoryAssurance(DIRECTORY , fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String filePath = DIRECTORY + "/" + fileName + ".java";
        return  filePath;
    }
    private static File fileWithDirectoryAssurance(String directory, String filename) {
        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();
        return new File(directory + "/" + filename +".java");
    }

    private static void populateClassFile(Class<?> clazz , String filePath){
        try {
            FileWriter myWriter = new FileWriter(filePath);
            StringBuilder text = fillClassTemplate(clazz);
            myWriter.write(String.valueOf(text));
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static StringBuilder fillClassTemplate(Class<?> clazz) {
        String clazzName = clazz.getSimpleName();
        String packageName = clazz.getPackage().getName();
        Set<String> libraries = new HashSet<>();
// ---------------------------------PACKAGE CONTENT------------------------------------------------------------------------------------------------------------------------

        StringBuilder packageContent = new StringBuilder("package " + packageName + ";\n\n");

// ---------------------------------CLASS CONTENT------------------------------------------------------------------------------------------------------------------------

        StringBuilder classContent = new StringBuilder("\n");
        Annotation[] clazzAnnotations = clazz.getAnnotations();
        for(Annotation anno : clazzAnnotations){
            String classAnnoLibName = anno.annotationType().getName();
            String classAnnoName = anno.annotationType().getSimpleName();
            if(requiredAnnotation.contains(classAnnoName)){
                libraries.add(classAnnoLibName);
                classContent.append("@").append(classAnnoName).append("\n");
            }
        }
        classContent.append("public class ").append(clazzName).append(" {\n\n");

 // ---------------------------------FIELD CONTENT------------------------------------------------------------------------------------------------------------------------

        StringBuilder fieldContent = new StringBuilder("");
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            String fieldType = field.getGenericType().getTypeName();
            String fieldTypeShort = getFieldType(fieldType);
            String fieldName = field.getName();
            String fieldLibName = field.getType().getName();
            if (fieldTypeShort.equals("Logger")) continue;
            Class<?> c  = field.getType(); // for interface

            Annotation[] fieldAnnotation = field.getDeclaredAnnotations();
            boolean fieldContainsRequiredAnnotation = false;
            for (Annotation anno : fieldAnnotation) {
                String fieldAnnoLibName = anno.annotationType().getName();
                String fieldAnnoName = anno.annotationType().getSimpleName();
                if (requiredAnnotation.contains(fieldAnnoName)) {
                    fieldContainsRequiredAnnotation = true;
                    libraries.add(fieldAnnoLibName);
                    fieldContent.append("\t@").append(fieldAnnoName).append("\n");
                }
            }
            if (fieldContainsRequiredAnnotation) {
                libraries.add(fieldLibName);
                fieldContent.append("\t").append(fieldTypeShort).append(" ").append(fieldName).append(";\n");
            }
        }

// ---------------------------------CONSTRUCTOR CONTENT------------------------------------------------------------------------------------------------------------------------

        StringBuilder constructorContent = new StringBuilder("\n");

        Constructor<?> [] constructors = clazz.getDeclaredConstructors();
        for(Constructor<?>  constructor : constructors){
            Annotation[] constructorAnnotations = constructor.getDeclaredAnnotations();
            boolean constructorContainsRequiredAnnotation = false;
            for(Annotation anno  : constructorAnnotations){
                String constructorAnnoLibName = anno.annotationType().getName();
                String constructorAnnoName = anno.annotationType().getSimpleName();
                if(requiredAnnotation.contains(constructorAnnoName)){
                    constructorContainsRequiredAnnotation = true;
                    libraries.add(constructorAnnoLibName);
                    constructorContent.append("\t@").append(constructorAnnoName).append("\n");
                }
            }
            if(constructorContainsRequiredAnnotation){
                String constructorName = clazzName;
                constructorContent.append("\t").append(constructorName).append("(");
                TreeSet<String> initialisedFields = new TreeSet<>();
                Parameter [] constructorParameters = constructor.getParameters();
                for(Parameter constructorParameter : constructorParameters){
                    String parameterName = constructorParameter.getName();
                    String parameterType = constructorParameter.toString().replace(" " + parameterName,"");
                    String parameterTypeName = getFieldType(parameterType);
                    String parameterTypeLib = constructorParameter.getType().getTypeName();
                    fieldContent.append("\t").append(parameterTypeName).append(" ").append(parameterName).append(";\n");
                    constructorContent.append(parameterTypeName).append(" ").append(parameterName).append(",");
                    libraries.add(parameterTypeLib);
                    initialisedFields.add(parameterName);
                }
                if(constructorParameters.length!=0)
                    constructorContent.deleteCharAt(constructorContent.length() -1); // delete a comma
                constructorContent.append("){\n");
                for(String initialisedField : initialisedFields){
                    constructorContent.append("\t\t").append("this.").append(initialisedField).append(" = ").append(initialisedField).append(";\n");
                }
                constructorContent.append("\t}\n");
            }
        }

// ---------------------------------LIBRARY CONTENT------------------------------------------------------------------------------------------------------------------------

        StringBuilder libraryContent = new StringBuilder("\n");
        for(String s : libraries){
            if(!s.contains(packageName))
                 libraryContent.append("import ").append(s).append(";\n");
        }
// ---------------------------------FINALLY FILE  CONTENT------------------------------------------------------------------------------------------------------------------------

        StringBuilder fileContent = new StringBuilder("");
        fileContent.append(packageContent).append(libraryContent).append(classContent).append(fieldContent).append(constructorContent).append("}");
        return fileContent;
    }

    private static String getFieldType(String fieldType) {
        int index = 0 ;
        for(int i= 0 ; i<fieldType.length(); i++){
            if(fieldType.charAt(i)=='.') index = i + 1;
            else if (fieldType.charAt(i)=='<') break;
        }
        return fieldType.substring(index);
    }
}
