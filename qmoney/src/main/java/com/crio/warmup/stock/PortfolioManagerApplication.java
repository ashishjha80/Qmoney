package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
//import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
//import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
//import java.util.Comparator;
import java.util.List;
import java.util.UUID;
//import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
//import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {
  
  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
    File tradesFile = resolveFileFromResources(args[0]);
    ObjectMapper objectmapper = getObjectMapper();
    PortfolioTrade[] alltrades = objectmapper.readValue(tradesFile, PortfolioTrade[].class);
    //ArrayList<TotalReturnsDto> priceOnendDate = new ArrayList<>();
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    for (PortfolioTrade trade:alltrades) {
      List<TiingoCandle> collection = resultFromApi(trade, args);
      int n = collection.size();
      Double close = collection.get(n - 1).getClose();
      annualizedReturns.add(calculateAnnualizedReturns(collection.get(n - 1).getDate(),
              trade, collection.get(0).getOpen(), close));
      
          
    }
    
    Collections.sort(annualizedReturns, 
            Comparator.comparing((AnnualizedReturn::getAnnualizedReturn)).reversed());
    return annualizedReturns;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.
  //     ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    Double totalReturns = (sellPrice - buyPrice) / buyPrice;
    long days = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    Double years = (days / 365d);
    Double annualizedReturns = Math.pow(1 + totalReturns, 1 / years) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }


  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  // TODO: CRIO_TASK_MODULE_REST_API
  //  Find out the closing price of each stock on the end_date and return the list
  //  of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  //    and deserialize the results in List<Candle>

  
  static class SortByClosePrice implements Comparator<TotalReturnsDto>,Serializable {
    public int compare(TotalReturnsDto m1, TotalReturnsDto m2) {
      return (int) (m1.getClosingPrice() - m2.getClosingPrice());
    }
  }

  public static List<TiingoCandle> resultFromApi(PortfolioTrade trade,String[] args) 
      throws IOException, URISyntaxException {
    ObjectMapper objectmapper = getObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    String symbol = trade.getSymbol().toLowerCase();
    String startDate = trade.getPurchaseDate().toString();
    String endDate = args[1];
    String token = "35388790d5696fd71ef95e094c51f0906bd7106b";
    String url = "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" 
            + startDate + "&endDate=" + endDate + "&token=" + token; 
    String stockQuotes = restTemplate.getForObject(url, String.class);
    List<TiingoCandle> collection = objectmapper.readValue(stockQuotes, 
          new TypeReference<ArrayList<TiingoCandle>>() {
      });
    return collection;  
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    File tradesFile = resolveFileFromResources(args[0]);
    ObjectMapper objectmapper = getObjectMapper();
    PortfolioTrade[] alltrades = objectmapper.readValue(tradesFile, PortfolioTrade[].class);
    ArrayList<TotalReturnsDto> priceOnendDate = new ArrayList<>(); 
    for (PortfolioTrade trade:alltrades) {
      List<TiingoCandle> collection = resultFromApi(trade, args);
      int n = collection.size();
      Double close = collection.get(n - 1).getClose();
      TotalReturnsDto e = new TotalReturnsDto(trade.getSymbol(),close);
      priceOnendDate.add(e);
      
    }
    Collections.sort(priceOnendDate,new SortByClosePrice());
    List<String> sortedSymbols =  new ArrayList<>();
    for (int i = 0;i < priceOnendDate.size();i++) {
      sortedSymbols.add(priceOnendDate.get(i).getSymbol());
      
    }
    return sortedSymbols;
  }


  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  //  Read the json file provided in the argument[0]. The file will be available in the classpath.
  //    1. Use #resolveFileFromResources to get actual file from classpath.
  //    2. Extract stock symbols from the json file with ObjectMapper provided by #getObjectMapper.
  //    3. Return the list of all symbols in the same order as provided in json.

  //  Note:
  //  1. There can be few unused imports, you will need to fix them to make the build pass.
  //  2. You can use "./gradlew build" to check if your code builds successfully.

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    
       
    File tradesFile = resolveFileFromResources(args[0]);
    ObjectMapper objectmapper = getObjectMapper();
    PortfolioTrade[] alltrades = objectmapper.readValue(tradesFile, PortfolioTrade[].class);
    List<String> allSymbols = new ArrayList<>();
    for (PortfolioTrade trade:alltrades) {
      allSymbols.add(trade.getSymbol());
    } 
    return allSymbols;
  }


  



  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }


  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  //  Follow the instructions provided in the task documentation and fill up the correct values for
  //  the variables provided. First value is provided for your reference.
  //  A. Put a breakpoint on the first line inside mainReadFile() which says
  //    return Collections.emptyList();
  //  B. Then Debug the test #mainReadFile provided in PortfoliomanagerApplicationTest.java
  //  following the instructions to run the test.
  //  Once you are able to run the test, perform following tasks and record the output as a
  //  String in the function below.
  //  Use this link to see how to evaluate expressions -
  //  https://code.visualstudio.com/docs/editor/debugging#_data-inspection
  //  1. evaluate the value of "args[0]" and set the value
  //     to the variable named valueOfArgument0 (This is implemented for your reference.)
  //  2. In the same window, evaluate the value of expression below and set it
  //  to resultOfResolveFilePathArgs0
  //     expression ==> resolveFileFromResources(args[0])
  //  3. In the same window, evaluate the value of expression below and set it
  //  to toStringOfObjectMapper.
  //  You might see some garbage numbers in the output. Dont worry, its expected.
  //    expression ==> getObjectMapper().toString()
  //  4. Now Go to the debug window and open stack trace. Put the name of the function you see at
  //  second place from top to variable functionNameFromTestFileInStackTrace
  //  5. In the same window, you will see the line number of the function in the stack trace window.
  //  assign the same to lineNumberFromTestFileInStackTrace
  //  Once you are done with above, just run the corresponding test and
  //  make sure its working as expected. use below command to do the same.
  //  ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = 
            "/home/crio-user/workspace/akashishjha80-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@47542153";
    String functionNameFromTestFileInStackTrace = "mainReadFile()";
    String lineNumberFromTestFileInStackTrace = "22";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());



    printJsonObject(mainCalculateSingleReturn(args));

  }
}

