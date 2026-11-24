package tokentally.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import tokentally.app.Engine;

// starts the web api. all the real work is in the core Engine
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    // one engine for the whole app, loaded from the saved data
    @Bean
    public Engine engine(@Value("${tokentally.data}") String folder) throws Exception {
        Engine e = new Engine(folder);
        e.load();
        return e;
    }
}
