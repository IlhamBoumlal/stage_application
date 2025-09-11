package com.projetPFA.backend_pfa.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    private String type = "Point";
    private List<Double> coordinates;
}
