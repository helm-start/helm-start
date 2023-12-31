package com.start.helm.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Util class for Helm Charts.
 */
public final class HelmUtil {

	private HelmUtil() {
	}

	public static String removeMarkers(String content) {
		return Arrays.stream(content.split("\n"))
			.filter(s -> !s.contains("###@helm-start"))
			.collect(Collectors.joining("\n"));
	}

	public static Map<String, Object> makeSecretKeyRef(String name, String key, String appName) {
		return Map.of("name", name, "valueFrom",
				Map.of("secretKeyRef",
						Map.of("name", "{{ include \"REPLACEME.fullname\" . }}".replace("REPLACEME", appName), "key",
								key, "optional", false)));
	}

	public static Map<String, Object> initContainer(String appName, String dependencyName, String endpoint) {
		String name = "\"{{ include \"%s.fullname\" . }}-%schecker\"".formatted(appName, dependencyName);
		return Map.of("name", name, "image", "docker.io/busybox:stable", "imagePullPolicy", "Always", "securityContext",
				Map.of("allowPrivilegeEscalation", false, "runAsUser", 1000, "runAsGroup", 1000, "runAsNonRoot", true),
				"command", List.of("sh", "-c", """
						echo 'Waiting for %s to become ready...'
						until printf "." && nc -z -w 2 %s; do
						    sleep 2;
						done;
						echo '%s OK ✓'
						""".formatted(dependencyName, endpoint, dependencyName)), "resources", Map.of("requests",
						Map.of("cpu", "20m", "memory", "32Mi"), "limits", Map.of("cpu", "20m", "memory", "32Mi")));
	}

}
