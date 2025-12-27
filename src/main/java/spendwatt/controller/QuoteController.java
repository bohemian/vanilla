package spendwatt.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import spendwatt.model.Quote;
import spendwatt.model.QuoteParams;
import spendwatt.service.QuoteService;

@RestController
public class QuoteController {
    @Autowired
    private QuoteService quoteService;

    @PostMapping("/quote")
    public Quote getQuote(@Valid @RequestBody QuoteParams params) {
        return quoteService.getQuote(params);
    }
}
