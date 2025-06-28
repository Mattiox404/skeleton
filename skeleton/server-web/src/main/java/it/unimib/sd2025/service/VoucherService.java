package it.unimib.sd2025.service;

import it.unimib.sd2025.database.DatabaseClient;
import it.unimib.sd2025.database.DatabaseException;
import it.unimib.sd2025.model.Voucher;
import it.unimib.sd2025.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service layer per la gestione dei buoni.
 * Gestisce la concorrenza per prevenire over-spending dello stesso utente.
 */
public class VoucherService {
    
    private final UserService userService;
    
    // Lock per gestire accessi concorrenti per utente
    // In produzione si userebbe un distributed lock (Redis, Hazelcast, etc.)
    private final ReentrantLock userLock = new ReentrantLock();
    
    public VoucherService(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Crea un nuovo buono.
     * Verifica disponibilità budget e gestisce concorrenza.
          * @throws DatabaseException 
          */
         public Voucher createVoucher(int userId, double amount, String categoryStr) throws ServiceException, DatabaseException {
        // Validazione input
        if (amount <= 0) {
            throw new ServiceException("L'importo deve essere positivo");
        }
        
        if (amount > 500.0) {
            throw new ServiceException("L'importo non può superare 500€");
        }
        
        Voucher.Category category;
        try {
            category = Voucher.Category.fromString(categoryStr);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Categoria non valida: " + categoryStr);
        }
        
        // Lock per prevenire race condition su budget utente
        userLock.lock();
        try {
            // Verifica esistenza utente
            User user = userService.getUserById(userId);
            if (user == null) {
                throw new ServiceException("Utente non trovato");
            }
            
            // Verifica budget disponibile
            if (!userService.reserveBudget(userId, amount)) {
                throw new ServiceException("Budget insufficiente. Disponibile: €" + 
                                         String.format("%.2f", user.getAvailableBudget()));
            }
            
            try {
                // Genera nuovo ID voucher
                int newVoucherId = DatabaseClient.increment("counter:voucher");
                
                // Crea voucher
                Voucher voucher = new Voucher(userId, amount, category);
                voucher.setId(newVoucherId);

                System.out.println("DEBUG createVoucher appena creato: id=" + voucher.getId() + 
                   " status=" + voucher.getStatus() + 
                   " category=" + voucher.getCategory());

                
                // Salva nel database
                saveVoucher(voucher);
                
                return voucher;
                
            } catch (DatabaseException e) {
                // Rollback: rilascia il budget se la creazione fallisce
                userService.releaseBudget(userId, amount);
                throw new ServiceException("Errore durante la creazione del buono: " + e.getMessage(), e);
            }
            
        } finally {
            userLock.unlock();
        }
        

    }
    
    /**
     * Recupera un buono per ID.
     */
    public Voucher getVoucherById(int voucherId) throws ServiceException {
        if (voucherId <= 0) {
            return null;
        }
        
        try {
            // Verifica esistenza voucher
            if (!DatabaseClient.exists(String.format("voucher:%d:userId", voucherId))) {
                return null;
            }
            
            // Carica tutti i dati voucher
            Voucher voucher = new Voucher();
            voucher.setId(voucherId);
            voucher.setUserId(DatabaseClient.getInt(String.format("voucher:%d:userId", voucherId)));
            voucher.setAmount(DatabaseClient.getDouble(String.format("voucher:%d:amount", voucherId)));
            
            String categoryStr = DatabaseClient.get(String.format("voucher:%d:category", voucherId));
            voucher.setCategory(Voucher.Category.fromString(categoryStr));
            
            String statusStr = DatabaseClient.get(String.format("voucher:%d:status", voucherId));
            voucher.setStatus(Voucher.Status.fromString(statusStr));
            
            LocalDateTime createdAt = DatabaseClient.getDateTime(String.format("voucher:%d:createdAt", voucherId));
            if (createdAt != null) {
                voucher.setCreatedAt(createdAt);
            }
            
            LocalDateTime consumedAt = DatabaseClient.getDateTime(String.format("voucher:%d:consumedAt", voucherId));
            if (consumedAt != null) {
                voucher.setConsumedAt(consumedAt);
            }
            
            return voucher;
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante il recupero buono: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recupera tutti i buoni di un utente ordinati cronologicamente.
     */
    public List<Voucher> getUserVouchers(int userId) throws ServiceException {
        try {
            List<Voucher> vouchers = new ArrayList<>();
            String[] voucherKeys = DatabaseClient.getKeys("voucher:*:userId");
            
            for (String key : voucherKeys) {
                String voucherIdStr = key.split(":")[1];
                int voucherId = Integer.parseInt(voucherIdStr);
                
                // Verifica se il voucher appartiene all'utente
                int voucherUserId = DatabaseClient.getInt(String.format("voucher:%d:userId", voucherId));
                if (voucherUserId == userId) {
                    Voucher voucher = getVoucherById(voucherId);
                    if (voucher != null) {
                        vouchers.add(voucher);
                    }
                }
            }
            
            // Ordina per data di creazione (più recente prima)
            vouchers.sort((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt()));
            
            return vouchers;
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante il recupero buoni utente: " + e.getMessage(), e);
        }
    }
    
    /**
     * Consuma un buono.
          * @throws DatabaseException 
          */
         public boolean consumeVoucher(int voucherId) throws ServiceException, DatabaseException {
        userLock.lock();
        try {
            Voucher voucher = getVoucherById(voucherId);
            if (voucher == null) {
                throw new ServiceException("Buono non trovato");
            }
            
            if (voucher.getStatus() != Voucher.Status.ACTIVE) {
                throw new ServiceException("Il buono è già stato consumato");
            }
            
            // Aggiorna stato voucher
            voucher.setStatus(Voucher.Status.CONSUMED);
            voucher.setConsumedAt(LocalDateTime.now());
            
            // Aggiorna budget utente
            userService.consumeBudget(voucher.getUserId(), voucher.getAmount());
            
            // Salva modifiche
            saveVoucher(voucher);
            
            return true;
            
        } finally {
            userLock.unlock();
        }
    }
    
    /**
     * Cancella un buono (solo se non ancora consumato).
          * @throws DatabaseException 
          */
         public boolean deleteVoucher(int voucherId) throws ServiceException, DatabaseException {
        userLock.lock();
        try {
            Voucher voucher = getVoucherById(voucherId);
            if (voucher == null) {
                return false; // Voucher non esistente
            }
            
            if (voucher.getStatus() == Voucher.Status.CONSUMED) {
                throw new ServiceException("Non è possibile cancellare un buono già consumato");
            }
            
            // Rilascia budget
            userService.releaseBudget(voucher.getUserId(), voucher.getAmount());
            
            // Rimuovi voucher dal database
            deleteVoucherFromDatabase(voucherId);
            
            return true;
            
        } finally {
            userLock.unlock();
        }
    }
    
    /**
     * Modifica la categoria di un buono (solo se non consumato).
     */
    public boolean updateVoucherCategory(int voucherId, String newCategoryStr) throws ServiceException {
        Voucher.Category newCategory;
        try {
            newCategory = Voucher.Category.fromString(newCategoryStr);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Categoria non valida: " + newCategoryStr);
        }
        
        Voucher voucher = getVoucherById(voucherId);
        if (voucher == null) {
            throw new ServiceException("Buono non trovato");
        }
        
        if (voucher.getStatus() == Voucher.Status.CONSUMED) {
            throw new ServiceException("Non è possibile modificare un buono già consumato");
        }
        
        voucher.setCategory(newCategory);
        
        try {
            saveVoucher(voucher);
            return true;
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante l'aggiornamento categoria: " + e.getMessage(), e);
        }
    }
    
    /**
     * Salva un voucher nel database.
     */
    private void saveVoucher(Voucher voucher) throws DatabaseException {
        int voucherId = voucher.getId();
        
        System.out.println("DEBUG saveVoucher id=" + voucherId + " status=" + voucher.getStatus());
        DatabaseClient.setInt(String.format("voucher:%d:userId", voucherId), voucher.getUserId());
        DatabaseClient.setDouble(String.format("voucher:%d:amount", voucherId), voucher.getAmount());
        DatabaseClient.set(String.format("voucher:%d:category", voucherId), voucher.getCategory().getDisplayName());
        DatabaseClient.set(String.format("voucher:%d:status", voucherId), voucher.getStatus().getValue());
        DatabaseClient.setDateTime(String.format("voucher:%d:createdAt", voucherId), voucher.getCreatedAt());
        
        if (voucher.getConsumedAt() != null) {
            DatabaseClient.setDateTime(String.format("voucher:%d:consumedAt", voucherId), voucher.getConsumedAt());
        }
    }
    
    
    /**
     * Rimuove completamente un voucher dal database.
     */
    private void deleteVoucherFromDatabase(int voucherId) throws DatabaseException {
        DatabaseClient.delete(String.format("voucher:%d:userId", voucherId));
        DatabaseClient.delete(String.format("voucher:%d:amount", voucherId));
        DatabaseClient.delete(String.format("voucher:%d:category", voucherId));
        DatabaseClient.delete(String.format("voucher:%d:status", voucherId));
        DatabaseClient.delete(String.format("voucher:%d:createdAt", voucherId));
        DatabaseClient.delete(String.format("voucher:%d:consumedAt", voucherId));
    }
    
    /**
     * Verifica se un utente ha buoni in sospeso che potrebbero causare over-spending.
     */
    public boolean hasReservedBudget(int userId) throws ServiceException {
        User user = userService.getUserById(userId);
        return user != null && user.getUsedBudget() > 0;
    }
    
    /**
     * Recupera tutti i buoni del sistema (per admin).
     */
    public List<Voucher> getAllVouchers() throws ServiceException {
        try {
            List<Voucher> vouchers = new ArrayList<>();
            String[] voucherKeys = DatabaseClient.getKeys("voucher:*:userId");
            
            for (String key : voucherKeys) {
                String voucherIdStr = key.split(":")[1];
                int voucherId = Integer.parseInt(voucherIdStr);
                
                Voucher voucher = getVoucherById(voucherId);
                if (voucher != null) {
                    vouchers.add(voucher);
                }
            }
            
            // Ordina per data di creazione (più recente prima)
            vouchers.sort((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt()));
            
            return vouchers;
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante il recupero di tutti i buoni: " + e.getMessage(), e);
        }
    }
}