package app.deptinfo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DeptInfoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeptInfoApplication.class, args);
    }
}
