package me.helmify.kind;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class KindClusterTest {

	private List<String> getProperty(String propertyName) {
		String property = System.getProperty(propertyName);
		if (property == null || property.isEmpty()) {
			return List.of();
		}
		return property.contains(",") ? List.of(property.split(",")) : List.of(property);
	}

	private boolean checkIfAllPresent(List<String> actual, List<String> expected) {
		for (String expectedItem : expected) {
			boolean found = actual.stream().anyMatch(actualItem -> actualItem.contains(expectedItem));
			if (!found) {
				return false;
			}
		}

		return true;
	}

	@Test
	public void testCluster() {

		// only execute when explicitly called
		String property = System.getProperty("test");
		if (property == null || property.isEmpty() || !"KindClusterTest".equals(property)) {
			return;
		}

		List<String> expectedDeployments = getProperty("expected-deployments");
		List<String> expectedServices = getProperty("expected-services");
		List<String> expectedPods = getProperty("expected-pods");
		List<String> expectedConfigs = getProperty("expected-configs");
		List<String> expectedSecrets = getProperty("expected-secrets");
		List<String> expectedVolumes = getProperty("expected-volumes");

		try (KubernetesClient client = new DefaultKubernetesClient()) {

			List<String> actualServices = client.services()
				.list()
				.getItems()
				.stream()
				.map(service -> service.getMetadata().getName())
				.toList();
			System.out.println("Services: " + actualServices);

			boolean allExpectedServicesPresent = checkIfAllPresent(actualServices, expectedServices);
			Assertions.assertTrue(allExpectedServicesPresent, "Not all expected services found: " + expectedServices);

			System.out.println("-----------------------------------");
			List<String> actualPods = client.pods()
				.list()
				.getItems()
				.stream()
				.map(pod -> pod.getMetadata().getName())
				.toList();
			System.out.println("Pods: " + actualPods);
			boolean allExpectedPodsPresent = checkIfAllPresent(actualPods, expectedPods);
			Assertions.assertTrue(allExpectedPodsPresent, "Not all expected pods found: " + expectedPods);

			System.out.println("-----------------------------------");
			List<String> actualDeployments = client.apps()
				.deployments()
				.list()
				.getItems()
				.stream()
				.map(deployment -> deployment.getMetadata().getName())
				.toList();
			System.out.println("Deployments: " + actualDeployments);
			boolean allExpectedDeploymentsPresent = checkIfAllPresent(actualDeployments, expectedDeployments);
			Assertions.assertTrue(allExpectedDeploymentsPresent,
					"Not all expected deployments found: " + expectedDeployments);

			System.out.println("-----------------------------------");

			List<String> actualSecrets = client.secrets()
				.list()
				.getItems()
				.stream()
				.map(secret -> secret.getMetadata().getName())
				.toList();
			System.out.println("Secrets: " + actualSecrets);
			boolean allExpectedSecretsPresent = checkIfAllPresent(actualSecrets, expectedSecrets);
			Assertions.assertTrue(allExpectedSecretsPresent, "Not all expected secrets found: " + expectedSecrets);
			System.out.println("-----------------------------------");

			List<String> actualConfigMaps = client.configMaps()
				.list()
				.getItems()
				.stream()
				.map(configMap -> configMap.getMetadata().getName())
				.toList();
			System.out.println("ConfigMaps: " + actualConfigMaps);
			boolean allExpectedConfigsPresent = checkIfAllPresent(actualConfigMaps, expectedConfigs);
			Assertions.assertTrue(allExpectedConfigsPresent, "Not all expected configMaps found: " + expectedConfigs);
			System.out.println("-----------------------------------");

			List<String> actualVolumes = client.persistentVolumes()
				.list()
				.getItems()
				.stream()
				.map(volume -> volume.getMetadata().getName())
				.toList();
			System.out.println("Volumes: " + actualVolumes);

		}
		catch (Exception e) {
			log.error("Error getting cluster info", e);
		}
	}

}
