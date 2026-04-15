
# Proof of Concept
Questo repository contiene il PoC richiesto nel Test 1 della challenge.

## Stack Tecnologico 
Per risolvere i vincoli richiesti, ho scelto Java 21 con Spring Boot 3.5 e il framework Project Reactor. In realtà avrei voluto usare Go come linguaggio per questo PoC ma non ho ancora una padronanza sufficiente del linguaggio (Go è nel mio radar tecnologico e lo approfondirò a breve). L'alternativa in Java sarebbe stata sfruttare i virtual threads in combinazione con resilience4j per gestire il rate limiting e la backpressure, ma ho preferito l'approccio reattivo in quanto più adatto a gestire flussi di dati asincroni.

Paradigma Reactive (Non-blocking I/O): A differenza dell'approccio tradizionale, il paradigma reattivo permette di gestire flussi di dati asincroni.

Backpressure: Grazie a Project Reactor, la pipeline implementa nativamente la backpressure. Se l'API esterna o il database rallentano, il sistema non accumula dati in memoria, ma segnala alla sorgente di rallentare la lettura.


## Gestione dei Vincoli Architetturali
- ### Consumo di Memoria:
Il sistema non carica mai l'intero file in memoria. Viene utilizzato Files.lines() all'interno dell'operatore Flux.using per garantire che il file venga letto riga per riga in modalità lazy. La pipeline processa i dati mantenendo in memoria solo i record strettamente necessari al momento.

- ### Concorrenza e Rate Limiting (Throttling):
Il vincolo di 50 richieste al secondo (20ms di intervallo medio) è stato gestito tramite l'operatore .zipWith() combinato con un Flux.interval(). Questi funge da "metronomo" per la lettura delle righe. Accoppiando ogni riga del file a un tick temporale, si garantisce che il dispatcher non superi mai il limite imposto dall'API esterna, indipendentemente dalla velocità di lettura del disco.
L'uso di flatMap per l'arricchimento e il salvataggio permette di gestire le latenze di rete (simulate con 100ms come suggerito) senza bloccare il thread principale di esecuzione.
E' stato utilizzato Schedulers.boundedElastic() per assicurare che il pool di thread sia ottimizzato senza saturare la CPU.

## Prerequisiti e configurazione

L'applicazione necessita di JDK 21 o superiore e Maven per la build e l'esecuzione.

Il comportamento del sistema è configurabile tramite il file application.properties:

    app.rate-limit-ms=20    # Corrisponde a 50 req/sec
    app.file-path=data.txt  # Percorso del file sorgente
## Containerizzazione (Docker)
Per distribuire il servizio in un ambiente containerizzato, è possibile utilizzare il multi-stage build per ridurre le dimensioni dell'immagine finale.
Si veda il Dockerfile presente nella sottocartella infra.

## Note sulla gestione in Cloud/Kubernetes
In un contesto di produzione:

Volume Mount: Il file da processare dovrebbe essere montato tramite un PersistentVolumeClaim o scaricato dinamicamente da un object storage (S3 compatibile/MinIO).

Scalabilità: Essendo un processo batch-like, può essere eseguito come un Kubernetes Job.