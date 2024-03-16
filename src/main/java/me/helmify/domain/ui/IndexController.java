package me.helmify.domain.ui;

import lombok.RequiredArgsConstructor;
import me.helmify.domain.ChartCountTracker;
import me.helmify.domain.resolvers.DependencyResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for index page.
 * <p>
 * Users visiting the application will talk to this controller first.
 */
@Controller
@RequiredArgsConstructor
public class IndexController {

	private final ChartCountTracker chartCountTracker;

	private final BuildInfoProvider buildInfoProvider;

	private final List<DependencyResolver> resolvers;

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("message", "hello world");
		model.addAttribute("chartsGenerated", chartCountTracker.getChartCount());
		model.addAttribute("buildInfo", buildInfoProvider.getBuildInfo());

		Set<String> supportedDependenciesList = resolvers.stream()
			.map(DependencyResolver::dependencyName)
			.filter(s -> !s.equals("actuator"))
			.filter(s -> !s.equals("web"))
			.map(StringUtils::capitalize)
			.collect(Collectors.toSet());
		String supportedDependencies = String.join(", ", supportedDependenciesList);

		model.addAttribute("supportedDependencies", supportedDependencies);
		return "index";
	}

	@GetMapping("/about")
	public String about() {
		return "about";
	}

	@GetMapping("/privacy")
	public String privacy() {
		return "privacy";
	}

}