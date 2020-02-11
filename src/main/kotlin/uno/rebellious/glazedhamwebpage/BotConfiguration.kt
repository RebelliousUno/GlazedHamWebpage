package uno.rebellious.glazedhamwebpage

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:bot.properties")
class BotConfiguration() {

    @Value("\${client.id}")
    lateinit var client_id: String

    @Value("\${client.secret}")
    lateinit var client_secret: String

}
