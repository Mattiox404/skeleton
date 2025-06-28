package it.unimib.sd2025.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import it.unimib.sd2025.database.DatabaseClient;
import it.unimib.sd2025.database.DatabaseException;
import it.unimib.sd2025.model.SystemStats;
import it.unimib.sd2025.model.User;

/**
 * Service layer per la gestione degli utenti.
 * Contiene TUTTA la business logic, l'UI si limita a chiamare questi metodi.
 */
public class UserService {
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Registra un nuovo utente nel sistema.
     * Valida i dati e verifica l'unicità del codice fiscale.
     */
    public User registerUser(String name, String surname, String email, String fiscalCode) throws ServiceException {
        // Validazione input
        if (name == null || name.trim().isEmpty()) {
            throw new ServiceException("Nome obbligatorio");
        }
        if (surname == null || surname.trim().isEmpty()) {
            throw new ServiceException("Cognome obbligatorio");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new ServiceException("Email obbligatoria");
        }
        if (fiscalCode == null || fiscalCode.trim().isEmpty()) {
            throw new ServiceException("Codice fiscale obbligatorio");
        }
        
        // Validazione formato email (semplificata)
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ServiceException("Formato email non valido");
        }
        
        // Validazione codice fiscale (lunghezza base)
        if (fiscalCode.length() != 16) {
            throw new ServiceException("Codice fiscale deve essere di 16 caratteri");
        }
        
        try {
            // Verifica unicità codice fiscale
            if (findUserByFiscalCode(fiscalCode) != null) {
                throw new ServiceException("Codice fiscale già registrato");
            }
            
            // Genera nuovo ID utente
            int newUserId = DatabaseClient.increment("counter:user");
            
            // Crea utente
            User user = new User(name.trim(), surname.trim(), email.trim(), fiscalCode.trim().toUpperCase());
            user.setId(newUserId);
            
            // Salva nel database
            saveUser(user);
            
            return user;
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante la registrazione: " + e.getMessage(), e);
        }
    }
    
    /**
     * Trova un utente per codice fiscale.
     */
    public User findUserByFiscalCode(String fiscalCode) throws ServiceException {
        if (fiscalCode == null || fiscalCode.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Cerca tra tutti gli utenti
            String[] userKeys = DatabaseClient.getKeys("user:*:fiscalCode");
            for (String key : userKeys) {
                String storedFiscalCode = DatabaseClient.get(key);
                if (fiscalCode.trim().equalsIgnoreCase(storedFiscalCode)) {
                    // Estrai ID utente dalla chiave: "user:123:fiscalCode" -> "123"
                    String userIdStr = key.split(":")[1];
                    int userId = Integer.parseInt(userIdStr);
                    return getUserById(userId);
                }
            }
            return null;
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante la ricerca utente: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recupera un utente per ID.
     */
    public User getUserById(int userId) throws ServiceException {
        if (userId <= 0) {
            return null;
        }
        
        try {
            // Verifica esistenza utente
            if (!DatabaseClient.exists(String.format("user:%d:name", userId))) {
                return null;
            }
            
            // Carica tutti i dati utente
            User user = new User();
            user.setId(userId);
            user.setName(DatabaseClient.get(String.format("user:%d:name", userId)));
            user.setSurname(DatabaseClient.get(String.format("user:%d:surname", userId)));
            user.setEmail(DatabaseClient.get(String.format("user:%d:email", userId)));
            user.setFiscalCode(DatabaseClient.get(String.format("user:%d:fiscalCode", userId)));
            
            user.setTotalBudget(DatabaseClient.getDouble(String.format("user:%d:totalBudget", userId)));
            user.setAvailableBudget(DatabaseClient.getDouble(String.format("user:%d:availableBudget", userId)));
            user.setUsedBudget(DatabaseClient.getDouble(String.format("user:%d:usedBudget", userId)));
            user.setConsumedBudget(DatabaseClient.getDouble(String.format("user:%d:consumedBudget", userId)));
            
            LocalDateTime regDate = DatabaseClient.getDateTime(String.format("user:%d:registrationDate", userId));
            if (regDate != null) {
                user.setRegistrationDate(regDate);
            }
            
            return user;
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante il recupero utente: " + e.getMessage(), e);
        }
    }
    
    /**
     * Salva o aggiorna un utente nel database.
     */
    private void saveUser(User user) throws DatabaseException {
        int userId = user.getId();
        
        DatabaseClient.set(String.format("user:%d:name", userId), user.getName());
        DatabaseClient.set(String.format("user:%d:surname", userId), user.getSurname());
        DatabaseClient.set(String.format("user:%d:email", userId), user.getEmail());
        DatabaseClient.set(String.format("user:%d:fiscalCode", userId), user.getFiscalCode());
        
        DatabaseClient.setDouble(String.format("user:%d:totalBudget", userId), user.getTotalBudget());
        DatabaseClient.setDouble(String.format("user:%d:availableBudget", userId), user.getAvailableBudget());
        DatabaseClient.setDouble(String.format("user:%d:usedBudget", userId), user.getUsedBudget());
        DatabaseClient.setDouble(String.format("user:%d:consumedBudget", userId), user.getConsumedBudget());
        
        DatabaseClient.setDateTime(String.format("user:%d:registrationDate", userId), user.getRegistrationDate());
    }
    
    /**
     * Aggiorna i budget di un utente in modo atomico.
     */
    public boolean updateUserBudgets(User user) throws ServiceException {
        try {
            return DatabaseClient.updateUserBudgets(
                user.getId(),
                user.getAvailableBudget(),
                user.getUsedBudget(),
                user.getConsumedBudget()
            );
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante l'aggiornamento budget: " + e.getMessage(), e);
        }
    }
    
    /**
     * Riserva budget per un nuovo buono.
     * Verifica che ci sia budget sufficiente e aggiorna i valori.
     */
    public boolean reserveBudget(int userId, double amount) throws ServiceException, DatabaseException {
        if (amount <= 0) {
            throw new ServiceException("L'importo deve essere positivo");
        }
        
        User user = getUserById(userId);
        if (user == null) {
            throw new ServiceException("Utente non trovato");
        }
        
        // Verifica budget disponibile
        if (user.getAvailableBudget() < amount) {
            return false; // Budget insufficiente
        }
        
        // Riserva budget
        user.reserveBudget(amount);
        
        // Salva nel database
        updateUserBudgets(user);
        
        return true;
    }
    public void releaseBudget(int userId, double amount) throws ServiceException, DatabaseException {
        if (amount <= 0) {
            return;
        }
        
        User user = getUserById(userId);
        if (user == null) {
            throw new ServiceException("Utente non trovato");
        }
        
        user.releaseBudget(amount);
        updateUserBudgets(user);
    }
    
    /**
     * Consuma budget quando un buono viene utilizzato.
     */
    public void consumeBudget(int userId, double amount) throws ServiceException, DatabaseException {
        if (amount <= 0) {
            return;
        }
        
        User user = getUserById(userId);
        if (user == null) {
            throw new ServiceException("Utente non trovato");
        }
        
        user.consumeBudget(amount);
        updateUserBudgets(user);
    }
    
    /**
     * Ottiene le statistiche globali del sistema.
     */
    public SystemStats getSystemStats() throws ServiceException {
        try {
            // Conta utenti totali
            String[] userKeys = DatabaseClient.getKeys("user:*:name");
            int totalUsers = userKeys.length;
            
            // Calcola budget totali
            double totalBudget = 0;
            double availableBudget = 0;
            double usedBudget = 0;
            double consumedBudget = 0;
            
            for (String key : userKeys) {
                String userIdStr = key.split(":")[1];
                int userId = Integer.parseInt(userIdStr);
                
                totalBudget += DatabaseClient.getDouble(String.format("user:%d:totalBudget", userId));
                availableBudget += DatabaseClient.getDouble(String.format("user:%d:availableBudget", userId));
                usedBudget += DatabaseClient.getDouble(String.format("user:%d:usedBudget", userId));
                consumedBudget += DatabaseClient.getDouble(String.format("user:%d:consumedBudget", userId));
            }
            
            // Conta buoni
            String[] voucherKeys = DatabaseClient.getKeys("voucher:*:userId");
            int totalVouchers = voucherKeys.length;
            
            int activeVouchers = 0;
            int consumedVouchers = 0;
            
            for (String key : voucherKeys) {
                String voucherIdStr = key.split(":")[1];
                String status = DatabaseClient.get(String.format("voucher:%s:status", voucherIdStr));
                
                if ("active".equals(status)) {
                    activeVouchers++;
                } else if ("consumed".equals(status)) {
                    consumedVouchers++;
                }
            }
            
            return new SystemStats(totalUsers, totalBudget, availableBudget, 
                                 usedBudget, consumedBudget, totalVouchers, 
                                 activeVouchers, consumedVouchers);
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante il calcolo statistiche: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ottiene la lista di tutti gli utenti (per admin).
     */
    public List<User> getAllUsers() throws ServiceException {
        try {
            List<User> users = new ArrayList<>();
            String[] userKeys = DatabaseClient.getKeys("user:*:name");
            
            for (String key : userKeys) {
                String userIdStr = key.split(":")[1];
                int userId = Integer.parseInt(userIdStr);
                User user = getUserById(userId);
                if (user != null) {
                    users.add(user);
                }
            }
            
            return users;
            
        } catch (DatabaseException e) {
            throw new ServiceException("Errore durante il recupero utenti: " + e.getMessage(), e);
        }
    }
}
