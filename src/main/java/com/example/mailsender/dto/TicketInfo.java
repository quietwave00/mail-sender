package com.example.mailsender.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class TicketInfo {
    private String email;
    private String name;
    private List<Integer> ticketNumbers;
    private int ticketCount;

    public TicketInfo(String email, String name, List<Integer> ticketNumbers) {
        this.email = email;
        this.name = name;
        this.ticketNumbers = ticketNumbers;
        this.ticketCount = ticketNumbers.size();
    }
}