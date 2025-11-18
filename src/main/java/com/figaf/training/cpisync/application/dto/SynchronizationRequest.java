package com.figaf.training.cpisync.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SynchronizationRequest(List<String> packageTechnicalNames) {
}
