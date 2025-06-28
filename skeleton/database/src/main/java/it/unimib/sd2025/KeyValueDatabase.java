package it.unimib.sd2025;

import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Database chiave-valore generico in-memory con persistenza opzionale.
 * Utilizza esclusivamente Map<String, String> per garantire massima genericità.
 */
public class KeyValueDatabase {
    
    // Storage generico - SOLO stringhe per massima flessibilità
    private final Map<String, String> storage = new ConcurrentHashMap<>();
    
    // Lock per operazioni che richiedono atomicità
    private final Object storageLock = new Object();
    
    /**
     * Inizializza il database con dati preesistenti da file.
     */
    public void loadInitialData() {
        try {
            var dataPath = Paths.get("data", "initial_data.properties");
            if (Files.exists(dataPath)) {
                var props = new Properties();
                try (var input = Files.newBufferedReader(dataPath)) {
                    props.load(input);
                    for (String key : props.stringPropertyNames()) {
                        storage.put(key, props.getProperty(key));
                    }
                }
                System.out.println("Loaded " + storage.size() + " initial records");
            } else {
                // Dati di esempio per testing
                loadDefaultData();
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load initial data: " + e.getMessage());
            loadDefaultData();
        }
    }
    
    /**
     * Carica dati di default per testing.
     */
    private void loadDefaultData() {
        // Utenti di esempio
        storage.put("user:1:name", "Mario");
        storage.put("user:1:surname", "Rossi");
        storage.put("user:1:email", "mario.rossi@email.com");
        storage.put("user:1:fiscalCode", "RSSMRA90A01F205X");
        storage.put("user:1:totalBudget", "500.0");
        storage.put("user:1:availableBudget", "350.0");
        storage.put("user:1:usedBudget", "100.0");
        storage.put("user:1:consumedBudget", "50.0");

        
        storage.put("user:2:name", "Giulia");
        storage.put("user:1:surname", "Bianchi");
        storage.put("user:2:email", "giulia.bianchi@email.com");
        storage.put("user:2:fiscalCode", "BNCGLI95B02H501Y");
        storage.put("user:2:totalBudget", "500.0");
        storage.put("user:2:availableBudget", "500.0");
        storage.put("user:2:usedBudget", "0.0");
        storage.put("user:2:consumedBudget", "0.0");
        
        // Buoni di esempio
        storage.put("voucher:1:userId", "1");
        storage.put("voucher:1:amount", "50.0");
        storage.put("voucher:1:category", "cinema");
        storage.put("voucher:1:status", "consumed");
        storage.put("voucher:1:createdAt", "2025-06-20T10:30:00");
        storage.put("voucher:1:consumedAt", "2025-06-25T18:45:00");
        
        storage.put("voucher:2:userId", "1");
        storage.put("voucher:2:amount", "100.0");
        storage.put("voucher:2:category", "libri");
        storage.put("voucher:2:status", "active");
        storage.put("voucher:2:createdAt", "2025-06-28T09:15:00");
        
        // Contatori per ID auto-incrementali
        storage.put("counter:user", "2");
        storage.put("counter:voucher", "2");
        
        System.out.println("Loaded default test data");
    }
    
    /**
     * Operazione SET generica.
     */
    public boolean set(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        synchronized (storageLock) {
            storage.put(key, value != null ? value : "");
            return true;
        }
    }
    
    /**
     * Operazione GET generica.
     */
    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        
        return storage.get(key);
    }
    
    /**
     * Operazione DELETE generica.
     */
    public boolean delete(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        synchronized (storageLock) {
            return storage.remove(key) != null;
        }
    }
    
    /**
     * Operazione EXISTS - verifica esistenza chiave.
     */
    public boolean exists(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        return storage.containsKey(key);
    }
    
    /**
     * Operazione KEYS - trova chiavi che matchano un pattern.
     */
    public String getKeys(String pattern) {
        synchronized (storageLock) {
            var matchingKeys = storage.keySet().stream()
                .filter(key -> key.matches(pattern.replace("*", ".*")))
                .sorted()
                .toList();
            
            return String.join(",", matchingKeys);
        }
    }
    
    /**
     * Operazione INCR - incrementa valore numerico.
     */
    public String increment(String key) {
        synchronized (storageLock) {
            String currentValue = storage.get(key);
            try {
                int newValue = (currentValue == null) ? 1 : Integer.parseInt(currentValue) + 1;
                storage.put(key, String.valueOf(newValue));
                return String.valueOf(newValue);
            } catch (NumberFormatException e) {
                return null; // Errore: valore non numerico
            }
        }
    }
    
    /**
     * Operazione SIZE - restituisce numero di chiavi.
     */
    public int size() {
        return storage.size();
    }
    
    /**
     * Operazione FLUSH - cancella tutto (solo per testing).
     */
    public void flush() {
        synchronized (storageLock) {
            storage.clear();
        }
    }
}