package me.skybeast.nmsprotocol;

import org.bukkit.Bukkit;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * A class to use Reflection in Minecraft easily.
 * <p>
 * Idea taken from
 * <a href="https://github.com/aadnk/ProtocolLib/blob/master/modules/TinyProtocol/src/main/java/com/comphenix/tinyprotocol/Reflection.java">TinyProtocol</a>
 *
 * @author SkyBeast
 */
@SuppressWarnings({"unused", "unchecked", "ClassWithTooManyMethods"})
public final class NMSReflection
{

	/*
     * PACKAGES
	 */

    /**
     * A String that represents the CraftBukkit package.
     */
    public static final String CB = Bukkit.getServer().getClass().getPackage().getName();

    /**
     * A String that represents the NMS-Package version of the server.
     */
    public static final String VERSION = CB.substring(23);

    /**
     * A String that represents the NMS package.
     */
    public static final String NMS = "net.minecraft.server." + VERSION;

    private static final Class[]  EMPTY_CLASS_ARRAY  = new Class[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private NMSReflection() {}

    /*
     * INTERNAL
     */

    /*
     * Search field in parent classes.
     */
    private static Field findField(Class<?> clazz, String name)
    {
        return findField(clazz, name, clazz);
    }

    /*
     * Recursive.
     */
    private static Field findField(Class<?> clazz, String name, Class<?> search)
    {
        Field[] fields = search.getDeclaredFields();
        for (Field field : fields)
            if (field.getName().equals(name))
                return field;

        Class<?> superClass = search.getSuperclass();

        if (superClass != null)
            return findField(clazz, name, superClass);

        throw new NMSReflectionException("Cannot find field " + name + " in " + clazz);
    }

    /*
     * Search implemented method in parent classes, and default implementations in interfaces.
     */
    private static Method findMethod(Class<?> clazz,
                                     String name,
                                     Class<?>[] argsType)

    {
        return findMethod(clazz, name, argsType, clazz);
    }

    /*
     * Search implemented method in parent classes, and default implementations in interfaces.
     */
    private static Method findMethod(Class<?> clazz,
                                     String name,
                                     Class<?>[] argsType,
                                     Class<?> search)

    {
        Method[] methods = search.getDeclaredMethods();
        for (Method method : methods)
            if (method.getName().equals(name)
                && Arrays.equals(argsType, method.getParameterTypes())
                && (!search.isInterface() || method.isDefault())) //Default methods in interfaces are fine
                return method;

        Class<?> superClass = search.getSuperclass();
        Class[]  interfaces = search.getInterfaces();

        for (Class interf : interfaces)
            findMethod(interf, name, argsType); //Find default methods in interfaces

        if (superClass != null)
            return findMethod(clazz, name, argsType, superClass);

        throw new NMSReflectionException("Cannot find field " + name + " in " + clazz);
    }

    /*
     * Do not search in parent classes.
     */
    private static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>[] argsType)
    {
        try
        {
            return clazz.getDeclaredConstructor(argsType);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Find class by name.
     */
    private static <T> Class<T> findClass(String name)
    {
        try
        {
            return (Class<T>) Class.forName(name);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Used to search in parent classes.
     */
    private static Field countFieldOfType(Class<?> clazz,
                                          Class<?> type,
                                          int count)
    {
        return countFieldOfType(clazz, type, count, clazz);
    }

    /*
     * Recursive.
     */
    private static Field countFieldOfType(Class<?> clazz,
                                          Class<?> type,
                                          int count,
                                          Class<?> search)
    {
        int i = 0;
        for (Field field : search.getDeclaredFields())
        {
            if (field.getType() == type)
            {
                if (i == count)
                {
                    field.setAccessible(true);
                    return field;
                }
                i++;
            }
        }

        Class<?> superClass = search.getSuperclass();
        if (superClass != null)
            return countFieldOfType(clazz, type, count, superClass);

        throw new NMSReflectionException("Cannot find a field with type " + type + " in " + clazz + '.');
    }

    /*
     * Access a field.
     */
    private static <T> T fieldGet(Field field, Object inst)
    {
        try
        {
            field.setAccessible(true);
            return (T) field.get(inst);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Set a field.
     */
    private static void fieldSet(Field field, Object inst, Object value)
    {
        try
        {
            field.setAccessible(true);
            field.set(inst, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Call a method.
     */
    private static <T> T methodCall(Method method, Object inst, Object[] args)
    {
        try
        {
            method.setAccessible(true);
            return (T) method.invoke(inst, args);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Call a constructor.
     */
    private static <T> T constructorNewInstance(Constructor<T> method, Object[] args)
    {
        try
        {
            method.setAccessible(true);
            return method.newInstance(args);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Instantiate a class.
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    private static <T> T classNewInstance(Class<T> clazz)
    {
        try
        {
            return clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new NMSReflectionException(e);
        }
    }

	/*
     * EXCEPTION
	 */

    /**
     * A runtime exception to rethrow any non-runtime exception related to NMS Reflection.
     */
    public static final class NMSReflectionException extends RuntimeException
    {
        private NMSReflectionException()                            {}

        private NMSReflectionException(String arg0)                 {super(arg0);}

        private NMSReflectionException(String arg0, Throwable arg1) {super(arg0, arg1);}

        private NMSReflectionException(Throwable arg0)              {super(arg0);}
    }

	/*
     * FIELD ACCESSOR
	 */

    /**
     * A Functional Interface holding a Field.
     */
    @FunctionalInterface
    public interface FieldAccessor<T> extends Supplier<Field>
    {

        /**
         * Get the value of the field.
         *
         * @param instance the instance -- null for static access
         * @return the current value
         */
        default T get(Object instance)
        {
            try
            {
                return (T) get().get(instance);
            }
            catch (ReflectiveOperationException e)
            {
                throw new NMSReflectionException(e);
            }
        }

        /**
         * Set the value of the field.
         *
         * @param instance the instance -- null for static access
         * @param value    the new value
         */
        default void set(Object instance, T value)
        {
            try
            {
                get().set(instance, value);
            }
            catch (ReflectiveOperationException e)
            {
                throw new NMSReflectionException(e);
            }
        }

        /**
         * Check whether the target has the exact same field.
         *
         * @param target the target
         * @return <code>true</code> if the target is a sub class of the declaring class, <code>false</code> otherwise
         */
        default boolean has(Class<?> target)
        {
            return get().getDeclaringClass().isAssignableFrom(target);
        }

        /**
         * Get the declaring class of the field.
         *
         * @return the declaring class
         */
        default Class<?> getDeclaringClass()
        {
            return get().getDeclaringClass();
        }

        /**
         * Get the type of the field.
         *
         * @return the type of the field
         */
        default Class<T> getType()
        {
            return (Class<T>) get().getType();
        }
    }

    /**
     * Get a FieldAccessor by its class and name.
     *
     * @param clazz the class of the field
     * @param name  the name of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> FieldAccessor<T> getFieldAccessor(@Nonnull Class<?> clazz, @Nonnull String name)
    {
        return getFieldAccessor(findField(clazz, name));
    }

    /**
     * Get a FieldAccessor by its class and name. This method resolves the clazz.
     *
     * @param clazz a string representing the clazz
     * @param name  the name of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> FieldAccessor<T> getFieldAccessor(@Nonnull String clazz, @Nonnull String name)
    {
        return getFieldAccessor(findField(getClass(clazz), name));
    }

    /**
     * Get a FieldAccessor by its Field.
     *
     * @param field the field to hook to
     * @return a FieldAccessor holding the Field
     */
    public static <T> FieldAccessor<T> getFieldAccessor(@Nonnull Field field)
    {
        field.setAccessible(true); // Disable Accessible check -- Faster
        return () -> field;
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found.
     *
     * @param clazz the class of the field
     * @param type  the type of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(@Nonnull Class<?> clazz, @Nonnull Class<?> type)
    {
        return getCountFieldOfTypeAccessor(clazz, type, 0);
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found. This method resolves the type.
     *
     * @param clazz the class of the field
     * @param type  a string representing the type of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(@Nonnull Class<?> clazz, @Nonnull String type)
    {
        return getCountFieldOfTypeAccessor(clazz, getClass(type), 0);
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found. This method resolves the class.
     *
     * @param clazz a string representing the class
     * @param type  the class of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(@Nonnull String clazz, @Nonnull Class<?> type)
    {
        return getCountFieldOfTypeAccessor(getClass(clazz), type, 0);
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found. This method resolves the classes.
     *
     * @param clazz a string representing the class
     * @param type  a string representing the type of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(@Nonnull String clazz, @Nonnull String type)
    {
        return getCountFieldOfTypeAccessor(getClass(clazz), getClass(type), 0);
    }

    /**
     * Find a field accessor by its clazz, type and place in the clazz.
     *
     * @param clazz the class of the field
     * @param type  the type of the field
     * @param count the place of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(@Nonnull Class<?> clazz, @Nonnull Class<?> type,
                                                                   int count)
    {
        return getFieldAccessor(countFieldOfType(clazz, type, count));
    }

    /**
     * Find a field accessor by its clazz, type and place in the clazz. This method resolves the type.
     *
     * @param clazz the class of the field
     * @param type  a string representing the type of the field
     * @param count the place of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(@Nonnull Class<?> clazz, @Nonnull String type,
                                                                   int count)
    {
        return getCountFieldOfTypeAccessor(clazz, getClass(type), count);
    }

    /**
     * Find a field accessor by its clazz, type and place in the clazz. This method resolves the class.
     *
     * @param clazz a string representing the class
     * @param type  the class of the field
     * @param count the place of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(@Nonnull String clazz, @Nonnull Class<?> type,
                                                                   int count)
    {
        return getCountFieldOfTypeAccessor(getClass(clazz), type, count);
    }

    /**
     * Find a field accessor by its clazz, type and place in the clazz. This method resolves the classes.
     *
     * @param clazz a string representing the class
     * @param type  a string representing the type of the field
     * @param count the place of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(@Nonnull String clazz, @Nonnull String type,
                                                                   int count)
    {
        return getCountFieldOfTypeAccessor(getClass(clazz), getClass(type), count);
    }

	/*
     * METHOD ACCESSOR
	 */

    /**
     * A Functional Interface holding a Method.
     */
    @FunctionalInterface
    public interface MethodAccessor<T> extends Supplier<Method>
    {

        /**
         * Invoke the method.
         *
         * @param instance the instance -- null for static access
         * @param args     the args of the method
         * @return the result of dispatching the method or <code>null</code> if the return type is void
         */
        default T invoke(Object instance, Object... args)
        {
            try
            {
                return (T) get().invoke(instance, args);
            }
            catch (ReflectiveOperationException e)
            {
                throw new NMSReflectionException(e);
            }
        }

        /**
         * Check whether the target has the exact same method.
         *
         * @param target the target
         * @return <code>true</code> if the target is a sub class of the declaring class, <code>false</code> otherwise
         */
        default boolean has(@Nonnull Class<?> target)
        {
            return get().getDeclaringClass().isAssignableFrom(target);
        }

        /**
         * Get the declaring class of the field.
         *
         * @return the declaring class
         */
        default Class<?> getDeclaringClass()
        {
            return get().getDeclaringClass();
        }

        /**
         * Get the return type of the method.
         *
         * @return the return type of the method
         */
        default Class<T> getReturnType()
        {
            return (Class<T>) get().getReturnType();
        }

        /**
         * Get the parameter types of the method.
         *
         * @return the parameter types
         */
        default Class<?>[] getParameterTypes()
        {
            return get().getParameterTypes();
        }

        /**
         * Get the exception types of the method.
         *
         * @return the exception types
         */
        default Class<?>[] getExceptionTypes()
        {
            return get().getExceptionTypes();
        }
    }

    /**
     * Get a MethodAccessor by its class, name and parameters.
     *
     * @param clazz the class of the method
     * @param name  the name of the method
     * @param args  the parameters of the method
     * @return a MethodAccessor holding the Method
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> MethodAccessor<T> getMethodAccessor(@Nonnull Class<?> clazz, @Nonnull String name,
                                                          @Nonnull Class<?>... args)
    {
        return getMethodAccessor(findMethod(clazz, name, args));
    }

    /**
     * Get a MethodAccessor by its class, name and parameters. This method resolves the clazz.
     *
     * @param clazz a string representing the clazz
     * @param name  the name of the method
     * @param args  the parameters of the method
     * @return a MethodAccessor holding the Method
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> MethodAccessor<T> getMethodAccessor(@Nonnull String clazz, @Nonnull String name,
                                                          @Nonnull Class<?>... args)
    {
        return getMethodAccessor(findMethod(getClass(clazz), name, args));
    }

    /**
     * Get a MethodAccessor by its Method.
     *
     * @param method the method to hook to
     * @return a MethodAccessor holding the Method
     */
    public static <T> MethodAccessor<T> getMethodAccessor(@Nonnull Method method)
    {
        method.setAccessible(true); // Disable Accessible check -- Faster
        return () -> method;
    }

	/*
     * CONSTRUCTOR ACCESSOR
	 */

    /**
     * A Functional Interface holding a Constructor.
     */
    @FunctionalInterface
    public interface ConstructorAccessor<T> extends Supplier<Constructor<T>>
    {

        /**
         * Invoke the constructor.
         *
         * @param args the args of the method
         * @return the new instance
         */
        default T newInstance(Object... args)
        {
            try
            {
                return get().newInstance(args);
            }
            catch (ReflectiveOperationException e)
            {
                throw new NMSReflectionException(e);
            }
        }

        /**
         * Check whether the target has the exact same constructor.
         *
         * @param target the target
         * @return <code>true</code> if the target is a sub class of the declaring class, <code>false</code> otherwise
         */
        default boolean has(@Nonnull Class<?> target)
        {
            return get().getDeclaringClass().isAssignableFrom(target);
        }

        /**
         * Get the declaring class of the field.
         *
         * @return the declaring class
         */
        default Class<?> getDeclaringClass()
        {
            return get().getDeclaringClass();
        }

        /**
         * Get the parameter types of the constructor.
         *
         * @return the parameter types
         */
        default Class<?>[] getParameterTypes()
        {
            return get().getParameterTypes();
        }

        /**
         * Get the exception types of the constructor.
         *
         * @return the exception types
         */
        default Class<?>[] getExceptionTypes()
        {
            return get().getExceptionTypes();
        }
    }

    /**
     * Get a ConstructorAccessor by its class and parameters.
     *
     * @param clazz the class of the constructor
     * @param args  the parameters of the constructor
     * @return a ConstructorAccessor holding the Constructor
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> ConstructorAccessor<T> getConstructorAccessor(@Nonnull Class<T> clazz, @Nonnull Class<?>... args)
    {
        return getConstructorAccessor(findConstructor(clazz, args));
    }

    /**
     * Get a ConstructorAccessor by its class and parameters. This method resolves the clazz.
     *
     * @param clazz a string representing the clazz
     * @param args  the parameters of the constructor
     * @return a ConstructorAccessor holding the Constructor
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> ConstructorAccessor<T> getConstructorAccessor(@Nonnull String clazz, @Nonnull Class<?>... args)
    {
        return (ConstructorAccessor<T>) getConstructorAccessor(findConstructor(getClass(clazz), args));
    }

    /**
     * Get a ConstructorAccessor by its Constructor.
     *
     * @param constructor the constructor to hook to
     * @return a ConstructorAccessor holding the Constructor
     */
    public static <T> ConstructorAccessor<T> getConstructorAccessor(@Nonnull Constructor<T> constructor)
    {
        constructor.setAccessible(true); // Disable Accessible check -- Faster
        return () -> constructor;
    }

	/*
     * UTILS
	 */

    /**
     * Get a class from its name. Replace <code>{nms}</code> to {@link #NMS}, <code>{cb}</code> to {@link #CB} and
     * <code>{version}</code> to {@link #VERSION}
     *
     * @param clazz the class to resolve
     * @return a ConstructorAccessor holding the Constructor
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
    public static <T> Class<T> getClass(@Nonnull String clazz)
    {
        return findClass(clazz.replace("{nms}", NMS).replace("{cb}", CB).replace("{version}", VERSION));
    }

    /**
     * Get a NMS class by its simple name.
     *
     * @param clazz the simple name of the class
     * @return the class
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> Class<T> getNMSClass(@Nonnull String clazz)
    {
        return findClass(NMS + '.' + clazz);
    }

    /**
     * Get a CB class by its name without the CB prefix.
     *
     * @param clazz the simple name of the class
     * @return the class
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> Class<T> getCBClass(@Nonnull String clazz)
    {
        return findClass(CB + '.' + clazz);
    }

    /**
     * Get the name of the package of a class.
     *
     * @param clazz the class
     * @return the name of the package
     */
    public static String getPackage(@Nonnull String clazz)
    {
        int index = clazz.lastIndexOf('.');
        return index > 0 ? clazz.substring(0, index) : ""; //Empty string if default package
    }

	/*
     * HANDY REFLECTION
	 */

    /**
     * Invoke a method in a non-static way.
     *
     * @param instance the instance
     * @param method   the name of the method to call
     * @param argsType the types of the arguments
     * @param args     the arguments
     * @return the result of dispatching the method or <code>null</code> if the return type is void
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T callMethod(@Nonnull Object instance,
                                   @Nonnull String method,
                                   @Nonnull Class<?>[] argsType,
                                   @Nonnull Object[] args)
    {
        return methodCall(findMethod(instance.getClass(), method, argsType), instance, args);
    }

    /**
     * Invoke a method in a static way.
     *
     * @param clazz    the class where the method is
     * @param method   the name of the method to call
     * @param argsType the types of the arguments
     * @param args     the arguments
     * @return the result of dispatching the method or <code>null</code> if the return type is void
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T callStaticMethod(@Nonnull Class<?> clazz,
                                         @Nonnull String method,
                                         @Nonnull Class<?>[] argsType,
                                         @Nonnull Object[] args)
    {
        return methodCall(findMethod(clazz, method, argsType), null, args);
    }

    /**
     * Invoke a method in a static way. This method resolves the class.
     *
     * @param clazz    a string representing the clazz
     * @param method   the name of the method to call
     * @param argsType the types of the arguments
     * @param args     the arguments
     * @return the result of dispatching the method or <code>null</code> if the return type is void
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T callStaticMethod(@Nonnull String clazz,
                                         @Nonnull String method,
                                         @Nonnull Class<?>[] argsType,
                                         @Nonnull Object[] args)
    {
        return methodCall(findMethod(getClass(clazz), method, argsType), null, args);
    }

    /**
     * Get the value of a field in a non-static way.
     *
     * @param instance the instance
     * @param field    the name of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T getValue(@Nonnull Object instance, @Nonnull String field)
    {
        return fieldGet(findField(instance.getClass(), field), instance);
    }

    /**
     * Get the value of a field by its type. Return the first found.
     *
     * @param instance the instance
     * @param type     the type of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> T getFirstValueOfType(@Nonnull Object instance, @Nonnull Class<?> type)
    {
        return fieldGet(countFieldOfType(instance.getClass(), type, 0), instance);
    }

    /**
     * Get the value of a field by its type. Return the first found. This method resolves the class.
     *
     * @param instance the instance
     * @param type     a string representing the type of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> T getFirstValueOfType(@Nonnull Object instance, @Nonnull String type)
    {
        return fieldGet(countFieldOfType(instance.getClass(), getClass(type), 0), instance);
    }

    /**
     * Get the value of the xth field with type <code>type</code>.
     *
     * @param instance the instance
     * @param type     a string representing the type of the field
     * @param count    x
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> T getCountValueOfType(@Nonnull Object instance, @Nonnull Class<?> type, int count)
    {
        return fieldGet(countFieldOfType(instance.getClass(), type, count), instance);
    }

    /**
     * Get the value of the xth field with type <code>type</code>. This method resolves the class.
     *
     * @param instance the instance
     * @param type     a string representing the type of the field
     * @param count    x
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> T getCountValueOfType(@Nonnull Object instance, @Nonnull String type, int count)
    {
        return fieldGet(countFieldOfType(instance.getClass(), getClass(type), count), instance);
    }

    /**
     * Set the value of a field in a non-static way.
     *
     * @param instance the instance
     * @param field    the name of the field
     * @param value    the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static void setField(@Nonnull Object instance, @Nonnull String field, @Nonnull Object value)
    {
        fieldSet(findField(instance.getClass(), field), instance, value);
    }

    /**
     * Get the value of a field by its type. Return the first found.
     *
     * @param instance the instance
     * @param type     the type of the field
     * @param value    the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static void setFirstValueOfType(@Nonnull Object instance, @Nonnull Class<?> type, @Nonnull Object value)
    {
        fieldSet(countFieldOfType(instance.getClass(), type, 0), instance, value);
    }

    /**
     * Get the value of a field by its type. Return the first found. This method resolves the class.
     *
     * @param instance the instance
     * @param type     a string representing the type of the field
     * @param value    the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static void setFirstValueOfType(@Nonnull Object instance, @Nonnull String type, @Nonnull Object value)
    {
        fieldSet(countFieldOfType(instance.getClass(), getClass(type), 0), instance, value);
    }

    /**
     * Get the value of a field in a static way.
     *
     * @param clazz the class where the field is
     * @param field the name of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T getStaticValue(@Nonnull Class<?> clazz, @Nonnull String field)
    {
        return fieldGet(findField(clazz, field), null);
    }

    /**
     * Get the value of a field in a static way. This method resolves the class.
     *
     * @param clazz a string representing the clazz
     * @param field the name of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T getStaticValue(@Nonnull String clazz, @Nonnull String field)
    {
        return fieldGet(findField(getClass(clazz), field), null);
    }

    /**
     * Set the value of a field in a static way.
     *
     * @param clazz the class where the field is
     * @param field the name of the field
     * @param value the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static void setStaticField(@Nonnull Class<?> clazz, @Nonnull String field, @Nonnull Object value)
    {
        fieldSet(findField(clazz, field), null, value);
    }

    /**
     * Set the value of a field in a static way. This method resolves the class.
     *
     * @param clazz a string representing the clazz
     * @param field the name of the field
     * @param value the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static void setStaticField(@Nonnull String clazz, @Nonnull String field, @Nonnull Object value)
    {
        fieldSet(findField(getClass(clazz), field), null, value);
    }

    /**
     * Call a constructor.
     *
     * @param clazz    the class where the constructor is
     * @param argsType the args type of the constructor
     * @param args     the args of the constructor
     * @return the new instance
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T newInstance(@Nonnull Class<T> clazz, @Nonnull Class<?>[] argsType, @Nonnull Object[] args)
    {
        return constructorNewInstance(findConstructor(clazz, argsType), args);
    }

    /**
     * Call a constructor. This method resolves the class.
     *
     * @param clazz    a string representing the clazz
     * @param argsType the args type of the constructor
     * @param args     the args of the constructor
     * @return the new instance
     * @throws NMSReflectionException if any exception is thrown
     */
    public static <T> T newInstance(@Nonnull String clazz, @Nonnull Class<?>[] argsType, @Nonnull Object[] args)
    {
        return constructorNewInstance(findConstructor(getClass(clazz), argsType), args);
    }

    /**
     * Find a constructor with no arg and call it.
     *
     * @param clazz the class where the constructor is
     * @return the new instance
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T newInstance(@Nonnull Class<T> clazz)
    {
        return constructorNewInstance(findConstructor(clazz, EMPTY_CLASS_ARRAY), EMPTY_OBJECT_ARRAY);
    }

    /**
     * Find a constructor with no arg and call it. This method resolves the class.
     *
     * @param clazz a string representing the clazz
     * @return the new instance
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T newInstance(@Nonnull String clazz)
    {
        return constructorNewInstance(findConstructor(getClass(clazz), EMPTY_CLASS_ARRAY), EMPTY_OBJECT_ARRAY);
    }

    /**
     * <a href="http://stackoverflow.com/questions/195321/why-is-class-newinstance-evil">Evil</a>
     *
     * @param clazz the class to instanciate
     * @return the new instance
     * @throws NMSReflectionException if ANY exception is thrown
     */
    @SuppressWarnings({"ClassNewInstance", "OverlyBroadCatchBlock"})
    public static <T> T evilNewInstance(@Nonnull Class<T> clazz)
    {
        return classNewInstance(clazz);
    }

    /**
     * <a href="http://stackoverflow.com/questions/195321/why-is-class-newinstance-evil">Evil</a> This method resolves
     * the class.
     *
     * @param clazz a string representing the clazz
     * @return the new instance
     * @throws NMSReflectionException if ANY exception is thrown
     */
    public static <T> T evilNewInstance(@Nonnull String clazz)
    {
        return classNewInstance(getClass(clazz));
    }
}