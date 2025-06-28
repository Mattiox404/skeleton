package it.unimib.sd2025;

import it.unimib.sd2025.resource.SystemResource;
import it.unimib.sd2025.resource.UserResource;
import it.unimib.sd2025.resource.VoucherResource;
import it.unimib.sd2025.resource.SessionResource;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

/**
 * Configurazione JAX-RS Application.
 * Registra manualmente tutte le Resource classes e i Provider.
 */
@ApplicationPath("/")
public class RestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            // Resource classes
            SystemResource.class,
            UserResource.class,
            VoucherResource.class,
            SessionResource.class,
            
            // Provider classes (filtri, exception handlers)
            CorsFilter.class,
            JsonException.class,
            JsonParsingException.class
        );
    }
}