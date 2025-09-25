package com.example.mailsender;

import com.example.mailsender.service.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MailSenderApplicationTests {

    @Autowired
    private MailService mailService;

//    @Test
//    void simulateMailSendTimeout() throws InterruptedException, IOException {
//        // given
//        String testEmail = "test@test.com";
//        String testName = "test";
//        List<Integer> ticketNumbers = List.of(1, 2, 3);
//
//        // when
//        assertThatThrownBy(() ->
//                mailService.sendMailTest(new TicketInfo(testEmail, testName, ticketNumbers), testName))
//                .isInstanceOf(Exception.class)
//                .hasMessageContaining("timed out");
//    }
}
