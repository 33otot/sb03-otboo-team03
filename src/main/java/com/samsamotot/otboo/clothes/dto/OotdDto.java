package com.samsamotot.otboo.clothes.dto;

import com.samsamotot.otboo.clothes.entity.ClothesType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record OotdDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

    public String toSummaryStringWithDefs() {
        // 1) 레이블: 타입 + 이름(있으면 그대로)
        String label = (type != null ? type.name() : "알수없음");
        if (name != null && !name.isBlank()) {
            label += " - " + name.trim();
        }

        // 2) 속성 수집
        if (attributes == null || attributes.isEmpty()) {
            return label;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (ClothesAttributeWithDefDto attr : attributes) {
            if (attr == null) continue;
            String def = attr.definitionName();
            String val = attr.value();
            if (def != null && val != null && !val.isBlank()) map.put(def, val);
        }

        // 3) 표시 순서(우선 키 먼저)
        List<String> order = List.of("두께", "방수", "스타일");
        List<String> parts = new ArrayList<>();
        for (String key : order) {
            if (map.containsKey(key)) parts.add(key + ": " + map.get(key));
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!order.contains(e.getKey())) parts.add(e.getKey() + ": " + e.getValue());
        }

        return parts.isEmpty() ? label : label + " (" + String.join(", ", parts) + ")";
    }
}
