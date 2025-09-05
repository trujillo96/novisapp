package com.example.novisapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String company; // Empresa del cliente (si aplica)

    @Column(length = 50)
    private String country;

    @Column(length = 50)
    private String clientType; // "Individual", "Empresa", "Gobierno", etc.

    @Column(length = 1000)
    private String notes; // Notas adicionales sobre el cliente

    @Column(nullable = false)
    private Boolean active = true;

    // Relación con casos
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LegalCase> cases = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Método utilitario para obtener información de contacto
    public String getContactInfo() {
        StringBuilder contact = new StringBuilder(email);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            contact.append(" - ").append(phoneNumber);
        }
        return contact.toString();
    }
}