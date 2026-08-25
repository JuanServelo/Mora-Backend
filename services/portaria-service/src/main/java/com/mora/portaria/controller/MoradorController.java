package com.mora.portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mora.portaria.entity.Morador;
import com.mora.portaria.service.MoradorService;

import java.util.List;

@RestController
@RequestMapping("/moradores")
@RequiredArgsConstructor
public class MoradorController {

    private final MoradorService moradorService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Morador> cadastrar(@Valid @RequestBody Morador morador) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moradorService.cadastrar(morador));
    }

    @GetMapping
    public List<Morador> listarAtivos() {
        return moradorService.listarAtivos();
    }

    @GetMapping("/todos")
    public List<Morador> listarTodos() {
        return moradorService.listarTodos();
    }

    @GetMapping("/{id}")
    public Morador buscarPorId(@PathVariable String id) {
        return moradorService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Morador atualizar(@PathVariable String id, @Valid @RequestBody Morador dados) {
        return moradorService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        moradorService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}

