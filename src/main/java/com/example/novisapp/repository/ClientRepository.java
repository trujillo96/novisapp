package com.example.novisapp.repository;


import com.example.novisapp.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // Búsquedas básicas
    Optional<Client> findByEmail(String email);

    List<Client> findByActiveTrue();

    List<Client> findByCountry(String country);

    List<Client> findByClientType(String clientType);

    // Búsquedas por nombre (case insensitive)
    @Query("SELECT c FROM Client c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.active = true")
    List<Client> findByNameContainingIgnoreCase(@Param("name") String name);

    // Búsquedas por empresa
    @Query("SELECT c FROM Client c WHERE LOWER(c.company) LIKE LOWER(CONCAT('%', :company, '%')) AND c.active = true")
    List<Client> findByCompanyContainingIgnoreCase(@Param("company") String company);

    // Clientes con casos activos
    @Query("SELECT DISTINCT c FROM Client c JOIN c.cases ca WHERE ca.status IN ('OPEN', 'IN_PROGRESS') AND c.active = true")
    List<Client> findClientsWithActiveCases();

    // Estadísticas
    @Query("SELECT COUNT(c) FROM Client c WHERE c.active = true")
    long countActiveClients();

    @Query("SELECT COUNT(c) FROM Client c WHERE c.country = :country AND c.active = true")
    long countActiveClientsByCountry(@Param("country") String country);

    @Query("SELECT c.country, COUNT(c) FROM Client c WHERE c.active = true GROUP BY c.country")
    List<Object[]> countClientsByCountry();

    @Query("SELECT c.clientType, COUNT(c) FROM Client c WHERE c.active = true GROUP BY c.clientType")
    List<Object[]> countClientsByType();
}