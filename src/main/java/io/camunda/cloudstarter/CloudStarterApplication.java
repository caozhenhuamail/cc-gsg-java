package io.camunda.cloudstarter;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.spring.client.EnableZeebeClient;
import io.zeebe.spring.client.annotation.ZeebeDeployment;
import io.zeebe.spring.client.annotation.ZeebeWorker;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableZeebeClient
@ZeebeDeployment(classPathResources = {"demoProcess.bpmn"})
public class CloudStarterApplication {

	Logger logger = LoggerFactory.getLogger(CloudStarterApplication.class);

	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger(CloudStarterApplication.class);

		log.info("Yo!");
		SpringApplication.run(CloudStarterApplication.class, args);
	}

	@ZeebeWorker(type = "get-time")
	public void handleGetTime(final JobClient client, final ActivatedJob job) {
		// do whatever you need to do
		logger.info(job.toString());
		client.newCompleteCommand(job.getKey())
				.variables("{\"fooResult\": 1}")
				.send().join();
	}

	@ZeebeWorker(type = "make-greeting")
	public void handleMakeGreeting(final JobClient client, final ActivatedJob job) {
		// do whatever you need to do
		client.newCompleteCommand(job.getKey())
				.variables("{\"fooResult\": 1}")
				.send().join();
	}
}
