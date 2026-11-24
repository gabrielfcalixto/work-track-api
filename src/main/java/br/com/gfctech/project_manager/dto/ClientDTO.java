package br.com.gfctech.project_manager.dto;

import br.com.gfctech.project_manager.entity.ClientEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientDTO(Long id,
                        @NotBlank(message = "Informe o nome do cliente.") @Size(max = 255) String name,
                        @Size(max = 30) String cnpj,
                        @Size(max = 255) String razaoSocial,
                        @NotBlank(message = "Informe o CPF do contato.") @Size(max = 20) String cpf,
                        @NotBlank(message = "Informe o e-mail do cliente.") @Email @Size(max = 255) String email,
                        @NotBlank(message = "Informe o telefone do cliente.") @Size(max = 30) String phone,
                        @Size(max = 255) String address) {
    public ClientDTO(ClientEntity client) {
        this(client.getId(), client.getName(), client.getCnpj(), client.getRazaoSocial(),
                client.getCpf(), client.getEmail(), client.getPhone(), client.getAddress());
    }
}
