package com.example.mailsender.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class TicketInfo {
    private String email;
    private String name;
    private List<Integer> bookingNo;

    public TicketInfo(String email, String name, List<Integer> bookingNo) {
        this.email = email;
        this.name = name;
        this.bookingNo = bookingNo;
    }
}