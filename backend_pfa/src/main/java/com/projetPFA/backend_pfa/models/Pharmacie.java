package com.projetPFA.backend_pfa.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "pharmacies")
@Data
public class Pharmacie {
    @Id
    private String id;
    private String name;
    private String address;
    private String phone;
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Medicament> getMedicamentsDisponibles() {
        return medicamentsDisponibles;
    }

    public void setMedicamentsDisponibles(List<Medicament> medicamentsDisponibles) {
        this.medicamentsDisponibles = medicamentsDisponibles;
    }

    private boolean enService;
    private List<Medicament> medicamentsDisponibles;

    // Nouveau constructeur pour le scraping
    public Pharmacie(String name, String address) {
        this.name = name;
        this.address = address;
        // Initialiser d'autres champs à des valeurs par défaut si nécessaire
        this.enService = false; // Par exemple, false par défaut
        this.phone = null;
        this.medicamentsDisponibles = null;
        this.location = null;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnService() {
        return enService;
    }

    public void setEnService(boolean enService) {
        this.enService = enService;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
//sert à créer un index géospatial sur un champ de type position (Point) dans MongoDB
    //Elle permet à MongoDB d’exécuter des requêtes géospatiales efficaces
    // permet d’activer les recherches par proximité (longitude/latitude) dans MongoDB,
    // comme une sorte de "Google Maps interne".

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private Point location;

    // NOUVELLES méthodes utilitaires pour la géolocalisation
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }

    public void setLatitude(Double latitude) {
        if (this.location == null) {
            this.location = new Point(0.0, latitude);
        } else {
            this.location = new Point(this.location.getX(), latitude);
        }
    }

    public void setLongitude(Double longitude) {
        if (this.location == null) {
            this.location = new Point(longitude, 0.0);
        } else {
            this.location = new Point(longitude, this.location.getY());
        }
    }


}
