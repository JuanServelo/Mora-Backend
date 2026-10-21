package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.CriarUsuarioDTO;
import portaria.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Cadastro unificado de morador, visitante ou funcionário.")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/criar")
    @Operation(summary = "Cadastrar pessoa", description = "Cria um morador, visitante ou funcionário conforme `tipoUsuario`; cada tipo usa os campos dele.")
    @ApiResponse(responseCode = "201", description = "Pessoa criada.")
    public ResponseEntity<Object> criarUsuario(@Valid @RequestBody CriarUsuarioDTO dto) {
        Object usuario = usuarioService.criarUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }
}
