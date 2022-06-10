package com.start.intern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.context.support.StandardServletEnvironment;


/*
 * README
 * -> add the file in your project
 * -> add this line to main `ClassScanner.findAllAnnotatedClassesInPackage(base_package_name, Annotation.class);`
 * -> update ROOT String to the path where you want to create the files
 * -> uncomment the storeFields/ printClassCollection to see the dependency of the Annotated Classes in the project;
 * -> After running this file will create a new package of the Annotated Classes in hierarchy at the ROOT location.
 * -> Add this packet to new project to run it .
 * -> Make sure to add the required dependencies to pom.xml
 * BUG :
 * -> doesn't implements interface
 * -> container classes eg Set<Class> , List<class> etc
 * */
public class ClassScanner {

  private static final Logger logger = LoggerFactory.getLogger(ClassScanner.class);
  private static HashMap<String , Vector> classCollection = new HashMap<>();
  static final String ROOT = "/Users/ashish/Desktop/CreateFile/";

  ClassScanner(){

  }
  public static void findAllAnnotatedClassesInPackage(String packageName, Class<? extends Annotation> clazz) {
    final List<Class<?>> result = new ArrayList<>();
    final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true, new StandardServletEnvironment());
    provider.addIncludeFilter(new AnnotationTypeFilter(clazz));
    for (BeanDefinition beanDefinition : provider.findCandidateComponents(packageName)) {
      try {
        result.add(Class.forName(beanDefinition.getBeanClassName()));
      } catch (ClassNotFoundException e) {
        logger.warn("Could not resolve class object for bean definition", e);
      }
    }
    for(Class<?> obj : result ){
      createClassFile(obj);
//        storeFields(obj);
    }
  }

  public static void createClassFile(Class<?> clazz){
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
    populateFile(clazz , filePath);
  }
  private static File fileWithDirectoryAssurance(String directory, String filename) {
    File dir = new File(directory);
    if (!dir.exists()) dir.mkdirs();
    return new File(directory + "/" + filename +".java");
  }

  private static void populateFile(Class<?> clazz , String filePath){
    try {
      FileWriter myWriter = new FileWriter(filePath);
      StringBuilder text = fillTemplate(clazz);
      myWriter.write(String.valueOf(text));
      myWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  private static StringBuilder fillTemplate(Class<?> clazz) {
    StringBuilder template = new StringBuilder("");
    String clazzName = clazz.getSimpleName();
    String packageName = clazz.getPackage().getName();
    Field[] declaredFields = clazz.getDeclaredFields();
    StringBuilder pckageName = new StringBuilder("package " + packageName + ";\n\n");
    Set<String> libraries = new HashSet<>();

    Annotation[] clazzAnnotations = clazz.getAnnotations();
    for(Annotation anno : clazzAnnotations){
      String libName = anno.annotationType().getName();
      String annoName = anno.annotationType().getSimpleName();
      libraries.add(libName);
      template.append("@").append(annoName).append("\n");
    }
    template.append("public class ").append(clazzName).append(" {\n\n");
    TreeMap<String , String> fieldsWithNoAnnotation = new TreeMap<>();
    for (Field field : declaredFields) {
      String fieldType = field.getGenericType().getTypeName();
      String fieldName = field.getName();
      String sss = field.getDeclaringClass().getName();
      if(fieldType.equals("Logger"))continue;
      Annotation[] fieldAnnotation = field.getAnnotations();
      if(fieldAnnotation.length!=0) {
        for (Annotation anno : fieldAnnotation) {
          String libName = anno.annotationType().getName();
          String annoName = anno.annotationType().getSimpleName();
          libraries.add(libName);
          template.append("\t@").append(annoName).append("\n");
        }
      }
      else {
        try {
          Class<?> c = Class.forName(fieldType);
          if (c.getAnnotations().length!=0) {
            fieldsWithNoAnnotation.put(fieldType, fieldName);
          }
        }catch(ClassNotFoundException e){
          System.out.println("field missed : " + fieldName + ", inside class : " + clazzName + ", inside package : " + packageName);
        }
      }
      template.append("\t").append(fieldType).append(" ").append(fieldName).append(";\n");
    }
    if(!fieldsWithNoAnnotation.isEmpty()){
      template.append("\n\t");
      template.append("@Autowired\n\t").append(clazzName).append("( ");
      libraries.add("org.springframework.beans.factory.annotation.Autowired");
      for (String S : fieldsWithNoAnnotation.keySet()) {
        template.append(S).append(" ").append(fieldsWithNoAnnotation.get(S)).append(",");
      }
      template.deleteCharAt(template.length()-1);
      template.append("){\n");
      for (String S : fieldsWithNoAnnotation.keySet()) {
        template.append("\t\t").append("this.").append(fieldsWithNoAnnotation.get(S)).append(" = ").append(fieldsWithNoAnnotation.get(S)).append(";\n");
      }
      template.append("\t}");
    }
    template.append("\n}\n");

    StringBuilder library = new StringBuilder("");
    for(String s : libraries){
      library.append("import ").append(s).append(";\n");
    }

    StringBuilder fileContent = new StringBuilder("" + pckageName + library + "\n" + template);
    return fileContent;
  }

//    public static void storeFields(Class<?> clazz){
//        Field[] fieldValues = clazz.getDeclaredFields();
//        String[] allFields = Arrays.toString(fieldValues).split(",");
//        Vector<String> fields = new Vector<>(List.of(allFields));
//        classCollection.put(clazz.getName(), fields);
//        printClassCollection();
//    }
//    public static void  printClassCollection(){
//        for (String S : classCollection.keySet()) {
//            System.out.println(S + " : " + classCollection.get(S));
//        }
//    }
}