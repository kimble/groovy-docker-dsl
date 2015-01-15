package com.developerb.dm;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.PrintStream;

import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.ansi;

/**
 *
 */
public class Console {

    private final String[] names;

    public Console(String... names) {
        this.names = names;
    }

    public Console subConsole(String name) {
        String[] allNames = new String[names.length + 1];
        System.arraycopy(names, 0, allNames, 0, names.length);
        allNames[names.length] = name;

        return new Console(allNames);
    }

    public void out(String message, Object... args) {
        AnsiConsole.systemInstall();
        printNames(System.out);

        String formatted = String.format(message, args);
        System.out.println(formatted);

        AnsiConsole.systemUninstall();
    }

    public void err(String message) {
        AnsiConsole.systemInstall();

        printNames(System.err);

        System.err.println(message);

        AnsiConsole.systemUninstall();
    }

    private void printNames(PrintStream stream) {
        for (String name : names) {
            stream.print(ansi()
                            .fg(BLUE)
                            .a(name)
                            .fg(MAGENTA)
                            .a(" > ")
                            .reset()
            );
        }
    }

}
