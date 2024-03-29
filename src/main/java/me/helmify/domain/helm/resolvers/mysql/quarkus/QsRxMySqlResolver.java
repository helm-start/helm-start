package me.helmify.domain.helm.resolvers.mysql.quarkus;

import me.helmify.domain.helm.HelmContext;
import me.helmify.domain.helm.resolvers.mysql.MySqlResolver;
import me.helmify.util.HelmUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QsRxMySqlResolver implements MySqlResolver {

	@Override
	public List<String> matchOn() {
		return List.of("quarkus-reactive-mysql-client");
	}

	public List<Map<String, Object>> getEnvironmentEntries(HelmContext context) {
		return List.of(HelmUtil.makeSecretKeyRef("QUARKUS_DATASOURCE_USERNAME", "mysql-username", context.getAppName()),
				HelmUtil.makeSecretKeyRef("QUARKUS_DATASOURCE_PASSWORD", "mysql-password", context.getAppName()));
	}

	public Map<String, Object> getSecretEntries() {
		return Map.of("mysql-username", "{{ .Values.mysql.auth.username | b64enc | quote }}", "mysql-password",
				"{{ .Values.mysql.auth.password | b64enc | quote }}");
	}

	public Map<String, String> getDefaultConfig() {
		return Map.of("%prod.quarkus.datasource.reactive.url",
				"vertx-reactive:mysql://{{ .Values.global.hosts.mysql }}:{{ .Values.global.ports.mysql }}/{{ .Values.mysql.database }}");
	}

}
