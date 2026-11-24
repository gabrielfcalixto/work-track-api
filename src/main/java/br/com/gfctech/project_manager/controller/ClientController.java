package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.ClientDTO;
import br.com.gfctech.project_manager.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ClientController {
    private final ClientService clients;

    public ClientController(ClientService clients) {
        this.clients = clients;
    }

    @GetMapping
    public List<ClientDTO> list() {
        return clients.getAllClients();
    }

    @GetMapping("/{id}")
    public ClientDTO get(@PathVariable Long id) {
        return clients.getClientById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDTO create(@Valid @RequestBody ClientDTO request) {
        return clients.addClient(request);
    }

    @PutMapping("/{id}")
    public ClientDTO update(@PathVariable Long id, @Valid @RequestBody ClientDTO request) {
        return clients.updateClient(id, request);
    }
}
