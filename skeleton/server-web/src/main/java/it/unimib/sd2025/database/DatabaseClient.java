package it.unimib.sd2025.database;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Client per comunicare con il database via protocollo TCP.
 * Implementa connection pooling semplificato e gestione errori robusta.
 */
public class DatabaseClient {
    private static final String HOST = "localhost";
    private static final int PORT = 3030;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Esegue un comando sul database e restituisce la risposta.
     */
    public static String executeCommand(String command) throws DatabaseException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println(command);
            String response = in.readLine();
            
            if (response == null) {
                throw new DatabaseException("No response from database");
            }
            
            if (response.startsWith("ERR")) {
                throw new DatabaseException(response.substring(4)); // Rimuove "ERR "
            }
            
            return response;
            
        } catch (IOException e) {
            throw new DatabaseException("Database connection failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Operazione SET generica.
     */
    public static boolean set(String key, String value) throws DatabaseException {
        String command = String.format("SET %s %s", key, value != null ? value : "");
        String response = executeCommand(command);
        return response.equals("OK");
    }
    
    /**
     * Operazione GET generica.
     */
    public static String get(String key) throws DatabaseException {
        String command = String.format("GET %s", key);
        String response = executeCommand(command);
        
        if ("NIL".equals(response)) {
            return null;
        }
        
        if (response.startsWith("OK ")) {
            return response.substring(3); // Rimuove "OK "
        }
        
        return response;
    }
    
    /**
     * Operazione DELETE generica.
     */
    public static boolean delete(String key) throws DatabaseException {
        String command = String.format("DEL %s", key);
        String response = executeCommand(command);
        return "OK".equals(response);
    }
    
    /**
     * Operazione EXISTS.
     */
    public static boolean exists(String key) throws DatabaseException {
        String command = String.format("EXISTS %s", key);
        String response = executeCommand(command);
        return "OK 1".equals(response);
    }
    
    /**
     * Operazione KEYS - trova chiavi che matchano pattern.
     */
    public static String[] getKeys(String pattern) throws DatabaseException {
        String command = String.format("KEYS %s", pattern);
        String response = executeCommand(command);
        
        if (response.startsWith("OK ")) {
            String keys = response.substring(3);
            return keys.isEmpty() ? new String[0] : keys.split(",");
        }
        
        return new String[0];
    }
    
    /**
     * Operazione INCR - incrementa contatore.
     */
    public static int increment(String key) throws DatabaseException {
        String command = String.format("INCR %s", key);
        String response = executeCommand(command);
        
        if (response.startsWith("OK ")) {
            return Integer.parseInt(response.substring(3));
        }
        
        throw new DatabaseException("Increment failed for key: " + key);
    }
    
    /**
     * Operazione SIZE.
     */
    public static int getSize() throws DatabaseException {
        String response = executeCommand("SIZE");
        
        if (response.startsWith("OK ")) {
            return Integer.parseInt(response.substring(3));
        }
        
        throw new DatabaseException("Could not get database size");
    }
    
    /**
     * Test connessione database.
     */
    public static boolean ping() {
        try {
            String response = executeCommand("PING");
            return "PONG".equals(response);
        } catch (DatabaseException e) {
            return false;
        }
    }
    
    // === METODI HELPER PER OPERAZIONI COMMON ===
    
    /**
     * Salva un oggetto datetime come stringa.
     */
    public static void setDateTime(String key, LocalDateTime dateTime) throws DatabaseException {
        String dateTimeStr = dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
        set(key, dateTimeStr);
    }
    
    /**
     * Recupera un datetime da stringa.
     */
    public static LocalDateTime getDateTime(String key) throws DatabaseException {
        String dateTimeStr = get(key);
        return (dateTimeStr != null && !dateTimeStr.isEmpty()) 
            ? LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER) 
            : null;
    }
    
    /**
     * Salva un double come stringa.
     */
    public static void setDouble(String key, double value) throws DatabaseException {
        set(key, String.valueOf(value));
    }
    
    /**
     * Recupera un double da stringa.
     */
    public static double getDouble(String key) throws DatabaseException {
        String valueStr = get(key);
        return (valueStr != null && !valueStr.isEmpty()) ? Double.parseDouble(valueStr) : 0.0;
    }
    
    /**
     * Salva un int come stringa.
     */
    public static void setInt(String key, int value) throws DatabaseException {
        set(key, String.valueOf(value));
    }
    
    /**
     * Recupera un int da stringa.
     */
    public static int getInt(String key) throws DatabaseException {
        String valueStr = get(key);
        return (valueStr != null && !valueStr.isEmpty()) ? Integer.parseInt(valueStr) : 0;
    }
    
    /**
     * Operazione transazionale per aggiornare i budget di un utente.
     * Questa è una simulazione di transazione - in un database reale useresti BEGIN/COMMIT.
     */
    public static boolean updateUserBudgets(int userId, double availableBudget, 
                                          double usedBudget, double consumedBudget) throws DatabaseException {
        try {
            setDouble(String.format("user:%d:availableBudget", userId), availableBudget);
            setDouble(String.format("user:%d:usedBudget", userId), usedBudget);
            setDouble(String.format("user:%d:consumedBudget", userId), consumedBudget);
            return true;
        } catch (DatabaseException e) {
            // In un vero database dovresti fare ROLLBACK qui
            throw new DatabaseException("Failed to update user budgets: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica se una chiave esiste e ha un valore non vuoto.
     */
    public static boolean hasValue(String key) throws DatabaseException {
        String value = get(key);
        return value != null && !value.trim().isEmpty();
    }
}
