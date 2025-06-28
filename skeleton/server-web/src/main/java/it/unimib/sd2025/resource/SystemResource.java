package it.unimib.sd2025.resource;

import it.unimib.sd2025.service.UserService;
import it.unimib.sd2025.service.ServiceException;
import it.unimib.sd2025.model.SystemStats;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Resource REST per le statistiche globali del sistema.
 * Espone endpoint per dashboard amministrativa.
 */
@Path("system")
public class SystemResource {
    
    private final UserService userService = new UserService();
    
    /**
     * GET /system/stats - Recupera statistiche globali del sistema
     */
    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemStats() {
        try {
            SystemStats stats = userService.getSystemStats();
            return Response.ok(stats).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il calcolo statistiche"))
                         .build();
        }
    }
    
    /**
     * GET /system/health - Health check del sistema
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        try {
            // Verifica connessione database
            it.unimib.sd2025.database.DatabaseClient.ping();
            
            return Response.ok(Map.of(
                "status", "healthy",
                "database", "connected",
                "timestamp", java.time.LocalDateTime.now().toString()
            )).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                         .entity(Map.of(
                             "status", "unhealthy",
                             "database", "disconnected",
                             "error", e.getMessage(),
                             "timestamp", java.time.LocalDateTime.now().toString()
                         )).build();
        }
    }
}