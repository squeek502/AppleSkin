package squeek.appleskin.helpers;


import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.api.AppleSkinPlugin;

import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Ref: https://github.com/mezz/JustEnoughItems/blob/1.17/src/main/java/mezz/jei/util/AnnotatedInstanceUtil.java
public final class AnnotatedInstanceHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private AnnotatedInstanceHelper()
    {
    }

    public static List<AppleSkinApi> getModPlugins()
    {
        return getInstances(AppleSkinPlugin.class, AppleSkinApi.class);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> List<T> getInstances(Class<?> annotationClass, Class<T> instanceClass)
    {
        Type annotationType = Type.getType(annotationClass);
        List<ModFileScanData> allScanData = ModList.get().getAllScanData();
        Set<String> pluginClassNames = new LinkedHashSet<>();
        for (ModFileScanData scanData : allScanData)
        {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();
            for (ModFileScanData.AnnotationData a : annotations)
            {
                if (Objects.equals(a.getAnnotationType(), annotationType))
                {
                    String memberName = a.getMemberName();
                    pluginClassNames.add(memberName);
                }
            }
        }
        List<T> instances = new ArrayList<>();
        for (String className : pluginClassNames)
        {
            try
            {
                Class<?> asmClass = Class.forName(className);
                Class<? extends T> asmInstanceClass = asmClass.asSubclass(instanceClass);
                Constructor<? extends T> constructor = asmInstanceClass.getDeclaredConstructor();
                T instance = constructor.newInstance();
                instances.add(instance);
            }
            catch (ReflectiveOperationException | LinkageError e)
            {
                LOGGER.error("Failed to load: {}", className, e);
            }
        }
        return instances;
    }
}