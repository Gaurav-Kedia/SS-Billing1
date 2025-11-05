package com.example.billing.catalog;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HsnCodeCatalog {

    private final Map<String, String> hsnToDescription = new LinkedHashMap<>();

    public HsnCodeCatalog() {
        hsnToDescription.put("44189090", "PVC & Aluminium Doors and Windows");
        hsnToDescription.put("70080000", "Toughened/Tempered Glass Panels");
        hsnToDescription.put("76101000", "Aluminium Doors, Windows and Frames");
        hsnToDescription.put("39253000", "Plastic Shutters and Fittings");
        hsnToDescription.put("73083000", "Steel Structures and Frames");
    }

    public Map<String, String> getHsnToDescription() {
        return hsnToDescription;
    }
}
