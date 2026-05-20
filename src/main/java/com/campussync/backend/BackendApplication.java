package com.campussync.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
		UserDetailsServiceAutoConfiguration.class
})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableAsync
public class BackendApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(BackendApplication.class, args);
	}

	private static void loadDotenv() {
		java.nio.file.Path dotenvPath = java.nio.file.Paths.get(".env").toAbsolutePath().normalize();
		if (!java.nio.file.Files.exists(dotenvPath)) {
			dotenvPath = java.nio.file.Paths.get("..", ".env").toAbsolutePath().normalize();
		}

		if (java.nio.file.Files.exists(dotenvPath)) {
			System.out.println("[Main] Loading .env from " + dotenvPath);
			try {
				java.util.List<String> lines = java.nio.file.Files.readAllLines(dotenvPath);
				for (String line : lines) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) continue;
					int eqPos = line.indexOf('=');
					if (eqPos > 0) {
						String key = line.substring(0, eqPos).trim();
						String value = line.substring(eqPos + 1).trim();
						if (System.getProperty(key) == null && System.getenv(key) == null) {
							System.setProperty(key, value);
						}
					}
				}
			} catch (java.io.IOException e) {
				System.err.println("[Main] Failed to load .env: " + e.getMessage());
			}
		} else {
			System.out.println("[Main] .env file not found");
		}
	}

}
