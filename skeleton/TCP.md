# Progetto Sistemi Distribuiti 2024-2025 - Protocollo TCP

Documentazione del protocollo TCP per il database chiave-valore del sistema "Carta Cultura Giovani".

## 1. Panoramica

- **Tipo**: Protocollo testuale (ispirato a Redis)
- **Porta**: 3030
- **Encoding**: UTF-8
- **Separatore comandi**: Line Feed (`\n`)
- **Timeout sessione**: Nessuno (connessione persistente fino a QUIT)

## 2. Struttura dei Messaggi

### 2.1. Formato Generale

Il protocollo utilizza messaggi testuali con la seguente struttura:

```
COMANDO [arg1] [arg2] [...]\n
```

- I comandi sono case-insensitive ma per convenzione si usano in MAIUSCOLO
- Gli argomenti sono separati da spazi singoli
- Ogni comando termina con un Line Feed (`\n`)
- Se un valore contiene spazi, tutti gli argomenti dal secondo in poi vengono concatenati

### 2.2. Formato delle Risposte

Le risposte del server seguono questo formato:

```
STATO [dati]\n
```

**Stati possibili**:
- `OK [dati]`: Operazione riuscita, dati opzionali
- `NIL`: Valore non trovato (per GET)
- `ERR [messaggio]`: Errore nell'operazione
- `PONG`: Risposta a PING
- `BYE`: Conferma disconnessione

## 3. Comandi Supportati

### 3.1. Comandi Base

| Comando | Parametri | Descrizione | Esempio |
|---------|-----------|-------------|---------|
| `SET` | `chiave valore` | Imposta valore per una chiave | `SET user:1:name Mario Rossi` |
| `GET` | `chiave` | Recupera valore di una chiave | `GET user:1:name` |
| `DEL`/`DELETE` | `chiave` | Elimina una chiave | `DEL user:1:name` |
| `EXISTS` | `chiave` | Verifica esistenza chiave | `EXISTS user:1:name` |

### 3.2. Comandi Avanzati

| Comando | Parametri | Descrizione | Esempio |
|---------|-----------|-------------|---------|
| `KEYS` | `pattern` | Trova chiavi che matchano pattern | `KEYS user:*:name` |
| `INCR` | `chiave` | Incrementa valore numerico | `INCR counter:user` |
| `SIZE` | - | Restituisce numero totale chiavi | `SIZE` |
| `FLUSH` | - | Cancella tutto il database | `FLUSH` |

### 3.3. Comandi di Sistema

| Comando | Parametri | Descrizione | Esempio |
|---------|-----------|-------------|---------|
| `PING` | - | Test connessione | `PING` |
| `QUIT` | - | Chiude connessione | `QUIT` |

## 4. Dettaglio Comandi

### 4.1. SET - Imposta Valore

**Sintassi**: `SET chiave valore`

**Descrizione**: Salva un valore stringa associato a una chiave. Se la chiave esiste già, il valore viene sovrascritto.

**Esempi**:
```
> SET user:1:name Mario Rossi
< OK

> SET user:1:budget 500.0
< OK

> SET counter:voucher 1
< OK
```

**Risposte**:
- `OK`: Valore impostato correttamente
- `ERR invalid key`: Chiave non valida (vuota o null)

---

### 4.2. GET - Recupera Valore

**Sintassi**: `GET chiave`

**Descrizione**: Recupera il valore associato a una chiave.

**Esempi**:
```
> GET user:1:name
< OK Mario Rossi

> GET user:999:name
< NIL

> GET user:1:budget
< OK 500.0
```

**Risposte**:
- `OK [valore]`: Valore trovato
- `NIL`: Chiave non esistente
- `ERR GET requires exactly one key`: Numero parametri errato

---

### 4.3. DELETE - Elimina Chiave

**Sintassi**: `DEL chiave` o `DELETE chiave`

**Descrizione**: Rimuove una chiave e il suo valore dal database.

**Esempi**:
```
> DEL user:1:temp
< OK

> DELETE user:999:name
< NIL
```

**Risposte**:
- `OK`: Chiave eliminata
- `NIL`: Chiave non esistente
- `ERR DELETE requires exactly one key`: Numero parametri errato

---

### 4.4. EXISTS - Verifica Esistenza

**Sintassi**: `EXISTS chiave`

**Descrizione**: Verifica se una chiave esiste nel database.

**Esempi**:
```
> EXISTS user:1:name
< OK 1

> EXISTS user:999:name
< OK 0
```

**Risposte**:
- `OK 1`: Chiave esistente
- `OK 0`: Chiave non esistente
- `ERR EXISTS requires exactly one key`: Numero parametri errato

---

### 4.5. KEYS - Ricerca Pattern

**Sintassi**: `KEYS pattern`

**Descrizione**: Trova tutte le chiavi che corrispondono al pattern specificato. Usa `*` come wildcard.

**Esempi**:
```
> KEYS user:*:name
< OK user:1:name,user:2:name,user:3:name

> KEYS voucher:*:amount
< OK voucher:1:amount,voucher:2:amount

> KEYS nonexistent:*
< OK 
```

**Risposte**:
- `OK [lista_chiavi]`: Lista di chiavi separate da virgola (può essere vuota)
- Pattern di default se non specificato: `*` (tutte le chiavi)

---

### 4.6. INCR - Incrementa Contatore

**Sintassi**: `INCR chiave`

**Descrizione**: Incrementa di 1 il valore numerico di una chiave. Se la chiave non esiste, la crea con valore 1.

**Esempi**:
```
> INCR counter:user
< OK 1

> INCR counter:user
< OK 2

> SET test:string "hello"
< OK

> INCR test:string
< ERR value is not a number
```

**Risposte**:
- `OK [nuovo_valore]`: Incremento riuscito
- `ERR value is not a number`: Valore non numerico
- `ERR INCR requires exactly one key`: Numero parametri errato

---

### 4.7. SIZE - Dimensione Database

**Sintassi**: `SIZE`

**Descrizione**: Restituisce il numero totale di chiavi nel database.

**Esempi**:
```
> SIZE
< OK 25

> FLUSH
< OK

> SIZE
< OK 0
```

**Risposte**:
- `OK [numero]`: Numero di chiavi presenti

---

### 4.8. FLUSH - Cancella Database

**Sintassi**: `FLUSH`

**Descrizione**: Rimuove tutte le chiavi dal database. **ATTENZIONE**: Operazione irreversibile!

**Esempi**:
```
> FLUSH
< OK

> SIZE
< OK 0
```

**Risposte**:
- `OK`: Database svuotato

---

### 4.9. PING - Test Connessione

**Sintassi**: `PING`

**Descrizione**: Verifica che la connessione sia attiva.

**Esempi**:
```
> PING
< PONG
```

**Risposte**:
- `PONG`: Connessione attiva

---

### 4.10. QUIT - Chiudi Connessione

**Sintassi**: `QUIT`

**Descrizione**: Termina la connessione in modo pulito.

**Esempi**:
```
> QUIT
< BYE
[connessione chiusa]
```

**Risposte**:
- `BYE`: Conferma disconnessione

## 5. Gestione degli Errori

### 5.1. Tipi di Errore

| Errore | Causa | Esempio |
|--------|-------|---------|
| `ERR empty command` | Comando vuoto | `> \n` |
| `ERR unknown command: XXX` | Comando non riconosciuto | `> INVALID\n` |
| `ERR [comando] requires exactly one key` | Numero parametri errato | `> GET\n` |
| `ERR [comando] requires key and value` | Parametri mancanti per SET | `> SET onlykey\n` |
| `ERR value is not a number` | Tentativo INCR su valore non numerico | `> INCR text:key\n` |
| `ERR invalid key` | Chiave non valida (vuota) | `> SET "" value\n` |

### 5.2. Gestione Disconnessioni

- **Disconnessione client**: Il server chiude automaticamente la connessione
- **Errore di rete**: La connessione viene terminata senza notifica
- **Timeout**: Non implementato (connessioni persistenti)

## 6. Esempi di Sessione Completa

### 6.1. Scenario: Gestione Utente

```
> PING
< PONG

> SET user:1:name Mario Rossi
< OK

> SET user:1:email mario.rossi@email.com
< OK

> SET user:1:budget 500.0
< OK

> GET user:1:name
< OK Mario Rossi

> EXISTS user:1:phone
< OK 0

> KEYS user:1:*
< OK user:1:name,user:1:email,user:1:budget

> INCR counter:user
< OK 1

> QUIT
< BYE
```

### 6.2. Scenario: Gestione Errori

```
> GET
< ERR GET requires exactly one key

> UNKNOWN_COMMAND test
< ERR unknown command: UNKNOWN_COMMAND

> SET user:1:age "venticinque"
< OK

> INCR user:1:age
< ERR value is not a number

> GET user:999:name
< NIL

> DEL user:999:name
< NIL
```

## 7. Pattern Architetturali

### 7.1. Convenzioni Chiavi

Il database usa convenzioni per organizzare le chiavi in namespace logici:

- **Utenti**: `user:{id}:{campo}` (es. `user:1:name`, `user:1:budget`)
- **Buoni**: `voucher:{id}:{campo}` (es. `voucher:1:amount`, `voucher:1:status`)
- **Contatori**: `counter:{tipo}` (es. `counter:user`, `counter:voucher`)

### 7.2. Operazioni Atomiche

Il database gestisce la concorrenza attraverso:
- **Lock esplicito**: Ogni operazione è atomica a livello di singola chiave
- **Thread safety**: Uso di `ConcurrentHashMap` per operazioni concurrent-safe
- **Sincronizzazione**: Lock esplicito per operazioni che richiedono atomicità

### 7.3. Persistenza

- **In-memory**: Tutti i dati sono mantenuti in RAM
- **Inizializzazione**: Caricamento dati da file `data/initial_data.properties`
- **Backup**: Non implementato (dati persi al riavvio)

## 8. Limitazioni e Considerazioni

### 8.1. Limitazioni Attuali

- **Persistence**: No salvataggio su disco (solo in-memory)
- **Authentication**: Nessuna autenticazione client
- **Encryption**: Comunicazione in chiaro (non SSL/TLS)
- **Backup**: Nessun sistema di backup automatico
- **Replication**: Nessuna replica o clustering

### 8.2. Scalabilità

- **Concorrenza**: Gestita tramite thread pool e lock
- **Memory**: Limitata dalla RAM disponibile
- **Performance**: Ottimizzata per workload read-heavy
- **Connections**: Nessun limite esplicito di connessioni

### 8.3. Monitoraggio

- **Logging**: Log base su stdout/stderr
- **Metrics**: Solo comando SIZE per statistiche
- **Health check**: Comando PING per verifica stato

---

**Nota**: Questo protocollo è progettato per essere semplice ma efficace, seguendo i principi KISS (Keep It Simple, Stupid) mantenendo comunque robustezza e funzionalità necessarie per il progetto.