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
import org.springframework.util.ClassUtils;
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
 * -> container classes eg Set<Class> , List<class> etc
 * */
public class ClassScanner {

  private static final Logger logger = LoggerFactory.getLogger(ClassScanner.class);
  private static HashMap<String , Vector> classCollection = new HashMap<>();
  private static  TreeSet<String> interfaceCollection = new TreeSet<>();
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
    createInterfaceFile();
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
    StringBuilder template = new StringBuilder("");
    String clazzName = clazz.getSimpleName();
    String packageName = clazz.getPackage().getName();
    Field[] declaredFields = clazz.getDeclaredFields();
    StringBuilder pckageName = new StringBuilder("package " + packageName + ";\n\n");
    Set<String> libraries = new HashSet<>();

    Annotation[] clazzAnnotations = clazz.getAnnotations();
    for(Annotation anno : clazzAnnotations){
      String classAnnoLibName = anno.annotationType().getName();
      String classAnnoName = anno.annotationType().getSimpleName();
      libraries.add(classAnnoLibName);
      template.append("@").append(classAnnoName).append("\n");
    }
    template.append("public class ").append(clazzName).append(" {\n\n");
    TreeMap<String , String> addToConstructor = new TreeMap<>();
    for (Field field : declaredFields) {
      String fieldType = field.getGenericType().getTypeName();
      String fieldTypeShort = getFieldType(fieldType);
      String fieldName = field.getName();
      if(fieldTypeShort.equals("Logger")) continue;
      String fieldLibName = field.getType().getName();
      libraries.add(fieldLibName);


      Annotation[] fieldAnnotation = field.getAnnotations();
      if(fieldAnnotation.length!=0) {
        for (Annotation anno : fieldAnnotation) {
          String fieldAnnoLibName = anno.annotationType().getName();
          String fieldAnnoName = anno.annotationType().getSimpleName();
          libraries.add(fieldAnnoLibName);
          template.append("\t@").append(fieldAnnoName).append("\n");
        }
      }

      try {
        Class<?> c = Class.forName(fieldType); //class can be an interface/annotated/ not annotated
        if(c.isInterface()) { // is interface -> create one
          interfaceCollection.add(fieldLibName);
        }
        else if (c.getAnnotations().length!=0) { // is a class -> check for annotated , if is component/service
          addToConstructor.put(fieldName, fieldTypeShort);
        }
      }catch(ClassNotFoundException e){
        addToConstructor.put(fieldName, fieldTypeShort);
        if(!isPrimitiveType(fieldType))
          System.out.println("missed inner fieldType of: " + fieldTypeShort + ", inside field : " + fieldName+ ", inside class : " + clazzName + ", inside package : " + packageName);
      }

      template.append("\t").append(fieldTypeShort).append(" ").append(fieldName).append(";\n");
    }
    if(!addToConstructor.isEmpty()){
      template.append("\n\t");
      template.append("@Autowired\n\t").append(clazzName).append("( ");
      libraries.add("org.springframework.beans.factory.annotation.Autowired");
      for (String S : addToConstructor.keySet()) {
        template.append(addToConstructor.get(S)).append(" ").append(S).append(",");
      }
      template.deleteCharAt(template.length()-1);
      template.append("){\n");
      for (String S : addToConstructor.keySet()) {
        template.append("\t\t").append("this.").append(S).append(" = ").append(S).append(";\n");
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

  private static String getFieldType(String fieldType) {
    int index = 0 ;
    for(int i= 0 ; i<fieldType.length(); i++){
      if(fieldType.charAt(i)=='.') index = i + 1;
      else if (fieldType.charAt(i)=='<') break;
    }
    return fieldType.substring(index);
  }


  private static void createInterfaceFile(){
    for(String c : interfaceCollection ){
      try {
        Class<?> clazz = Class.forName(c);
        String filePath = createFile(clazz);
        populateInterfaceFile(clazz , filePath);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }
  private static void populateInterfaceFile(Class<?> clazz , String filePath){
    try {
      FileWriter myWriter = new FileWriter(filePath);
      StringBuilder text = fillInterfaceTemplate(clazz);
      myWriter.write(String.valueOf(text));
      myWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  private  static StringBuilder fillInterfaceTemplate(Class<?> clazz){
    StringBuilder template = new StringBuilder("");
    String clazzName = clazz.getSimpleName();
    String packageName = clazz.getPackage().getName();
    template.append("package ").append(packageName).append(";\n\n");
    template.append("public interface ").append(clazzName).append("{\n}");
    return template;
  }
  public static boolean isPrimitiveType(String primitive){
    return primitive.equals("boolean") ||
            primitive.equals("byte") ||
            primitive.equals("char") ||
            primitive.equals("short") ||
            primitive.equals("int") ||
            primitive.equals("long") ||
            primitive.equals("float") ||
            primitive.equals("double") ;
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