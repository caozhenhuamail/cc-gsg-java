package io.camunda.cloudstarter;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.response.Topology;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.client.api.response.WorkflowInstanceResult;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.spring.client.EnableZeebeClient;
import io.zeebe.spring.client.ZeebeClientLifecycle;
import io.zeebe.spring.client.annotation.ZeebeDeployment;
import io.zeebe.spring.client.annotation.ZeebeWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Map;

@SpringBootApplication
@EnableZeebeClient
@RestController
@ZeebeDeployment(classPathResources = {"test-process.bpmn"})
public class CloudStarterApplication {

	@Autowired
	private ZeebeClientLifecycle client;

	Logger logger = LoggerFactory.getLogger(CloudStarterApplication.class);
	Instant start;
	public static void main(String[] args) {
		SpringApplication.run(CloudStarterApplication.class, args);
	}

	@GetMapping("/status")
	public String getStatus() {
		Topology res = client.newTopologyRequest().send().join();
		return res.toString();
	}

	@GetMapping("/start")
	public String startWorkflowInstance() {
		Date date = new Date();
		start = date.toInstant();
		logger.info("Start workflow: " + start.toString());
		WorkflowInstanceResult workflowInstanceResult = client
				.newCreateInstanceCommand()
				.bpmnProcessId("test-process")
				.latestVersion()
				.variables("{\"name\": \"Josh Wulf\"}")
				.withResult()
				.send()
				.join();
		date = new Date();
		Instant finish = date.toInstant();
		logger.info("Finish workflow: " + finish.toString());
		Duration elapsedTime = Duration.between(start, finish);
		logger.info("End-to-end latency: " + elapsedTime.toString());
		return (String) workflowInstanceResult
				.getVariablesAsMap()
				.getOrDefault("say", "Error: No greeting returned");
	}

	@ZeebeWorker(type = "get-time")
	public void handleGetTime(final JobClient client, final ActivatedJob job) {
		Date date = new Date();
		Instant workerStart = date.toInstant();
		Duration elapsedTime = Duration.between(start, workerStart);
		logger.info("get-time worker latency: " + elapsedTime.toString());

		final String uri = "https://json-api.joshwulf.com/time";

		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject(uri, String.class);

		client.newCompleteCommand(job.getKey())
				.variables("{\"time\":" + result + "}")
				.send().join();
		Instant workerFinish = new Date().toInstant();
		logger.info("get-time worker finish: " + Duration.between(workerStart, workerFinish).toString());
	}

	@ZeebeWorker(type = "make-greeting")
	public void handleMakeGreeting(final JobClient client, final ActivatedJob job) {
		Date date = new Date();
		Instant workerStart = date.toInstant();
		Duration elapsedTime = Duration.between(start, workerStart);
		logger.info("make-greeting worker start latency: " + elapsedTime.toString());
		Map<String, String> headers = job.getCustomHeaders();
		String greeting = headers.getOrDefault("greeting", "Good day");
		Map<String, Object> variablesAsMap = job.getVariablesAsMap();
		String name = (String) variablesAsMap.getOrDefault("name", "there");
		String say = greeting + " " + name;
		client.newCompleteCommand(job.getKey())
				.variables("{\"say\": \"" + say + "\"}")
				.send().join();
		Instant workerFinish = new Date().toInstant();
		logger.info("make-greeting worker task latency: " + Duration.between(workerStart, workerFinish).toString());
	}
}
