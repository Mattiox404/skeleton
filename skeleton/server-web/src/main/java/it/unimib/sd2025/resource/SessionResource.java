package it.unimib.sd2025.resource;

import it.unimib.sd2025.service.UserService;
import it.unimib.sd2025.service.ServiceException;
import it.unimib.sd2025.model.User;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.NewCookie;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Resource REST per la gestione delle sessioni utente.
 * Implementa un sistema di sessioni basato su cookie per tracciare gli utenti.
 */
@Path("session")
public class SessionResource {
    
    private final UserService userService = new UserService();
    
    // In-memory session storage (in produzione si userebbe Redis o database)
    private static final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    private static final int SESSION_TIMEOUT_HOURS = 8; // 8 ore di timeout
    
    /**
     * POST /session/login - Login utente tramite codice fiscale
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String rawLoginData) {
        try {
            var jsonb = JsonbBuilder.create();
            
            // Parse JSON input
            @SuppressWarnings("unchecked")
            Map<String, Object> loginData = jsonb.fromJson(rawLoginData, Map.class);
            
            String fiscalCode = (String) loginData.get("fiscalCode");
            
            if (fiscalCode == null || fiscalCode.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                             .entity(Map.of("error", "Codice fiscale obbligatorio"))
                             .build();
            }
            
            // Cerca utente per codice fiscale
            User user = userService.findUserByFiscalCode(fiscalCode);
            
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                             .entity(Map.of("error", "Utente non trovato"))
                             .build();
            }
            
            // Crea nuova sessione
            String sessionId = UUID.randomUUID().toString();
            SessionData session = new SessionData(user.getId(), user.getFiscalCode(), LocalDateTime.now());
            activeSessions.put(sessionId, session);
            
            // Non facciamo cleanup qui per evitare race conditions con più richieste parallele
            
            // Crea cookie di sessione con SameSite LAX
            NewCookie sessionCookie = new NewCookie.Builder("CARTA_CULTURA_SESSION")
                .value(sessionId)
                .path("/")
                .maxAge(SESSION_TIMEOUT_HOURS * 3600)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.LAX)   // LAX per farlo accettare anche su cross-origin dev
                .build();

            Map<String, Object> response = Map.of(
                "user", user,
                "sessionId", sessionId,
                "message", "Login effettuato con successo"
            );
            
            return Response.ok(response)
                         .cookie(sessionCookie)
                         .build();
                         
        } catch (JsonbException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", "JSON non valido"))
                         .build();
                         
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il login"))
                         .build();
        }
    }
    
    /**
     * GET /session/current - Recupera informazioni sessione corrente
     */
    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentSession(@CookieParam("CARTA_CULTURA_SESSION") String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                         .entity(Map.of("error", "Sessione non trovata"))
                         .build();
        }
        
        synchronized (activeSessions) {
            SessionData session = activeSessions.get(sessionId);
            
            if (session == null || isSessionExpired(session)) {
                activeSessions.remove(sessionId);
                
                return Response.status(Response.Status.UNAUTHORIZED)
                             .entity(Map.of("error", "Sessione scaduta"))
                             .build();
            }
            
            try {
                // Aggiorna last access
                session.setLastAccess(LocalDateTime.now());
                
                // Recupera dati utente aggiornati
                User user = userService.getUserById(session.getUserId());
                
                if (user == null) {
                    activeSessions.remove(sessionId);
                    return Response.status(Response.Status.UNAUTHORIZED)
                                 .entity(Map.of("error", "Utente non più valido"))
                                 .build();
                }
                
                Map<String, Object> response = Map.of(
                    "user", user,
                    "sessionId", sessionId,
                    "lastAccess", session.getLastAccess().toString()
                );
                
                return Response.ok(response).build();
                
            } catch (ServiceException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                             .entity(Map.of("error", "Errore durante il recupero sessione"))
                             .build();
            }
        }
    }
    
    /**
     * POST /session/logout - Logout utente
     */
    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@CookieParam("CARTA_CULTURA_SESSION") String sessionId) {
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
        
        // Cancella cookie
        NewCookie clearCookie = new NewCookie.Builder("CARTA_CULTURA_SESSION")
            .value("")
            .path("/")
            .maxAge(0) // Scadenza immediata
            .httpOnly(true)
            .build();
        
        return Response.ok(Map.of("message", "Logout effettuato con successo"))
                     .cookie(clearCookie)
                     .build();
    }
    
    /**
     * GET /session/validate - Valida una sessione (per uso interno)
     */
    @GET
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateSession(@CookieParam("CARTA_CULTURA_SESSION") String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                         .entity(Map.of("valid", false, "error", "No session"))
                         .build();
        }
        
        SessionData session = activeSessions.get(sessionId);
        
        if (session == null || isSessionExpired(session)) {
            activeSessions.remove(sessionId);
            return Response.status(Response.Status.UNAUTHORIZED)
                         .entity(Map.of("valid", false, "error", "Session expired"))
                         .build();
        }
        
        // Aggiorna last access
        session.setLastAccess(LocalDateTime.now());
        
        return Response.ok(Map.of(
            "valid", true,
            "userId", session.getUserId(),
            "fiscalCode", session.getFiscalCode()
        )).build();
    }
    
    /**
     * Utility per verificare se una sessione è scaduta.
     */
    private boolean isSessionExpired(SessionData session) {
        LocalDateTime expiry = session.getLastAccess().plus(SESSION_TIMEOUT_HOURS, ChronoUnit.HOURS);
        return LocalDateTime.now().isAfter(expiry);
    }
    
    /**
     * Classe per rappresentare i dati di una sessione.
     */
    private static class SessionData {
        private final int userId;
        private final String fiscalCode;
        private final LocalDateTime createdAt;
        private LocalDateTime lastAccess;
        
        public SessionData(int userId, String fiscalCode, LocalDateTime createdAt) {
            this.userId = userId;
            this.fiscalCode = fiscalCode;
            this.createdAt = createdAt;
            this.lastAccess = createdAt;
        }
        
        public int getUserId() { return userId; }
        public String getFiscalCode() { return fiscalCode; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccess() { return lastAccess; }
        public void setLastAccess(LocalDateTime lastAccess) { this.lastAccess = lastAccess; }
    }
}

