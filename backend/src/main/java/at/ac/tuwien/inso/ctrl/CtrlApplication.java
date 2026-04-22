package at.ac.tuwien.inso.ctrl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CtrlApplication {

  public static void main(String[] args) {
    SpringApplication.run(CtrlApplication.class, args);
  }
}
