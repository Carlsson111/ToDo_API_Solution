package se.lexicon.todo_app.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import se.lexicon.notify.config.NotifyUtilConfig;

@Configuration
@ComponentScan("se.lexicon.todo_app.*")
@Import(NotifyUtilConfig.class)
public class AppConfig {
}