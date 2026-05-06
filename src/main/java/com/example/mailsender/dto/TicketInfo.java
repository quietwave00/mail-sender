package com.example.mailsender.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class TicketInfo {
    private Integer rowId;
    private String email;
    private String name;
    private List<Integer> ticketNumbers;
    private int ticketCount;

    public TicketInfo(String email, String name, List<Integer> ticketNumbers) {
        this(null, email, name, ticketNumbers);
    }

    public TicketInfo(Integer rowId, String email, String name, List<Integer> ticketNumbers) {
        this.rowId = rowId;
        this.email = email;
        this.name = name;
        this.ticketNumbers = ticketNumbers;
        this.ticketCount = ticketNumbers.size();
    }
}
