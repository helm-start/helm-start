package com.start.helm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.util.List;

import static com.start.helm.util.TestUtil.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HelmChartTests {

	MockMvc mvc;

	@LocalServerPort
	int port;

	@Autowired
	WebApplicationContext ctx;

	@BeforeEach
	void before() {
		this.mvc = MockMvcBuilders.webAppContextSetup(ctx).alwaysDo(MockMvcResultHandlers.print()).build();
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(HelmChartTests.class);

	record HelmUnittestContext(String chartName, String chartVersion, List<String> dependencies,
			String unittestSourceDirectory, List<String> unittestFiles) {
	}

	void lintAndTestChart(HelmUnittestContext context) {
		// generate chart
		File chartDirectory = downloadStarter(this.mvc, context.chartName, context.chartVersion, context.dependencies);

		// lint chart
		GenericContainer<?> lintContainer = new GenericContainer<>("alpine/helm:3.11.1")
			.withCopyToContainer(MountableFile.forHostPath(chartDirectory.toPath(), 0777), "/apps")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER))
			.withCommand("lint");
		lintContainer.start();
		waitForCondition(() -> !lintContainer.isRunning(), 10);
		Assertions.assertTrue(lintContainer.getLogs().contains("0 chart(s) failed"));

		// copy helm unittest files to helmchart/tests directory
		addHelmUnittestFiles(chartDirectory, context.unittestSourceDirectory, context.unittestFiles);
		addHelmUnittestValues(chartDirectory);

		// helm-unittest chart
		GenericContainer<?> unittestContainer = new GenericContainer<>("helmunittest/helm-unittest:latest")
			.withCopyToContainer(MountableFile.forHostPath(chartDirectory.toPath(), 0777), "/apps")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER))
			.withCommand("-o /apps/test-output.xml -t junit .".split(" "));
		unittestContainer.start();
		waitForCondition(() -> !unittestContainer.isRunning(), 10);
		Assertions.assertFalse(unittestContainer.getLogs().contains("exited with error"));
	}

	@Test
	public void testChartWithPostgres() {

		List<String> starterDependencies = List.of("postgresql");
		//@formatter:off
        List<String> unittestFiles = List.of(
                "deployment_postgres_test.yaml",
                "service_postgres_test.yaml",
                "configmap_postgres_test.yaml",
                "secrets_postgres_test.yaml"
        );

        lintAndTestChart(
            new HelmUnittestContext(
                "test-postgres-chart",
                "1.0.0",
                starterDependencies,
                "postgres",
                unittestFiles)
        );
    }

}
