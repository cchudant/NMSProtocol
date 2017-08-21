package me.skybeast.nmsprotocol;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A class to use Reflection in Minecraft easily.
 * <p>
 * Idea taken from
 * {@link <a href="https://github.com/aadnk/ProtocolLib/blob/master/modules/TinyProtocol/src/main/java/com/comphenix/tinyprotocol/Reflection.java">TinyProtocol</a>}
 *
 * @author SkyBeast
 */
@SuppressWarnings("unused")
public final class NMSReflection
{

	/*
     * PACKAGE
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
    public static <T> FieldAccessor<T> getFieldAccessor(Class<?> clazz, String name)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(name);

        try
        {
            return getFieldAccessor(clazz.getDeclaredField(name));
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get a FieldAccessor by its class and name. This method resolves the clazz.
     *
     * @param clazz a string representing the clazz
     * @param name  the name of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> FieldAccessor<T> getFieldAccessor(String clazz, String name)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(name);

        try
        {
            return getFieldAccessor(getClass(clazz).getDeclaredField(name));
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get a FieldAccessor by its Field.
     *
     * @param field the field to hook to
     * @return a FieldAccessor holding the Field
     */
    public static <T> FieldAccessor<T> getFieldAccessor(Field field)
    {
        Objects.requireNonNull(field);

        field.setAccessible(true); // Disable Accessible check -- Faster

        return () -> field;
    }

    /*
     * Used to search in parent classes
     */
    @SuppressWarnings("unchecked")
    private static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(Class<?> clazz, Class<?> type, int count,
                                                                    Class<?> search)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(type);

        int i = 0;
        for (Field field : search.getDeclaredFields())
        {
            if (field.getType() == type)
            {
                if (i == count)
                {
                    field.setAccessible(true);
                    return () -> field;
                }
                i++;
            }
        }

        Class<?> superClass = search.getSuperclass();
        if (superClass != null)
            return getCountFieldOfTypeAccessor(clazz, type, count, superClass);

        throw new NMSReflectionException("Cannot find a field with type " + type + " in " + clazz + '.');
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found.
     *
     * @param clazz the class of the field
     * @param type  the type of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(Class<?> clazz, Class<?> type)
    {
        return getCountFieldOfTypeAccessor(clazz, type, 0, clazz);
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found. This method resolves the type.
     *
     * @param clazz the class of the field
     * @param type  a string representing the type of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(Class<?> clazz, String type)
    {
        return getCountFieldOfTypeAccessor(clazz, getClass(type), 0, clazz);
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found. This method resolves the class.
     *
     * @param clazz a string representing the class
     * @param type  the class of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(String clazz, Class<?> type)
    {
        Class<?> c = getClass(clazz);
        return getCountFieldOfTypeAccessor(c, type, 0, c);
    }

    /**
     * Find a field accessor by its clazz and type. Return the first found. This method resolves the classes.
     *
     * @param clazz a string representing the class
     * @param type  a string representing the type of the field
     * @return a FieldAccessor holding the Field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> FieldAccessor<T> getFirstFieldOfTypeAccessor(String clazz, String type)
    {
        Class<?> c = getClass(clazz);
        return getCountFieldOfTypeAccessor(c, getClass(type), 0, c);
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
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(Class<?> clazz, Class<?> type, int count)
    {
        return getCountFieldOfTypeAccessor(clazz, type, count, clazz);
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
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(Class<?> clazz, String type, int count)
    {
        return getCountFieldOfTypeAccessor(clazz, getClass(type), count, clazz);
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
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(String clazz, Class<?> type, int count)
    {
        Class<?> c = getClass(clazz);
        return getCountFieldOfTypeAccessor(c, type, count, c);
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
    public static <T> FieldAccessor<T> getCountFieldOfTypeAccessor(String clazz, String type, int count)
    {
        Class<?> c = getClass(clazz);
        return getCountFieldOfTypeAccessor(c, getClass(type), count, c);
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
        @SuppressWarnings("unchecked")
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
         * Get the return type of the method.
         *
         * @return the return type of the method
         */
        @SuppressWarnings("unchecked")
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
    public static <T> MethodAccessor<T> getMethodAccessor(Class<?> clazz, String name, Class<?>... args)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(name);
        Objects.requireNonNull(args);

        try
        {
            return getMethodAccessor(clazz.getDeclaredMethod(name, args));
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
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
    public static <T> MethodAccessor<T> getMethodAccessor(String clazz, String name, Class<?>... args)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(name);
        Objects.requireNonNull(args);

        try
        {
            return getMethodAccessor(getClass(clazz).getDeclaredMethod(name, args));
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get a MethodAccessor by its Method.
     *
     * @param method the method to hook to
     * @return a MethodAccessor holding the Method
     */
    public static <T> MethodAccessor<T> getMethodAccessor(Method method)
    {
        Objects.requireNonNull(method);

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
    public static <T> ConstructorAccessor<T> getConstructorAccessor(Class<T> clazz, Class<?>... args)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(args);

        try
        {
            return getConstructorAccessor(clazz.getDeclaredConstructor(args));
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get a ConstructorAccessor by its class and parameters. This method resolves the clazz.
     *
     * @param clazz a string representing the clazz
     * @param args  the parameters of the constructor
     * @return a ConstructorAccessor holding the Constructor
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    @SuppressWarnings("unchecked")
    public static <T> ConstructorAccessor<T> getConstructorAccessor(String clazz, Class<?>... args)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(args);

        try
        {
            return (ConstructorAccessor<T>) getConstructorAccessor(getClass(clazz).getDeclaredConstructor(args));
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get a ConstructorAccessor by its Method.
     *
     * @param constructor the constructor to hook to
     * @return a ConstructorAccessor holding the Constructor
     */
    public static <T> ConstructorAccessor<T> getConstructorAccessor(Constructor<T> constructor)
    {
        Objects.requireNonNull(constructor);

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
    @SuppressWarnings({"unchecked", "DynamicRegexReplaceableByCompiledPattern"})
    public static <T> Class<T> getClass(String clazz)
    {
        Objects.requireNonNull(clazz);

        try
        {
            return (Class<T>) Class
                    .forName(clazz.replace("{nms}", NMS).replace("{cb}", CB).replace("{version}", VERSION));
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get a NMS class by its simple name.
     *
     * @param clazz the simple name of the class
     * @return the class
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getNMSClass(String clazz)
    {
        Objects.requireNonNull(clazz);

        try
        {
            return (Class<T>) Class.forName(NMS + '.' + clazz);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get a CB class by its name without the CB prefix.
     *
     * @param clazz the simple name of the class
     * @return the class
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getCBClass(String clazz)
    {
        Objects.requireNonNull(clazz);

        try
        {
            return (Class<T>) Class.forName(CB + '.' + clazz);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get the name of the package of a class.
     *
     * @param clazz the class
     * @return the name of the package
     */
    public static String getPackage(String clazz)
    {
        Objects.requireNonNull(clazz);

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
    @SuppressWarnings("unchecked")
    public static <T> T callMethod(Object instance, String method, Class<?>[] argsType, Object[] args)
    {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(method);
        Objects.requireNonNull(argsType);
        Objects.requireNonNull(args);

        try
        {
            Method reflect = instance.getClass().getDeclaredMethod(method, argsType);
            reflect.setAccessible(true);
            return (T) reflect.invoke(instance, args);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
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
    @SuppressWarnings("unchecked")
    public static <T> T callStaticMethod(Class<?> clazz, String method, Class<?>[] argsType, Object[] args)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(method);
        Objects.requireNonNull(argsType);
        Objects.requireNonNull(args);

        try
        {
            Method reflect = clazz.getDeclaredMethod(method, argsType);
            reflect.setAccessible(true);
            return (T) reflect.invoke(null, args);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
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
    public static <T> T callStaticMethod(String clazz, String method, Class<?>[] argsType, Object[] args)
    {
        return callStaticMethod(getClass(clazz), method, argsType, args);
    }

    /**
     * Get the value of a field in a non-static way.
     *
     * @param instance the instance
     * @param field    the name of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object instance, String field)
    {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(field);

        try
        {
            Field reflect = instance.getClass().getDeclaredField(field);
            reflect.setAccessible(true);
            return (T) reflect.get(instance);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Used to search in parent classes
     */
    @SuppressWarnings("unchecked")
    private static <T> T getCountValueOfType(Object instance, Class<?> type, int count, Class<?> search)
    {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(type);

        try
        {
            int i = 0;
            for (Field field : search.getDeclaredFields())
            {
                if (field.getType() == type)
                {
                    if (i == count)
                    {
                        field.setAccessible(true);
                        return (T) field.get(instance);
                    }
                    i++;
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }

        Class<?> superClass = search.getSuperclass();
        if (superClass != null)
            return getCountValueOfType(instance, type, count, superClass);

        throw new NMSReflectionException("Cannot find a field with type " + type + " in " + instance
                .getClass() + '.');
    }

    /**
     * Get the value of a field by its type. Return the first found.
     *
     * @param instance the instance
     * @param type     the type of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> T getFirstValueOfType(Object instance, Class<?> type)
    {
        return getCountValueOfType(instance, type, 0, instance.getClass());
    }

    /**
     * Get the value of a field by its type. Return the first found. This method resolves the class.
     *
     * @param instance the instance
     * @param type     a string representing the type of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static <T> T getFirstValueOfType(Object instance, String type)
    {
        return getCountValueOfType(instance, getClass(type), 0, instance.getClass());
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
    public static <T> T getCountValueOfType(Object instance, Class<?> type, int count)
    {
        return getCountValueOfType(instance, type, count, instance.getClass());
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
    public static <T> T getCountValueOfType(Object instance, String type, int count)
    {
        return getCountValueOfType(instance, getClass(type), count, instance.getClass());
    }

    /**
     * Set the value of a field in a non-static way.
     *
     * @param instance the instance
     * @param field    the name of the field
     * @param value    the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static void setField(Object instance, String field, Object value)
    {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(field);

        try
        {
            Field reflect = instance.getClass().getDeclaredField(field);
            reflect.setAccessible(true);
            reflect.set(instance, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /*
     * Used to search in parent classes
     */
    @SuppressWarnings("unchecked")
    private static void setCountValueOfType(Object instance, Class<?> type, int count, Object value, Class<?> search)
    {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(type);

        try
        {
            int i = 0;
            for (Field field : search.getDeclaredFields())
            {
                if (field.getType() == type)
                {
                    if (i == count)
                    {
                        field.setAccessible(true);
                        field.set(instance, value);
                    }
                    i++;
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }

        Class<?> superClass = search.getSuperclass();
        if (superClass != null)
            setCountValueOfType(instance, type, count, value, superClass);
        else
            throw new NMSReflectionException("Cannot find a field with type " + type + " in " + instance
                    .getClass() + '.');
    }

    /**
     * Get the value of a field by its type. Return the first found.
     *
     * @param instance the instance
     * @param type     the type of the field
     * @param value    the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static void setFirstValueOfType(Object instance, Class<?> type, Object value)
    {
        setCountValueOfType(instance, type, 0, value, instance.getClass());
    }

    /**
     * Get the value of a field by its type. Return the first found. This method resolves the class.
     *
     * @param instance the instance
     * @param type     a string representing the type of the field
     * @param value    the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown or if the field was not found
     */
    public static void setFirstValueOfType(Object instance, String type, Object value)
    {
        setCountValueOfType(instance, getClass(type), 0, value, instance.getClass());
    }

    /**
     * Get the value of a field in a static way.
     *
     * @param clazz the class where the field is
     * @param field the name of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    @SuppressWarnings("unchecked")
    public static <T> T getStaticValue(Class<?> clazz, String field)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(field);

        try
        {
            Field reflect = clazz.getDeclaredField(field);
            reflect.setAccessible(true);
            return (T) reflect.get(null);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Get the value of a field in a static way. This method resolves the class.
     *
     * @param clazz a string representing the clazz
     * @param field the name of the field
     * @return the value of the field
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T getStaticValue(String clazz, String field)
    {
        return getStaticValue(getClass(clazz), field);
    }

    /**
     * Set the value of a field in a static way.
     *
     * @param clazz the class where the field is
     * @param field the name of the field
     * @param value the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static void setStaticField(Class<?> clazz, String field, Object value)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(field);

        try
        {
            Field reflect = clazz.getDeclaredField(field);
            reflect.setAccessible(true);
            reflect.set(null, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * Set the value of a field in a static way. This method resolves the class.
     *
     * @param clazz a string representing the clazz
     * @param field the name of the field
     * @param value the value to set
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static void setStaticField(String clazz, String field, Object value)
    {
        setStaticField(getClass(clazz), field, value);
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
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> clazz, Class<?>[] argsType, Object[] args)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(argsType);
        Objects.requireNonNull(args);

        try
        {
            Constructor<?> reflect = clazz.getDeclaredConstructor(argsType);
            reflect.setAccessible(true);
            return (T) reflect.newInstance(args);
        }
        catch (ReflectiveOperationException e)
        {
            throw new NMSReflectionException(e);
        }
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
    public static <T> T newInstance(String clazz, Class<?>[] argsType, Object[] args)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(argsType);
        Objects.requireNonNull(args);

        return newInstance(getClass(clazz), argsType, args);
    }

    /**
     * Find a constructor with no arg and call it.
     *
     * @param clazz the class where the constructor is
     * @return the new instance
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> clazz)
    {
        return newInstance(clazz, EMPTY_CLASS_ARRAY, EMPTY_OBJECT_ARRAY);
    }

    /**
     * Find a constructor with no arg and call it. This method resolves the class.
     *
     * @param clazz a string representing the clazz
     * @return the new instance
     * @throws NMSReflectionException if any non-runtime exception is thrown
     */
    public static <T> T newInstance(String clazz)
    {
        return newInstance(getClass(clazz));
    }

    /**
     * {@link <a href="http://stackoverflow.com/questions/195321/why-is-class-newinstance-evil">Evil</a>}
     *
     * @param clazz the class to instanciate
     * @return the new instance
     * @throws NMSReflectionException if ANY exception is thrown
     */
    @SuppressWarnings({"unchecked", "ClassNewInstance"})
    public static <T> T evilNewInstance(Class<?> clazz)
    {
        Objects.requireNonNull(clazz);

        try
        {
            return (T) clazz.newInstance(); // Propagate any exception if found
            // -- EVIL
        }
        catch (Exception e)
        {
            throw new NMSReflectionException(e);
        }
    }

    /**
     * {@link <a href="http://stackoverflow.com/questions/195321/why-is-class-newinstance-evil">Evil</a>} This method
     * resolves the class.
     *
     * @param clazz a string representing the clazz
     * @return the new instance
     * @throws NMSReflectionException if ANY exception is thrown
     */
    public static <T> T evilNewInstance(String clazz)
    {
        return evilNewInstance(getClass(clazz));
    }
}