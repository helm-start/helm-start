package me.helmify.domain.helm.chart.providers;

import lombok.RequiredArgsConstructor;
import me.helmify.domain.helm.HelmContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelmServiceAccountYamlProvider implements HelmFileProvider {

	private static final String template = """
			{{- if .Values.serviceAccount.create -}}
			apiVersion: v1
			kind: ServiceAccount
			metadata:
			  name: {{ include "%s.serviceAccountName" . }}
			  labels:
			    {{- include "%s.labels" . | nindent 4 }}
			  {{- with .Values.serviceAccount.annotations }}
			  annotations:
			    {{- toYaml . | nindent 4 }}
			  {{- end }}
			{{- end }}
			""";

	@Override
	public String getFileContent(HelmContext context) {
		return String.format(template, context.getAppName(), context.getAppName());
	}

	@Override
	public String getFileName() {
		return "templates/serviceaccount.yaml";
	}

}
