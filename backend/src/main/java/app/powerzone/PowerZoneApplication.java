package app.powerzone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PowerZoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(PowerZoneApplication.class, args);
    }
}
