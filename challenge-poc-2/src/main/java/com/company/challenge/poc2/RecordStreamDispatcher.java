package com.company.challenge.poc2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.company.challenge.poc2.entity.EventRecord;
import com.company.challenge.poc2.repository.EventRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RecordStreamDispatcher implements CommandLineRunner {

    private static final Duration ENRICHMENT_LATENCY = Duration.ofMillis(100);
    private static final String ENRICHED_PREFIX = "OK >> ";
    private static final String ENRICHMENT_TAG = " [ARCHIVED]";

    @Value("${app.rate-limit-ms}")
    private int tickIntervalMs;

    @Value("${app.file-path}")
    private String filePath;

    private final EventRepository eventRepository;
    private final AtomicInteger dispatched = new AtomicInteger(0);

    public RecordStreamDispatcher(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(String... args) {
        dispatch(filePath);
    }

    public void dispatch(String source) {
        log.info("Dispatcher avviato — sorgente: {}", source);

        streamLines(source)
                .subscribeOn(Schedulers.boundedElastic())
                // 1. Controllo del rate (Throttling)
                .zipWith(Flux.interval(Duration.ofMillis(tickIntervalMs))) // Genera un flusso di intervalli di tempo e lo abbina al flusso di righe, in modo che vengano processate una ogni tickIntervalMs
                .map(tuple -> tuple.getT1()) // Prende solo il primo elemento della coppia (la riga)
                // 2. Arricchimento (Simula chiamata a servizio esterno)
                .flatMap(this::enrichFromApiCall)
                // 3. Salvataggio su Database 
                .flatMap(this::saveToDb)
                .doOnNext(out -> log.debug("Processato e salvato: {}", out))
                .doOnComplete(() -> log.info("Dispatcher terminato - totale: {}", dispatched.get()))
                .doOnError(e -> log.error("Errore nella pipeline: ", e))
                .subscribe();
    }

    private Flux<String> streamLines(String source) {
        return Flux.using(
                () -> Files.lines(Paths.get(source)),
                Flux::fromStream,
                java.util.stream.BaseStream::close
        );
    }

    private Mono<String> enrichFromApiCall(String record) {
        return Mono.just(ENRICHED_PREFIX + record + " " + ENRICHMENT_TAG)
                .delayElement(ENRICHMENT_LATENCY)
                .doOnNext(r -> log.trace("Arricchito: {}", r));
    }

    private Mono<EventRecord> saveToDb(String content) {
        EventRecord record = new EventRecord(null, content);
        return eventRepository.save(record)
                .doOnNext(r -> dispatched.incrementAndGet());
    }

}