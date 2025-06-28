package it.unimib.sd2025.resource;

import it.unimib.sd2025.service.UserService;
import it.unimib.sd2025.service.ServiceException;
import it.unimib.sd2025.model.User;
import it.unimib.sd2025.model.SystemStats;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.List;

/**
 * Resource REST per la gestione degli utenti.
 * Espone gli endpoint per registrazione, login e gestione profilo.
 */
@Path("users")
public class UserResource {
    
    private final UserService userService = new UserService();
    
    /**
     * POST /users - Registra un nuovo utente
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(String rawUserData) {
        try {
            var jsonb = JsonbBuilder.create();
            
            // Parse JSON input
            @SuppressWarnings("unchecked")
            Map<String, Object> userData = jsonb.fromJson(rawUserData, Map.class);
            
            String name = (String) userData.get("name");
            String surname = (String) userData.get("surname");
            String email = (String) userData.get("email");
            String fiscalCode = (String) userData.get("fiscalCode");
            
            // Registra utente
            User newUser = userService.registerUser(name, surname, email, fiscalCode);
            
            // Crea URI per la risorsa creata
            URI userUri = new URI("/users/" + newUser.getId());
            
            return Response.created(userUri)
                         .entity(newUser)
                         .build();
                         
        } catch (JsonbException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", "JSON non valido"))
                         .build();
                         
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
                         
        } catch (URISyntaxException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore interno del server"))
                         .build();
        }
    }
    
    /**
     * GET /users/{id} - Recupera dati di un utente specifico
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserById(@PathParam("id") int userId) {
        try {
            User user = userService.getUserById(userId);
            
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Utente non trovato"))
                             .build();
            }
            
            return Response.ok(user).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il recupero utente"))
                         .build();
        }
    }
    
    /**
     * GET /users/by-fiscal-code/{fiscalCode} - Trova utente per codice fiscale
     */
    @GET
    @Path("/by-fiscal-code/{fiscalCode}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserByFiscalCode(@PathParam("fiscalCode") String fiscalCode) {
        try {
            User user = userService.findUserByFiscalCode(fiscalCode);
            
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Utente non trovato"))
                             .build();
            }
            
            return Response.ok(user).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante la ricerca utente"))
                         .build();
        }
    }
    
    /**
     * GET /users/{id}/budget - Recupera stato budget di un utente
     */
    @GET
    @Path("/{id}/budget")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserBudget(@PathParam("id") int userId) {
        try {
            User user = userService.getUserById(userId);
            
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Utente non trovato"))
                             .build();
            }
            
            Map<String, Object> budgetSummary = Map.of(
                "total", user.getTotalBudget(),
                "available", user.getAvailableBudget(),
                "used", user.getUsedBudget(),
                "consumed", user.getConsumedBudget()
            );
            
            return Response.ok(budgetSummary).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il recupero budget"))
                         .build();
        }
    }
    
    /**
     * GET /users - Recupera lista di tutti gli utenti (per admin)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return Response.ok(users).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il recupero utenti"))
                         .build();
        }
    }
}