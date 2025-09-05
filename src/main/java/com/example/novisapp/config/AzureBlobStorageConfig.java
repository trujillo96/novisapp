package com.example.novisapp.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para Azure Blob Storage
 * Sistema de gestión documental avanzado
 * Estructura de paquete plano
 */
@Configuration
public class AzureBlobStorageConfig {

    @Value("${azure.storage.connection-string:DefaultEndpointsProtocol=https;AccountName=novisapp;AccountKey=demo-key;EndpointSuffix=core.windows.net}")
    private String connectionString;

    /**
     * Cliente principal para Azure Blob Storage
     */
    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}