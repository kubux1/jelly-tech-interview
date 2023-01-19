package com.marcura.intergration

import com.marcura.BaseSpec
import com.marcura.client.FixerClient
import com.marcura.model.ApiError
import com.marcura.model.api.request.NewCurrencyExchange
import com.marcura.model.api.response.CurrencyExchange
import com.marcura.model.entity.ExchangeRate
import com.marcura.model.flixer.LatestExchangeRate
import com.marcura.repository.CurrencyExchangeRateRepository
import com.marcura.service.CurrencyExchangeService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

import java.time.Instant
import java.time.LocalDate

class CurrencyExchangeIntegrationSpec extends BaseSpec {
    private static final String EXCHANGE_ENDPOINT = "/exchange"
    private static final String GET_EXCHANGE_ENDPOINT = EXCHANGE_ENDPOINT + "?from=%s&to=%s"
    private static final String GET_EXCHANGE_ENDPOINT_WITH_DATE = GET_EXCHANGE_ENDPOINT + "&date=%s"
    private static final LocalDate INSERTED_CURRENCY_EXCHANGE_DATE = LocalDate.of(2023, 01, 01)
    private static final String VALID_CURRENCY_FROM = "EUR"
    private static final String VALID_CURRENCY_TO = "PLN"
    private static final String VALID_DATE = "2021-01-01"

    @Autowired
    private TestRestTemplate testRestTemplate

    @Autowired
    private CurrencyExchangeService currencyExchangeService

    @Autowired
    private CurrencyExchangeRateRepository currencyExchangeRateRepository

    @SpringBean
    private FixerClient fixerClient = Mock()

    @Value('${api.fixer.access-key}')
    private String fixerApiAccessKey

    @Value('${base-currency}')
    private String baseCurrency

    def setup() {
        jdbcTemplate.execute("TRUNCATE currency_exchange_rate")
        jdbcTemplate.execute("INSERT INTO currency_exchange_rate (currency_from, currency_to, rate, exchange_date)\n" +
                                     "VALUES ('USD', 'PLN', 3.7, '2023-01-01'),\n" +
                                     "       ('USD', 'EUR', 0.8, '2023-01-01');")
    }

    def "shouldReturnCurrencyExchangeAndOkResponse"() {
        when:
        final def response = testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT.formatted(currencyFrom, currencyTo),
                                                           CurrencyExchange.class)

        then:
        response.statusCode == HttpStatus.OK
        verifyAll(response.body) {
            it.from().equalsIgnoreCase(currencyFrom)
            it.to().equalsIgnoreCase(currencyTo)
            it.exchange() == BigDecimal.valueOf(expectedExchnageRate)
        }

        where:
        currencyFrom << ["EUR", "PLN", "USD"]
        currencyTo << ["PLN", "EUR", "PLN"]
        expectedExchnageRate << [4.49781250, 0.21025450, 3.598250]
    }

    def "shouldReturnCurrencyExchangeAndOkResponseWhenOptionalDateParamIsPresent"() {
        given:
        final def currencyFrom = "EUR"
        final def currencyTo = "PLN"

        when:
        final def response =
                testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT_WITH_DATE.formatted(
                        currencyFrom, currencyTo, LocalDate.now()), CurrencyExchange.class)

        then:
        response.statusCode == HttpStatus.OK
        verifyAll(response.body) {
            it.from().equalsIgnoreCase(currencyFrom)
            it.to().equalsIgnoreCase(currencyTo)
            it.exchange() == BigDecimal.valueOf(4.49781250000000)
        }
    }

    def "shouldIncreaseCounterWhenGetCurrencyExchangeRate"() {
        given:
        final def currencyFrom = "EUR"
        final def currencyTo = "PLN"

        when:
        testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT.formatted(currencyFrom, currencyTo), CurrencyExchange.class)
        testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT.formatted(currencyFrom, currencyTo), CurrencyExchange.class)
        testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT.formatted(currencyFrom, currencyTo), CurrencyExchange.class)

        then:
        final def allCurrencies = currencyExchangeRateRepository.findAll()
        final def accessedCurrencyFrom = findExchangeRateWithCurrency(allCurrencies, currencyFrom)
        final def accessedCurrencyTo = findExchangeRateWithCurrency(allCurrencies, currencyTo)
        accessedCurrencyFrom.accessCounter == 3
        accessedCurrencyTo.accessCounter == 3
    }

    def "shouldSuccessfullyRunGetLatestExchangeRateScheduledTask"() {
        given:
        final def newCurrencyName = "TEST_CURRENCY"
        final def newCurrencyExchangeRate = BigDecimal.valueOf(88.91)
        final def rates = new HashMap<String, BigDecimal>() {
            {
                put(newCurrencyName, BigDecimal.valueOf(newCurrencyExchangeRate))
            }
        }
        1 * fixerClient.getLatestExchangeRate(_ as String, _ as String) >> {
            String apiKey, String base ->
                assert apiKey.equalsIgnoreCase(fixerApiAccessKey)
                assert base.equalsIgnoreCase(baseCurrency)
                new LatestExchangeRate(baseCurrency, LocalDate.now(), rates)
        }

        when:
        currencyExchangeService.getLatestExchangeRate()

        then:
        final def allCurrencies = currencyExchangeRateRepository.findAll()
        final def expectedNewCurrencyExchange = findExchangeRateWithCurrency(allCurrencies, newCurrencyName)
        expectedNewCurrencyExchange != null
        verifyAll(expectedNewCurrencyExchange) {
            it.rate == newCurrencyExchangeRate
            it.accessCounter == 0
            it.exchangeDate == LocalDate.now()
            it.id != null
            it.createdAt != null
            it.createdAt.isBefore(Instant.now())
        }
    }

    def "shouldSuccessfullyTriggerLatestExchangesRetrievalAndUpdateAndCreateRates"() {
        given:
        final def eurRate = BigDecimal.valueOf(1.1)
        final def newCurrencyExchange1 = new NewCurrencyExchange("USD", "EUR", eurRate, INSERTED_CURRENCY_EXCHANGE_DATE)
        final def abcRate = BigDecimal.valueOf(2.2)
        final def newCurrencyExchange2 = new NewCurrencyExchange("USD", "ABC", abcRate, LocalDate.now())
        final def rates = new HashMap<String, BigDecimal>() {
            {
                put("AVC", BigDecimal.valueOf(1.1))
            }
        }
        1 * fixerClient.getLatestExchangeRate(_ as String, _ as String) >> {
            String apiKey, String base ->
                assert apiKey.equalsIgnoreCase(fixerApiAccessKey)
                assert base.equalsIgnoreCase(baseCurrency)
                new LatestExchangeRate(baseCurrency, LocalDate.now(), rates)
        }

        when:
        testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT_WITH_DATE.formatted("USD", "EUR", INSERTED_CURRENCY_EXCHANGE_DATE),
                                      CurrencyExchange.class)
        testRestTemplate.put(EXCHANGE_ENDPOINT, List.of(newCurrencyExchange1, newCurrencyExchange2))

        then:
        final def allCurrencies = currencyExchangeRateRepository.findAll()
        final def eurCurrency = findExchangeRateWithCurrencyAndDate(allCurrencies, "EUR", INSERTED_CURRENCY_EXCHANGE_DATE)
        final def abcCurrency = findExchangeRateWithCurrencyAndDate(allCurrencies, "ABC", LocalDate.now())
        eurCurrency != null
        verifyAll(eurCurrency) {
            it.rate == eurRate
            it.accessCounter == 1
        }
        abcCurrency != null
        verifyAll(abcCurrency) {
            it.rate == abcRate
            it.accessCounter == 0
        }
    }

    def "shouldReturnBadRequestStatusWhenExchangeCurrenciesAreTheSame"() {
        when:
        final def response = testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT.formatted("EUR", "EUR"), ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        verifyAll(response.body) {
            it.status() == HttpStatus.BAD_REQUEST
            it.message().equalsIgnoreCase("Cannot exchange the same currencies")
        }
    }

    def "shouldReturnBadRequestStatusWhenDateParamIsInFuture"() {
        given:
        final def futureDate = LocalDate.now().plusDays(1)

        when:
        final def response =
                testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT_WITH_DATE.formatted("EUR", "PLN", futureDate),
                                              ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        verifyAll(response.body) {
            it.status() == HttpStatus.BAD_REQUEST
            it.message().equalsIgnoreCase("Currency exchange date cannot be set to future")
        }
    }

    def "shouldReturnBadRequestStatusWhenParamsAreNotValidForGetEndpoint"() {
        when:
        final def response =
                testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT_WITH_DATE.formatted(currencyFrom, currencyTo, date),
                                              ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.status() == HttpStatus.BAD_REQUEST

        where:
        currencyFrom << ["", " ", VALID_CURRENCY_FROM, VALID_CURRENCY_FROM, VALID_CURRENCY_FROM, VALID_CURRENCY_FROM]
        currencyTo << [VALID_CURRENCY_TO, VALID_CURRENCY_TO, "", " ", VALID_CURRENCY_TO, VALID_CURRENCY_TO]
        date << [VALID_DATE, VALID_DATE, VALID_DATE, VALID_DATE, "2021-01-01T12:12:12", "abc"]
    }

    def "shouldReturnNotFoundStatusWhenCurrencyExchangeRateCannotBeFound"() {
        when:
        final def missingCurrency = "ABC"
        final def response = testRestTemplate.getForEntity(GET_EXCHANGE_ENDPOINT.formatted("EUR", missingCurrency),
                                                           ApiError.class)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        verifyAll(response.body) {
            it.status() == HttpStatus.NOT_FOUND
            it.message().equalsIgnoreCase("Currency exchange not found for %s with date %s".formatted(missingCurrency,
                                                                                                      LocalDate.now()))
        }
    }

    def "shouldReturnBadRequestStatusWhenCurrencyExchangeRateIsNegativeOrZero"() {
        given:
        final def newCurrencyExchange = new NewCurrencyExchange("USD", "PLN", BigDecimal.valueOf(invalidExchangeRate), LocalDate.now())
        final def httpEntity = new HttpEntity(List.of(newCurrencyExchange))

        when:
        final def response = testRestTemplate.exchange(EXCHANGE_ENDPOINT, HttpMethod.PUT, httpEntity, ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        verifyAll(response.body) {
            it.status() == HttpStatus.BAD_REQUEST
            it.message().equalsIgnoreCase("Currency exchange rate cannot be negative or zero")
        }

        where:
        invalidExchangeRate << [-2L, -1L, 0L]
    }

    def "shouldReturnBadRequestStatusWhenCurrencyExchangeRateOverflow"() {
        given:
        final def newCurrencyExchange = new NewCurrencyExchange("USD", "PLN", BigDecimal.valueOf(1234567890123456),
                                                                LocalDate.now())
        final def httpEntity = new HttpEntity(List.of(newCurrencyExchange))

        when:
        final def response = testRestTemplate.exchange(EXCHANGE_ENDPOINT, HttpMethod.PUT, httpEntity, ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        verifyAll(response.body) {
            it.status() == HttpStatus.BAD_REQUEST
            it.message().equalsIgnoreCase("Currency exchange rate overflow. Max accepted value length is 13")
        }
    }

    def "shouldReturnBadRequestStatusWhenCurrencyExchangeRateScaleOverflow"() {
        given:
        final def newCurrencyExchange = new NewCurrencyExchange("USD", "PLN", BigDecimal.valueOf(123.1234567),
                                                                LocalDate.now())
        final def httpEntity = new HttpEntity(List.of(newCurrencyExchange))

        when:
        final def response = testRestTemplate.exchange(EXCHANGE_ENDPOINT, HttpMethod.PUT, httpEntity, ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        verifyAll(response.body) {
            it.status() == HttpStatus.BAD_REQUEST
            it.message().equalsIgnoreCase("Currency exchange rate scale overflow. Max accepted length is 6")
        }
    }

    def "shouldReturnBadRequestStatusWhenCurrencyFromAndToParamsAreNotValidForPutEndpoint"() {
        given:
        final def newCurrencyExchange = new NewCurrencyExchange(currencyFrom, currencyTo, BigDecimal.valueOf(123.1),
                                                                LocalDate.now())
        final def httpEntity = new HttpEntity(List.of(newCurrencyExchange))

        when:
        final def response = testRestTemplate.exchange(EXCHANGE_ENDPOINT, HttpMethod.PUT, httpEntity, ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.status() == HttpStatus.BAD_REQUEST

        where:
        currencyFrom << ["", " ", VALID_CURRENCY_FROM, VALID_CURRENCY_FROM, VALID_CURRENCY_FROM]
        currencyTo << [VALID_CURRENCY_TO, VALID_CURRENCY_TO, "", " ", VALID_CURRENCY_FROM]
    }

    def "shouldReturnBadRequestStatusWhenExchangeDateIsInFutureForPutEndpoint"() {
        given:
        final def newCurrencyExchange = new NewCurrencyExchange(VALID_CURRENCY_FROM, VALID_CURRENCY_TO, BigDecimal.valueOf(123.1),
                                                                LocalDate.now().plusDays(1))
        final def httpEntity = new HttpEntity(List.of(newCurrencyExchange))

        when:
        final def response = testRestTemplate.exchange(EXCHANGE_ENDPOINT, HttpMethod.PUT, httpEntity, ApiError.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        verifyAll(response.body) {
            it.status() == HttpStatus.BAD_REQUEST
            it.message() == "Currency exchange date cannot be set to future"
        }
    }

    private ExchangeRate findExchangeRateWithCurrency(List<ExchangeRate> exchangeRates, String currencyFind) {
        return exchangeRates.stream()
                            .filter(currency -> currency.currencyFrom.equalsIgnoreCase(baseCurrency) &&
                                    currency.currencyTo.equalsIgnoreCase(currencyFind))
                            .findFirst()
                            .get()
    }

    private ExchangeRate findExchangeRateWithCurrencyAndDate(List<ExchangeRate> exchangeRates, String currencyFind,
                                                             LocalDate date) {
        return exchangeRates.stream()
                            .filter(currency -> currency.currencyFrom.equalsIgnoreCase(baseCurrency) &&
                                    currency.currencyTo.equalsIgnoreCase(currencyFind) && currency.exchangeDate == date)
                            .findFirst()
                            .get()
    }
}
