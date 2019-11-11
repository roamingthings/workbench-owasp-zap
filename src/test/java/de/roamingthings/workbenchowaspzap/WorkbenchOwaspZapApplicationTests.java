package de.roamingthings.workbenchowaspzap;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;

import static io.restassured.RestAssured.when;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@SpringBootTest(webEnvironment = DEFINED_PORT)
@Testcontainers
class WorkbenchOwaspZapApplicationTests {

	@Container
	GenericContainer owaspZapContainer =
			new GenericContainer("owasp/zap2docker-stable")
					.withExposedPorts(9080)
					.withCommand(
							"zap.sh",
							"-daemon",
							"-host", "0.0.0.0", "-port", "9080",
							"-config", "api.addrs.addr.name=.*",
							"-config", "api.addrs.addr.regex=true",
							"-config", "api.disablekey=true"
					);

	private String baseURI;
	private ClientApi zapApi;

	@BeforeAll
	static void setupTestcontainers() {
//		Note that the command should be invoked before containers are started.
		org.testcontainers.Testcontainers.exposeHostPorts(8080);
	}

	@BeforeEach
	void setupRestAssured() {
		baseURI = "http://host.testcontainers.internal:8080";
		RestAssured.baseURI = baseURI;
		RestAssured.proxy(owaspZapContainer.getMappedPort(9080));
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	void should_smoke() {
		zapApi = new ClientApi("localhost", owaspZapContainer.getMappedPort(9080), true);

		String target = RestAssured.baseURI + "/smoke";
		try {
			when()
					.request("GET", "/smoke")
					.then()
					.statusCode(200);

			// Start spidering the target
			System.out.println("Spider : " + target);
			// It's not necessary to pass the ZAP API key again, already set when creating the
			// ClientApi.
			ApiResponse resp = zapApi.spider.scan(target, null, null, null, null);
			String scanid;
			int progress;

			// The scan now returns a scan id to support concurrent scanning
			scanid = ((ApiResponseElement) resp).getValue();

			// Poll the status until it completes
			while (true) {
				SECONDS.sleep(1);
				progress =
						Integer.parseInt(
								((ApiResponseElement) zapApi.spider.status(scanid)).getValue());
				System.out.println("Spider progress : " + progress + "%");
				if (progress >= 100) {
					break;
				}
			}
			System.out.println("Spider complete");

			// Give the passive scanner a chance to complete
			SECONDS.sleep(2);

			System.out.println("Active scan : " + target);
			resp = zapApi.ascan.scan(target, "True", "False", null, null, null);

			// The scan now returns a scan id to support concurrent scanning
			scanid = ((ApiResponseElement) resp).getValue();

			// Poll the status until it completes
			while (true) {
				SECONDS.sleep(5);
				progress =
						Integer.parseInt(
								((ApiResponseElement) zapApi.ascan.status(scanid)).getValue());
				System.out.println("Active Scan progress : " + progress + "%");
				if (progress >= 100) {
					break;
				}
			}
			System.out.println("Active Scan complete");

			System.out.println("Alerts:");
			System.out.println(new String(zapApi.core.xmlreport(), UTF_8));

		} catch (Exception e) {
			System.out.println("Exception : " + e.getMessage());
			e.printStackTrace();
			System.out.println("Waiting so you can tinker with the container");
			try {
				HOURS.sleep(1);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}
}
