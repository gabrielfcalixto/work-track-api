package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.dto.ClientDTO;
import br.com.gfctech.project_manager.entity.ClientEntity;
import br.com.gfctech.project_manager.exceptions.ApiException;
import br.com.gfctech.project_manager.repository.ClientRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientService {
    private final ClientRepository clients;

    public ClientService(ClientRepository clients) {
        this.clients = clients;
    }

    public List<ClientDTO> getAllClients() {
        return clients.findAll(Sort.by("name", "id")).stream().map(ClientDTO::new).toList();
    }

    public ClientDTO getClientById(Long id) {
        return new ClientDTO(findClient(id));
    }

    @Transactional
    public ClientDTO addClient(ClientDTO request) {
        var client = new ClientEntity();
        apply(client, request);
        return new ClientDTO(clients.save(client));
    }

    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO request) {
        var client = findClient(id);
        apply(client, request);
        return new ClientDTO(client);
    }

    private ClientEntity findClient(Long id) {
        return clients.findById(id).orElseThrow(() -> ApiException.notFound("Cliente não encontrado."));
    }

    private static void apply(ClientEntity client, ClientDTO request) {
        client.setName(request.name().strip());
        client.setCnpj(optionalText(request.cnpj()));
        client.setRazaoSocial(optionalText(request.razaoSocial()));
        client.setCpf(request.cpf().strip());
        client.setEmail(request.email().strip());
        client.setPhone(request.phone().strip());
        client.setAddress(optionalText(request.address()));
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
