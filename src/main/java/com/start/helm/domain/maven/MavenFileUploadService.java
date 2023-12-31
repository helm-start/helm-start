package com.start.helm.domain.maven;

import com.start.helm.domain.FileUploadService;
import com.start.helm.domain.helm.HelmContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.start.helm.domain.maven.MavenModelParser.parsePom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MavenFileUploadService implements FileUploadService {

	private final MavenModelProcessor mavenModelProcessor;

	public HelmContext processBuildFile(String pomXml) {
		org.apache.maven.api.model.Model m = parsePom(pomXml).orElseThrow();
		HelmContext helmContext = mavenModelProcessor.process(m);
		helmContext.setAppVersion(m.getVersion());
		helmContext.setAppName(m.getArtifactId());
		helmContext.setDependencyDescriptor(pomXml);
		return helmContext;

	}

	@Override
	public HelmContext processBuildFile(String buildFile, String appName, String appVersion) {
		HelmContext helmContext = processBuildFile(buildFile);
		helmContext.setAppVersion(appVersion);
		helmContext.setAppName(appName);
		return helmContext;
	}

}
