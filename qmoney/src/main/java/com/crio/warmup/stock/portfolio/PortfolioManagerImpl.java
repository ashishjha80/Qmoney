
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF

  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws JsonProcessingException {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    for (PortfolioTrade trade:portfolioTrades) {
      List<TiingoCandle> collection = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      int n = collection.size();
      Double close = collection.get(n - 1).getClose();
      annualizedReturns.add(calculateAnnualizedReturns(collection.get(n - 1).getDate(),
              trade, collection.get(0).getOpen(), close));
      
          
    }
    
    Collections.sort(annualizedReturns, getComparator());
    return annualizedReturns;
  }

  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    Double totalReturns = (sellPrice - buyPrice) / buyPrice;
    long days = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    Double years = (days / 365d);
    Double annualizedReturns = Math.pow(1 + totalReturns, 1 / years) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<TiingoCandle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    
    String uri = buildUri(symbol, from, to);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    //RestTemplate restTemplate = new RestTemplate();
    String stockQuotes = restTemplate.getForObject(uri, String.class);
    List<TiingoCandle> collection = objectMapper.readValue(stockQuotes, 
          new TypeReference<ArrayList<TiingoCandle>>() {
      });
    return collection;
    
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    //  String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
    //      + "startDate=$STARTDATE&endDate=$ENDDATE&token=35388790d5696fd71ef95e094c51f0906bd7106b";
    String token = "35388790d5696fd71ef95e094c51f0906bd7106b";
    return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" 
            + startDate.toString() + "&endDate=" + endDate.toString() + "&token=" + token;
  }
}
