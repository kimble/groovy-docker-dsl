package com.developerb.dm.dsl;

import com.developerb.dm.domain.ContainerSource;
import com.developerb.dm.dsl.hook.AfterBootHook;
import com.developerb.dm.dsl.hook.AfterRebootHook;
import com.developerb.dm.dsl.hook.BeforeRebootHook;
import com.github.dockerjava.api.DockerClient;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

import static groovy.lang.Closure.DELEGATE_FIRST;

/**
 * Groovy DSL will inherit from this class
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class DSLBaseScriptBase extends Script {

    private final static Logger log = LoggerFactory.getLogger(DSLBaseScriptBase.class);


    private final Set<ContainerSource> containerSources = Sets.newHashSet();
    private AfterBootHook afterBootHook = new AfterBootHook();
    private AfterRebootHook afterRebootHook = new AfterRebootHook();
    private BeforeRebootHook beforeRebootHook = new BeforeRebootHook();

    private DockerClient client;
    private File workingDirectory;

    public void init(DockerClient client, File workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.client = client;
    }

    void dockerfiles(Closure<?> definition) {
        DockerfilesDelegate delegate = new DockerfilesDelegate(client, workingDirectory);
        definition.setResolveStrategy(DELEGATE_FIRST);
        definition.setDelegate(delegate);
        definition.run();

        containerSources.addAll(delegate.tasks());
    }

    void afterBoot(Closure<?> definition) {
        afterBootHook = new AfterBootHook(definition);
    }

    void afterReboot(Closure<?> definition) {
        afterRebootHook = new AfterRebootHook(definition);
    }

    void beforeReboot(Closure<?> definition) {
        beforeRebootHook = new BeforeRebootHook(definition);
    }

    public Set<ContainerSource> containerSources() {
        return containerSources;
    }

    public AfterBootHook afterBootHook() {
        return afterBootHook;
    }

    public AfterRebootHook afterRebootHook() {
        return afterRebootHook;
    }

    public BeforeRebootHook beforeRebootHook() {
        return beforeRebootHook;
    }

    public void notifySend(String message) {
        try {
            int exitCode = new ProcessBuilder("notify-send", message)
                    .start()
                    .waitFor();

            if (exitCode != 0) {
                log.warn("Failed to send notification '{}', notify-send returned {}", message, exitCode);
            }
        }
        catch (Exception ex) {
            log.warn("Failed to send notification: '{}'", message, ex);
        }
    }

}
