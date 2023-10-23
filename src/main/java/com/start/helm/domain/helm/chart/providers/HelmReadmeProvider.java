package com.start.helm.domain.helm.chart.providers;

import com.start.helm.domain.helm.HelmContext;
import org.springframework.stereotype.Component;

@Component
public class HelmReadmeProvider implements HelmFileProvider {

	@Override
	public String getFileContent(HelmContext context) {
		return """
				## Helm chart for %s

				This chart was generated by helm-start.com

				### Next steps

				These steps assume you have already built your image via:
				```shell
				mvn spring-boot:build-image
				```
				or
				```shell
				gradle bootBuildImage
				```

				When using kind (Kubernetes in Docker), make sure to version tag and push your image to the cluster via:
				```shell
				docker tag docker.io/library/%s:latest %s:some-specific-version
				kind load docker-image %s:some-specific-version
				```
				The image tag should be updated correspondingly in values.yaml:
				```yaml
				image:
				  pullPolicy: Always
				  repository: &s
				  secrets: []
				  tag: some-specific-version
				```

				To deploy this in a local setup with Kubernetes in Docker (kind) run:

				```shell
				helm install %s . --namespace %s --create-namespace
				```

				""".replace("%s", context.getAppName());
	}

	@Override
	public String getFileName() {
		return "README.MD";
	}

}
