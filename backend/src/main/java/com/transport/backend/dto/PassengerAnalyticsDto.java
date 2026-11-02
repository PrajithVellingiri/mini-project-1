package com.transport.backend.dto;

public record PassengerAnalyticsDto(
        int totalPassengers,
        int waitingPassengers,
        int inTransitPassengers,
        int completedPassengers,
        boolean conservationValid
) {}
