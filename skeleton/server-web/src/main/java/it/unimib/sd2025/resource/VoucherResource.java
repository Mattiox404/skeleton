package it.unimib.sd2025.resource;

import it.unimib.sd2025.service.VoucherService;
import it.unimib.sd2025.service.UserService;
import it.unimib.sd2025.service.ServiceException;
import it.unimib.sd2025.database.DatabaseException;
import it.unimib.sd2025.model.Voucher;

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
 * Resource REST per la gestione dei buoni (voucher).
 * Espone gli endpoint per creazione, gestione e consumo buoni.
 */
@Path("vouchers")
public class VoucherResource {
    
    private final UserService userService = new UserService();
    private final VoucherService voucherService = new VoucherService(userService);
    
    /**
     * POST /vouchers - Crea un nuovo buono
     * @throws DatabaseException 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVoucher(String rawVoucherData) throws DatabaseException {
        try {
            var jsonb = JsonbBuilder.create();
            
            // Parse JSON input
            @SuppressWarnings("unchecked")
            Map<String, Object> voucherData = jsonb.fromJson(rawVoucherData, Map.class);
            
            // Extract parameters
            Object userIdObj = voucherData.get("userId");
            Object amountObj = voucherData.get("amount");
            String category = (String) voucherData.get("category");
            
            if (userIdObj == null || amountObj == null || category == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                             .entity(Map.of("error", "userId, amount e category sono obbligatori"))
                             .build();
            }
            
            int userId;
            double amount;
            
            try {
                userId = ((Number) userIdObj).intValue();
                amount = ((Number) amountObj).doubleValue();
            } catch (ClassCastException | NumberFormatException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                             .entity(Map.of("error", "userId deve essere un intero, amount deve essere un numero"))
                             .build();
            }
            
            // Crea buono
            Voucher newVoucher = voucherService.createVoucher(userId, amount, category);
            
            // Crea URI per la risorsa creata
            URI voucherUri = new URI("/vouchers/" + newVoucher.getId());
            
            return Response.created(voucherUri)
                         .entity(newVoucher)
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
     * GET /vouchers/{id} - Recupera un buono specifico
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVoucherById(@PathParam("id") int voucherId) {
        try {
            Voucher voucher = voucherService.getVoucherById(voucherId);
            
            if (voucher == null) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Buono non trovato"))
                             .build();
            }
            
            return Response.ok(voucher).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il recupero buono"))
                         .build();
        }
    }
    
    /**
     * GET /vouchers/user/{userId} - Recupera tutti i buoni di un utente
     */
    @GET
    @Path("/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserVouchers(@PathParam("userId") int userId) {
        try {
            List<Voucher> vouchers = voucherService.getUserVouchers(userId);
            return Response.ok(vouchers).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il recupero buoni utente"))
                         .build();
        }
    }
    
    /**
     * PUT /vouchers/{id}/consume - Consuma un buono
          * @throws DatabaseException 
          */
         @PUT
         @Path("/{id}/consume")
         @Produces(MediaType.APPLICATION_JSON)
         public Response consumeVoucher(@PathParam("id") int voucherId) throws DatabaseException {
        try {
            boolean success = voucherService.consumeVoucher(voucherId);
            
            if (success) {
                return Response.ok(Map.of("message", "Buono consumato con successo")).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                             .entity(Map.of("error", "Impossibile consumare il buono"))
                             .build();
            }
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }
    
    /**
     * DELETE /vouchers/{id} - Cancella un buono
          * @throws DatabaseException 
          */
         @DELETE
         @Path("/{id}")
         @Produces(MediaType.APPLICATION_JSON)
         public Response deleteVoucher(@PathParam("id") int voucherId) throws DatabaseException {
        try {
            boolean success = voucherService.deleteVoucher(voucherId);
            
            if (success) {
                return Response.ok(Map.of("message", "Buono eliminato con successo")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Buono non trovato"))
                             .build();
            }
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }
    
    /**
     * PUT /vouchers/{id}/category - Modifica categoria di un buono
     */
    @PUT
    @Path("/{id}/category")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVoucherCategory(@PathParam("id") int voucherId, String rawCategoryData) {
        try {
            var jsonb = JsonbBuilder.create();
            
            // Parse JSON input
            @SuppressWarnings("unchecked")
            Map<String, Object> categoryData = jsonb.fromJson(rawCategoryData, Map.class);
            
            String newCategory = (String) categoryData.get("category");
            
            if (newCategory == null || newCategory.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                             .entity(Map.of("error", "Categoria obbligatoria"))
                             .build();
            }
            
            boolean success = voucherService.updateVoucherCategory(voucherId, newCategory);
            
            if (success) {
                return Response.ok(Map.of("message", "Categoria aggiornata con successo")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity(Map.of("error", "Buono non trovato"))
                             .build();
            }
            
        } catch (JsonbException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", "JSON non valido"))
                         .build();
                         
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(Map.of("error", e.getMessage()))
                         .build();
        }
    }
    
    /**
     * GET /vouchers - Recupera tutti i buoni del sistema (per admin)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllVouchers() {
        try {
            List<Voucher> vouchers = voucherService.getAllVouchers();
            return Response.ok(vouchers).build();
            
        } catch (ServiceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .entity(Map.of("error", "Errore durante il recupero buoni"))
                         .build();
        }
    }
}