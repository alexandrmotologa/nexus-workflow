package com.engine.nexus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class NexusWorkflowApplicationTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Should print version with --version")
    void shouldPrintVersionLongFlag() {
        NexusWorkflowApplication.main(new String[]{"--version"});
        assertThat(outContent.toString()).contains("nexus-workflow version 1.0.0");
    }

    @Test
    @DisplayName("Should print version with -v")
    void shouldPrintVersionShortFlag() {
        NexusWorkflowApplication.main(new String[]{"-v"});
        assertThat(outContent.toString()).contains("nexus-workflow version 1.0.0");
    }

    @Test
    @DisplayName("Should print version with version subcommand")
    void shouldPrintVersionCommand() {
        NexusWorkflowApplication.main(new String[]{"version"});
        assertThat(outContent.toString()).contains("nexus-workflow version 1.0.0");
    }

    @Test
    @DisplayName("Should print help with --help")
    void shouldPrintHelpLongFlag() {
        NexusWorkflowApplication.main(new String[]{"--help"});
        assertThat(outContent.toString()).contains("Usage:").contains("-v, --version, version");
    }

    @Test
    @DisplayName("Should print help with -h")
    void shouldPrintHelpShortFlag() {
        NexusWorkflowApplication.main(new String[]{"-h"});
        assertThat(outContent.toString()).contains("Usage:").contains("-v, --version, version");
    }
}
