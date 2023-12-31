package com.start.helm.domain.helm.chart;

import com.start.helm.TestUtil;
import com.start.helm.domain.helm.HelmContext;
import com.start.helm.domain.maven.MavenModelParser;
import com.start.helm.domain.maven.MavenModelProcessor;
import org.apache.maven.api.model.Model;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HelmChartServiceTest {

	@Autowired
	private HelmChartService service;

	@Autowired
	private MavenModelProcessor mavenModelProcessor;

	@Test
	void process() throws Exception {
		String filename = "pom-with-all.xml";
		Optional<Model> model = MavenModelParser
			.parsePom(TestUtil.inputStreamToString(getClass().getClassLoader().getResourceAsStream(filename)));
		assertTrue(model.isPresent());
		Model m = model.get();
		HelmContext context = mavenModelProcessor.process(m);

		context.setAppName("test");
		context.setAppVersion("1.0.0");

		assertNotNull(context);
		int expectedSlices = 9;
		assertEquals(expectedSlices, context.getHelmChartSlices().size());
		assertEquals(expectedSlices - 1, context.getValuesGlobalBlocks().size());
		assertEquals(expectedSlices - 1, context.getHelmDependencies().size());
		assertTrue(context.isHasActuator());
		assertTrue(context.isCreateIngress());

		boolean kafka = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("kafka"));

		boolean neo4j = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("neo4j"));

		boolean mariadb = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("mariadb"));

		boolean mongodb = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("mongodb"));
		boolean mysql = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("mysql"));
		boolean redis = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("redis"));
		boolean rabbitmq = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("rabbitmq"));
		boolean postgresql = context.getHelmChartSlices()
			.stream()
			.anyMatch(s -> s.getValuesEntries().keySet().contains("postgresql"));

		assertTrue(kafka);
		assertTrue(mysql);
		assertTrue(rabbitmq);
		assertTrue(postgresql);
		assertTrue(redis);
		assertTrue(mongodb);
		assertTrue(mariadb);
		assertTrue(neo4j);

		context.getHelmChartSlices()
			.stream()
			.filter(slice -> slice.getPreferredChart() != null)
			.forEach(s -> assertAll(() -> assertNotNull(s.getPreferredChart()),
					() -> assertNotNull(s.getValuesEntries()), () -> assertNotNull(s.getValuesEntries().get("global")),
					() -> assertNotNull(s.getSecretEntries()), () -> assertNotNull(s.getDefaultConfig()),
					() -> assertNotNull(s.getEnvironmentEntries()), () -> assertNotNull(s.getInitContainer())));

		context.setCustomizations(new HelmContext.HelmContextCustomization("test", "latest", null, Map.of()));
		context.setCustomized(true);

		byte[] process = service.process(context);
		Files.write(Paths.get("helm.zip"), process);
		File helmFile = new File("helm.zip");
		ZipFile zipFile = new ZipFile(helmFile);
		ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(process));
		Map<String, String> contents = new HashMap<>();
		List<String> names = new ArrayList<>();
		ZipEntry entry;
		while ((entry = zipInputStream.getNextEntry()) != null) {
			String name = entry.getName();
			names.add(name);
			InputStream inputStream = zipFile.getInputStream(entry);
			byte[] bytes = inputStream.readAllBytes();
			contents.put(name, new String(bytes));
		}

		assertAll(() -> assertTrue(names.contains("templates/")), () -> assertTrue(names.contains("Chart.yaml")),
				() -> assertTrue(names.contains("templates/configmap.yaml")),
				() -> assertTrue(names.contains("templates/deployment.yaml")),
				() -> assertTrue(names.contains("templates/_helpers.tpl")),
				() -> assertTrue(names.contains("templates/hpa.yaml")), () -> assertTrue(names.contains(".helmignore")),
				() -> assertTrue(names.contains("README.MD")),
				() -> assertTrue(names.contains("templates/ingress.yaml")),
				() -> assertTrue(names.contains("templates/NOTES.txt")),
				() -> assertTrue(names.contains("templates/secrets.yaml")),
				() -> assertTrue(names.contains("templates/serviceaccount.yaml")),
				() -> assertTrue(names.contains("templates/service.yaml")),
				() -> assertTrue(names.contains("values.yaml")));

		assertTrue(contents.keySet().containsAll(names));

		checkConfigMapYaml(contents);
		checkDeploymentYaml(contents);
		checkChartYaml(context, contents);
	}

	private static void checkDeploymentYaml(Map<String, String> contents) {
		String deploymentYaml = contents.get("templates/deployment.yaml");

		// look for init containers
		assertTrue(deploymentYaml.contains("-neo4jchecker"));
		assertTrue(deploymentYaml.contains("-mariadbchecker"));
		assertTrue(deploymentYaml.contains("-mongodbchecker"));
		assertTrue(deploymentYaml.contains("-rabbitmqchecker"));
		assertTrue(deploymentYaml.contains("-postgresqlchecker"));
		assertTrue(deploymentYaml.contains("-redischecker"));
		assertTrue(deploymentYaml.contains("-mysqlchecker"));
		assertTrue(deploymentYaml.contains("-kafkachecker"));

		// look for env vars
		assertTrue(deploymentYaml.contains("name: SPRING_NEO4J_AUTHENTICATION_USERNAME"));
		assertTrue(deploymentYaml.contains("key: neo4j-username"));
		assertTrue(deploymentYaml.contains("name: SPRING_NEO4J_AUTHENTICATION_PASSWORD"));
		assertTrue(deploymentYaml.contains("key: neo4j-password"));

		assertTrue(deploymentYaml.contains("name: SPRING_RABBITMQ_USERNAME"));
		assertTrue(deploymentYaml.contains("key: rabbitmq-username"));
		assertTrue(deploymentYaml.contains("name: SPRING_RABBITMQ_PASSWORD"));
		assertTrue(deploymentYaml.contains("key: rabbitmq-password"));

		assertTrue(deploymentYaml.contains("name: SPRING_DATA_REDIS_PASSWORD"));
		assertTrue(deploymentYaml.contains("key: redis-password"));

		assertTrue(deploymentYaml.contains("name: SPRING_DATASOURCE_USERNAME"));
		assertTrue(deploymentYaml.contains("key: postgres-username") || deploymentYaml.contains("key: mysql-username")
				|| deploymentYaml.contains("key: mariadb-username"));
		assertTrue(deploymentYaml.contains("name: SPRING_DATASOURCE_PASSWORD"));
		assertTrue(deploymentYaml.contains("key: postgres-password") || deploymentYaml.contains("key: mysql-password")
				|| deploymentYaml.contains("key: mariadb-password"));

		assertTrue(deploymentYaml.contains("name: SPRING_DATA_MONGODB_USERNAME"));
		assertTrue(deploymentYaml.contains("key: mongodb-username"));
		assertTrue(deploymentYaml.contains("name: SPRING_DATA_MONGODB_PASSWORD"));
		assertTrue(deploymentYaml.contains("key: mongodb-password"));

		// look for probes
		assertTrue(deploymentYaml.contains("readinessProbe"));
		assertTrue(deploymentYaml.contains("livenessProbe"));
		assertTrue(deploymentYaml.contains("path: /actuator/health/liveness"));
		assertTrue(deploymentYaml.contains("path: /actuator/health/readiness"));

		// look for lifecycle hook
		assertTrue(deploymentYaml.contains("preStop:"));
		assertTrue(deploymentYaml.contains("command: [\"sh\", \"-c\", \"sleep 10\"]"));

		// look for config volume mount
		assertTrue(deploymentYaml.contains("mountPath: /workspace/BOOT-INF/classes/application.properties"));

	}

	private static void checkConfigMapYaml(Map<String, String> contents) {
		String configmapYaml = contents.get("templates/configmap.yaml");
		assertTrue(configmapYaml.contains("  application.properties: |-"));
		assertTrue(configmapYaml.contains("    spring.application.name="));
		assertTrue(configmapYaml.contains("    spring.rabbitmq.virtual-host="));
		assertTrue(configmapYaml.contains("    spring.rabbitmq.host="));
		assertTrue(configmapYaml.contains("    spring.rabbitmq.port="));
		assertTrue(configmapYaml.contains("    spring.datasource.url="));
		assertTrue(configmapYaml.contains("    spring.data.redis.host="));
		assertTrue(configmapYaml.contains("    spring.data.redis.port="));
		assertTrue(configmapYaml.contains("    spring.data.mongodb.uri="));
		assertTrue(configmapYaml.contains("    spring.neo4j.uri="));
		assertTrue(configmapYaml.contains("    spring.kafka.bootstrap-servers="));

	}

	private static void checkChartYaml(HelmContext context, Map<String, String> contents) {
		String chartYaml = contents.get("Chart.yaml");
		assertTrue(chartYaml.contains("name: " + context.getAppName()));
		assertTrue(chartYaml.contains("appVersion: " + context.getAppVersion()));
		assertTrue(chartYaml.contains("type: application"));
		assertTrue(chartYaml.contains("version: 0.1.0"));
	//@formatter:off
    assertTrue(chartYaml.contains("""
          - name: postgresql
            version: 11.9.2
            repository: https://charts.bitnami.com/bitnami
            condition: postgresql.enabled
            tags: []
        """));
    //@formatter:off
    assertTrue(chartYaml.contains("""
          - name: rabbitmq
            version: 11.9.0
            repository: https://charts.bitnami.com/bitnami
            condition: rabbitmq.enabled
            tags: []
        """));
    //@formatter:off
    assertTrue(chartYaml.contains("""
          - name: redis
            version: 18.1.2
            repository: https://charts.bitnami.com/bitnami
            condition: redis.enabled
            tags: []
        """));
		//@formatter:off
		assertTrue(chartYaml.contains("""
          - name: neo4j
            version: 5.12.0
            repository: https://helm.neo4j.com/neo4j
            condition: neo4j.enabled
            tags: []
        """));
		//@formatter:off
		assertTrue(chartYaml.contains("""
          - name: kafka
            version: 26.4.2
            repository: https://charts.bitnami.com/bitnami
            condition: kafka.enabled
            tags: []
        """));
  }
}
