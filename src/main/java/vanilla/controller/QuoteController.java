package vanilla.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vanilla.model.Quote;
import vanilla.model.QuoteParams;
import vanilla.service.QuoteService;

@RestController
public class QuoteController {
    @Autowired
    private QuoteService quoteService;

    @PostMapping(value = "/quote", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Quote getQuote(@Valid @RequestBody QuoteParams params) {
        return quoteService.getQuote(params);
    }
}
