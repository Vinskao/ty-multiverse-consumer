package com.vinskao.ty_multiverse_consumer.module.people.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.PeopleImage;
import com.vinskao.ty_multiverse_consumer.module.people.service.PeopleImageService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/people-images")
public class PeopleImageController {
    
    @Autowired
    private PeopleImageService peopleImageService;
    
    /**
     * Get all people images
     */
    @GetMapping
    public Flux<PeopleImage> getAllPeopleImages() {
        return peopleImageService.getAllPeopleImages();
    }
    
    /**
     * Get people image by code name
     */
    @GetMapping("/{codeName}")
    public Mono<ResponseEntity<PeopleImage>> getPeopleImageByCodeName(@PathVariable String codeName) {
        return peopleImageService.getPeopleImageByCodeName(codeName)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Create a new people image
     */
    @PostMapping
    public Mono<ResponseEntity<PeopleImage>> createPeopleImage(@RequestBody PeopleImage peopleImage) {
        return peopleImageService.savePeopleImage(peopleImage)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }
    
    /**
     * Delete people image by code name
     */
    @DeleteMapping("/{codeName}")
    public Mono<ResponseEntity<Void>> deletePeopleImage(@PathVariable String codeName) {
        return peopleImageService.deletePeopleImage(codeName)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
