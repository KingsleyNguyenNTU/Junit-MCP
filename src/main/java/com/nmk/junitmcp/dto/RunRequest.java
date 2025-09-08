package com.nmk.junitmcp.dto;

import lombok.Data;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;

import java.util.ArrayList;
import java.util.List;

@Data
public class RunRequest {
    private List<String> tests; // e.g. ["com.example.UserServiceTest#shouldCreateUser"]

    public List<DiscoverySelector> toSelectors() {
        List<DiscoverySelector> selectors = new ArrayList<>();
        if (tests != null) {
            for (String t : tests) {
                if (t.contains("#")) {
                    String[] parts = t.split("#");
                    selectors.add(DiscoverySelectors.selectMethod(parts[0], parts[1]));
                } else {
                    selectors.add(DiscoverySelectors.selectClass(t));
                }
            }
        }
        return selectors;
    }
}
