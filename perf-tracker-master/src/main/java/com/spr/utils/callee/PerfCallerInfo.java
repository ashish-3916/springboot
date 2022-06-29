package com.spr.utils.callee;


import java.lang.reflect.Array;

/**
 * Date: 8/8/13
 * Time: 10:31 PM
 *
 * @author Abhishek Sanoujam
 */
public class PerfCallerInfo {

    private static final String JAVA_PACKAGE = "java.";
    private static final String SUN_PACKAGE = "sun.";
    private static final String COM_SUN_PACKAGE = "com.sun.";
    private static final String CURRENT_BASE_PACKAGE = "com.spr.utils.callee";
    private static final String GENERATED_PROXY_CLASSNAME = "$Proxy";

    private static final String[] SKIP_PACKAGES = {CURRENT_BASE_PACKAGE, JAVA_PACKAGE, GENERATED_PROXY_CLASSNAME,
            SUN_PACKAGE, COM_SUN_PACKAGE};

    public static PerfCallerInfo createCalleeInfo(String... skipPackage) {
        notEmptyArray(skipPackage, "skipPackage cannot be empty");
        return new PerfCallerInfo(skipPackage);
    }

    private final String callingClassName;
    private final String callingMethodName;


    private PerfCallerInfo(String... skipPackage) {
        StackTraceElement calleeStack = extractCalleeStack(skipPackage);
        this.callingClassName = calleeStack.getClassName();
        this.callingMethodName = calleeStack.getMethodName();
    }

    private StackTraceElement extractCalleeStack(String... skipPackage) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // skip all stacks from same base package
        // skip all java.* packages
        // use the first stack after this
        int index = skipStackStartingWith(stackTrace, addAll(skipPackage, SKIP_PACKAGES));
        return stackTrace[index];
    }

    private static int skipStackStartingWith(StackTraceElement[] stackTrace, String... prefixes) {
        for (int i = 0; i < stackTrace.length; i++) {
            if (!isClassNameStartsWith(stackTrace[i].getClassName(), prefixes)) {
                return i;
            }
        }
        return 0;
    }

    private static boolean isClassNameStartsWith(String className, String[] prefixes) {
        for (String prefix : prefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }


    public String getCallingClassName() {
        return callingClassName;
    }

    public String getCallingMethodName() {
        return callingMethodName;
    }

    private <T> T[] addAll(T[] array1, T... array2) {
        if (array1 == null) {
            return clone(array2);
        } else if (array2 == null) {
            return clone(array1);
        } else {
            Class<?> type1 = array1.getClass().getComponentType();
            //noinspection unchecked
            T[] joinedArray = (T[]) Array.newInstance(type1, array1.length + array2.length);
            System.arraycopy(array1, 0, joinedArray, 0, array1.length);

            try {
                System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
                return joinedArray;
            } catch (ArrayStoreException var6) {
                Class<?> type2 = array2.getClass().getComponentType();
                if (!type1.isAssignableFrom(type2)) {
                    throw new IllegalArgumentException("Cannot store " + type2.getName() + " in an array of " + type1.getName(), var6);
                } else {
                    throw var6;
                }
            }
        }
    }

    private <T> T[] clone(T[] array) {
        return array == null ? null : array.clone();
    }

    private static <T> void notEmptyArray(T[] values, String msg) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException("Field  - cannot be empty array " + msg);
        }
    }

    @Override
    public String toString() {
        return "CalleeInfo{" +
                "callingClassName='" + callingClassName + '\'' +
                ", callingMethodName='" + callingMethodName + '\'' +
                '}';
    }
}
