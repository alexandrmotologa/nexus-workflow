package com.engine.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NexusWorkflowApplication {

    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if ("--version".equalsIgnoreCase(arg) || "-v".equalsIgnoreCase(arg) || "version".equalsIgnoreCase(arg)) {
                    System.out.println("nexus-workflow version " + VERSION);
                    return;
                }
                if ("--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg) || "help".equalsIgnoreCase(arg)) {
                    printHelp();
                    return;
                }
            }
        }
        SpringApplication.run(NexusWorkflowApplication.class, args);
    }

    private static void printHelp() {
        System.out.println("NexusWorkflow - Resilient distributed workflow & DAG engine built on Java 21");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar nexus-workflow.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -v, --version, version    Print version information and exit");
        System.out.println("  -h, --help, help          Print this help message and exit");
    }
}
