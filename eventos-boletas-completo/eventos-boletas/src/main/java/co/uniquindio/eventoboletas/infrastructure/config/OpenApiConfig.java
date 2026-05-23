package co.uniquindio.eventoboletas.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger UI.
 * Principio SRP: sólo configura la documentación de la API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Eventos y Boletas API")
                .description("""
                    MVP - Sistema de venta de boletos para eventos.
                    Proyecto Ingeniería de Software II — Universidad del Quindío
                    Escenario 7A: Comprar Boleto (Cliente, Evento, Boleto)
                    
                    **Arquitectura limpia** | **SOLID** | **Patrones de diseño** | **Oracle DB**
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Sara Otero · Sara Acosta · Jennifer Cortés")
                    .email("ingenieria@uniquindio.edu.co")
                )
            );
    }
}
