# Progetto Sistemi Distribuiti 2024-2025 - API REST

Documentazione completa delle API REST per il sistema "Carta Cultura Giovani".

**Attenzione**: l'unica rappresentazione ammessa è in formato JSON. Vengono assunti gli header `Content-Type: application/json` e `Accept: application/json`.

## Base URL
```
http://localhost:8080
```

## Autenticazione
Il sistema utilizza sessioni basate su cookie. Dopo il login, viene impostato un cookie `CARTA_CULTURA_SESSION` che deve essere incluso in tutte le richieste successive.

---

## `/users` - Gestione Utenti

### POST `/users`
**Descrizione**: Registra un nuovo utente nel sistema.

**Parametri**: Nessuno nell'URL.

**Body richiesta**:
```json
{
  "name": "Mario",
  "surname": "Rossi", 
  "email": "mario.rossi@email.com",
  "fiscalCode": "RSSMRA90A01F205X"
}
```

**Risposta**: Oggetto utente creato con budget iniziale di 500€.

**Codici di stato**:
- 201 Created: Successo, utente registrato
- 400 Bad Request: Dati mancanti, email non valida, codice fiscale già esistente
- 500 Internal Server Error: Errore del database

---

### GET `/users/{id}`
**Descrizione**: Recupera le informazioni complete di un utente specifico.

**Parametri**: 
- `id` (path): ID numerico dell'utente

**Risposta**: Oggetto utente con tutti i campi inclusi budget aggiornati.

**Codici di stato**:
- 200 OK: Utente trovato
- 404 Not Found: Utente non esistente
- 500 Internal Server Error: Errore del database

---

### GET `/users/by-fiscal-code/{fiscalCode}`
**Descrizione**: Trova un utente tramite codice fiscale.

**Parametri**:
- `fiscalCode` (path): Codice fiscale dell'utente (16 caratteri)

**Risposta**: Oggetto utente se trovato.

**Codici di stato**:
- 200 OK: Utente trovato
- 404 Not Found: Codice fiscale non registrato
- 500 Internal Server Error: Errore del database

---

### GET `/users/{id}/budget`
**Descrizione**: Recupera solo le informazioni sui budget di un utente.

**Parametri**:
- `id` (path): ID numerico dell'utente

**Risposta**:
```json
{
  "total": 500.0,
  "available": 350.0,
  "used": 100.0,
  "consumed": 50.0
}
```

**Codici di stato**:
- 200 OK: Budget recuperato con successo
- 404 Not Found: Utente non trovato
- 500 Internal Server Error: Errore del database

---

### GET `/users`
**Descrizione**: Recupera l'elenco di tutti gli utenti registrati (funzione amministrativa).

**Parametri**: Nessuno.

**Risposta**: Array di oggetti utente.

**Codici di stato**:
- 200 OK: Lista utenti recuperata
- 500 Internal Server Error: Errore del database

---

## `/vouchers` - Gestione Buoni

### POST `/vouchers`
**Descrizione**: Crea un nuovo buono per un utente.

**Body richiesta**:
```json
{
  "userId": 1,
  "amount": 50.75,
  "category": "cinema"
}
```

**Categorie valide**: cinema, musica, concerti, eventi culturali, libri, musei, strumenti musicali, teatro, danza

**Risposta**: Oggetto buono creato con ID assegnato.

**Codici di stato**:
- 201 Created: Buono creato con successo
- 400 Bad Request: Budget insufficiente, categoria non valida, importo non valido
- 500 Internal Server Error: Errore del database

---

### GET `/vouchers/{id}`
**Descrizione**: Recupera i dettagli di un buono specifico.

**Parametri**:
- `id` (path): ID numerico del buono

**Risposta**: Oggetto buono con tutti i dettagli.

**Codici di stato**:
- 200 OK: Buono trovato
- 404 Not Found: Buono non esistente
- 500 Internal Server Error: Errore del database

---

### GET `/vouchers/user/{userId}`
**Descrizione**: Recupera tutti i buoni di un utente, ordinati cronologicamente (più recenti prima).

**Parametri**:
- `userId` (path): ID numerico dell'utente

**Risposta**: Array di oggetti buono dell'utente.

**Codici di stato**:
- 200 OK: Lista buoni recuperata (può essere vuota)
- 500 Internal Server Error: Errore del database

---

### PUT `/vouchers/{id}/consume`
**Descrizione**: Segna un buono come consumato. Operazione irreversibile.

**Parametri**:
- `id` (path): ID numerico del buono

**Body richiesta**: Vuoto.

**Risposta**:
```json
{
  "message": "Buono consumato con successo"
}
```

**Codici di stato**:
- 200 OK: Buono consumato con successo
- 400 Bad Request: Buono già consumato o non modificabile
- 404 Not Found: Buono non trovato
- 500 Internal Server Error: Errore del database

---

### DELETE `/vouchers/{id}`
**Descrizione**: Elimina un buono non ancora consumato e rilascia il budget all'utente.

**Parametri**:
- `id` (path): ID numerico del buono

**Risposta**: Nessun body (204 No Content).

**Codici di stato**:
- 204 No Content: Buono eliminato con successo
- 400 Bad Request: Buono già consumato, non eliminabile
- 404 Not Found: Buono non trovato
- 500 Internal Server Error: Errore del database

---

### PUT `/vouchers/{id}/category`
**Descrizione**: Modifica la categoria di un buono non ancora consumato.

**Parametri**:
- `id` (path): ID numerico del buono

**Body richiesta**:
```json
{
  "category": "libri"
}
```

**Risposta**:
```json
{
  "message": "Categoria aggiornata con successo"
}
```

**Codici di stato**:
- 200 OK: Categoria aggiornata
- 400 Bad Request: Categoria non valida, buono già consumato
- 404 Not Found: Buono non trovato
- 500 Internal Server Error: Errore del database

---

### GET `/vouchers`
**Descrizione**: Recupera tutti i buoni del sistema (funzione amministrativa).

**Parametri**: Nessuno.

**Risposta**: Array di tutti i buoni ordinati cronologicamente.

**Codici di stato**:
- 200 OK: Lista buoni recuperata
- 500 Internal Server Error: Errore del database

---

## `/session` - Gestione Sessioni

### POST `/session/login`
**Descrizione**: Effettua login tramite codice fiscale e crea una sessione.

**Body richiesta**:
```json
{
  "fiscalCode": "RSSMRA90A01F205X"
}
```

**Risposta**:
```json
{
  "user": { /* oggetto utente completo */ },
  "sessionId": "uuid-session-id",
  "message": "Login effettuato con successo"
}
```

**Headers risposta**: Cookie `CARTA_CULTURA_SESSION` con durata 8 ore.

**Codici di stato**:
- 200 OK: Login riuscito
- 400 Bad Request: Codice fiscale mancante
- 401 Unauthorized: Utente non trovato
- 500 Internal Server Error: Errore del database

---

### GET `/session/current`
**Descrizione**: Recupera informazioni sulla sessione corrente.

**Headers richiesta**: Cookie `CARTA_CULTURA_SESSION` automatico.

**Risposta**:
```json
{
  "user": { /* oggetto utente aggiornato */ },
  "sessionId": "uuid-session-id",
  "lastAccess": "2025-06-28T15:30:00"
}
```

**Codici di stato**:
- 200 OK: Sessione valida
- 401 Unauthorized: Sessione scaduta o non trovata
- 500 Internal Server Error: Errore del database

---

### POST `/session/logout`
**Descrizione**: Termina la sessione corrente.

**Headers richiesta**: Cookie `CARTA_CULTURA_SESSION` automatico.

**Risposta**:
```json
{
  "message": "Logout effettuato con successo"
}
```

**Headers risposta**: Cookie `CARTA_CULTURA_SESSION` cancellato.

**Codici di stato**:
- 200 OK: Logout riuscito (sempre)

---

### GET `/session/validate`
**Descrizione**: Valida una sessione esistente (uso interno).

**Headers richiesta**: Cookie `CARTA_CULTURA_SESSION` automatico.

**Risposta**:
```json
{
  "valid": true,
  "userId": 1,
  "fiscalCode": "RSSMRA90A01F205X"
}
```

**Codici di stato**:
- 200 OK: Sessione valida
- 401 Unauthorized: Sessione non valida

---

## `/system` - Statistiche Sistema

### GET `/system/stats`
**Descrizione**: Recupera statistiche globali del sistema (dashboard amministrativa).

**Parametri**: Nessuno.

**Risposta**:
```json
{
  "totalUsers": 150,
  "totalBudget": 75000.0,
  "availableBudget": 45000.0,
  "usedBudget": 20000.0,
  "consumedBudget": 10000.0,
  "totalVouchers": 500,
  "activeVouchers": 200,
  "consumedVouchers": 300
}
```

**Codici di stato**:
- 200 OK: Statistiche recuperate
- 500 Internal Server Error: Errore del database

---

### GET `/system/health`
**Descrizione**: Health check del sistema e verifica connessione database.

**Parametri**: Nessuno.

**Risposta**:
```json
{
  "status": "healthy",
  "database": "connected",
  "timestamp": "2025-06-28T15:30:00"
}
```

**Codici di stato**:
- 200 OK: Sistema funzionante
- 503 Service Unavailable: Database non raggiungibile

---

## Modelli Dati

### User
```json
{
  "id": 1,
  "name": "Mario",
  "surname": "Rossi",
  "email": "mario.rossi@email.com",
  "fiscalCode": "RSSMRA90A01F205X",
  "totalBudget": 500.0,
  "availableBudget": 350.0,
  "usedBudget": 100.0,
  "consumedBudget": 50.0,
  "registrationDate": "2025-06-20T10:30:00"
}
```

### Voucher
```json
{
  "id": 1,
  "userId": 1,
  "amount": 50.75,
  "category": {
    "displayName": "cinema"
  },
  "status": "active",
  "createdAt": "2025-06-28T09:15:00",
  "consumedAt": null
}
```

### SystemStats
```json
{
  "totalUsers": 150,
  "totalBudget": 75000.0,
  "availableBudget": 45000.0,
  "usedBudget": 20000.0,
  "consumedBudget": 10000.0,
  "totalVouchers": 500,
  "activeVouchers": 200,
  "consumedVouchers": 300
}
```

---

## Gestione Errori

Tutti gli endpoint restituiscono errori nel formato:
```json
{
  "error": "Descrizione dell'errore"
}
```

### Codici di Stato Comuni
- **200 OK**: Operazione riuscita
- **201 Created**: Risorsa creata con successo
- **204 No Content**: Operazione riuscita senza contenuto
- **400 Bad Request**: Errore nei dati inviati
- **401 Unauthorized**: Sessione non valida o login richiesto
- **404 Not Found**: Risorsa non trovata
- **500 Internal Server Error**: Errore interno del server

### Note sulla Concorrenza
- Le operazioni sui budget sono protette da lock per prevenire race condition
- La creazione contemporanea di buoni dallo stesso utente viene gestita correttamente
- Il sistema previene over-spending verificando sempre il budget disponibile