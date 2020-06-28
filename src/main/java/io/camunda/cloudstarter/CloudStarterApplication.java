package io.camunda.cloudstarter;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.spring.client.EnableZeebeClient;
import io.zeebe.spring.client.ZeebeClientLifecycle;
import io.zeebe.spring.client.annotation.ZeebeDeployment;
import io.zeebe.spring.client.annotation.ZeebeWorker;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashSet;

@SpringBootApplication
@EnableZeebeClient
@RestController
@ZeebeDeployment(classPathResources = {"test-process.bpmn"})
public class CloudStarterApplication {

	@Autowired
	private ZeebeClientLifecycle client;

	Logger logger = LoggerFactory.getLogger(CloudStarterApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CloudStarterApplication.class, args);
	}

	@GetMapping("/start")
	public String startWorkflowInstance() {
		WorkflowInstanceEvent workflowInstanceEvent = client.newCreateInstanceCommand().bpmnProcessId("test-process")
				.latestVersion()
//				.variables(Collections.singletonMap("proposal", proposal))
				.send().join();
		return workflowInstanceEvent.toString();
	}

	@ZeebeWorker(type = "get-time")
	public void handleGetTime(final JobClient client, final ActivatedJob job) {
		// do whatever you need to do
		logger.info(job.toString());

		final String uri = "https://json-api.joshwulf.com/time";

		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject(uri, String.class);

		System.out.println(result);
		client.newCompleteCommand(job.getKey())
				.variables("{\"time\":" + result + "}")
				.send().join();
	}

	@ZeebeWorker(type = "make-greeting")
	public void handleMakeGreeting(final JobClient client, final ActivatedJob job) {
		// do whatever you need to do
		logger.info(job.toString());
		client.newCompleteCommand(job.getKey())
				.variables("{\"fooResult\": 1}")
				.send().join();
	}
}
