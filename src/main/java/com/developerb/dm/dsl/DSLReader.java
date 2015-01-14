package com.developerb.dm.dsl;

import com.developerb.dm.domain.ContainerSource;
import com.developerb.dm.domain.ContainerSources;
import com.developerb.dm.dsl.hook.AfterBootHook;
import com.developerb.dm.dsl.hook.AfterRebootHook;
import com.developerb.dm.dsl.hook.BeforeRebootHook;
import com.github.dockerjava.api.DockerClient;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for turing a Groovy DSL file into a working model.
 */
public class DSLReader {

    private final DockerClient dockerClient;

    public DSLReader(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public ContainerSources load(CharSource scriptSource, File workingDirectory) {
        try {
            GroovyShell shell = createShell();
            return invokeScript(scriptSource, workingDirectory, shell);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to load from: " + scriptSource, ex);
        }
    }

    private GroovyShell createShell() {
        Map<String, Object> initialVariables = Maps.newHashMap();
        initialVariables.put("log", LoggerFactory.getLogger("groovy-dsl"));

        Binding initialBindings = new Binding(initialVariables);
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setScriptBaseClass(DSLBaseScriptBase.class.getCanonicalName());

        return new GroovyShell(initialBindings, configuration);
    }

    @SuppressWarnings("unchecked")
    private ContainerSources invokeScript(CharSource scriptSource, File workingDirectory, GroovyShell shell) throws IOException {
        try (Reader reader = scriptSource.openStream()) {
            Script script = shell.parse(reader);
            script.invokeMethod("init", new Object[] { dockerClient, workingDirectory });
            script.run();


            Set<ContainerSource> containerSources = invokeMethod(script, "containerSources", Set.class);
            AfterBootHook afterBootHook = invokeMethod(script, "afterBootHook", AfterBootHook.class);
            AfterRebootHook afterRebootHook = invokeMethod(script, "afterRebootHook", AfterRebootHook.class);
            BeforeRebootHook beforeRebootHook = invokeMethod(script, "beforeRebootHook", BeforeRebootHook.class);

            return new ContainerSources(containerSources, afterBootHook, afterRebootHook, beforeRebootHook);
        }
    }

    private <T> T invokeMethod(Script script, String methodName, Class<T> returnType) {
        Object returned = script.invokeMethod(methodName, new Object[]{});
        return returnType.cast(returned);
    }

}
