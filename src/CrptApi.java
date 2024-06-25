import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.List;
import java.util.concurrent.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Класс CrptApi содержит в себе определение основных полей и методов для формирования и отправки документов
 * с помощью запроса к внешнему сервису.
 * В качестве аргументов объект класса принимает измерение времени (секунды, минуты и т.д.),
 * а также ограничение по количеству запросов. Таким образом, достигается защита от превышения лимита запросов.
 */
public class CrptApi {
    private final String uri = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final TimeUnit timeUnit;
    private final int refreshSchedulePeriod;

    private final int requestLimit;
    private final Semaphore semaphore;

    private final ScheduledExecutorService scheduleService;

    private final HttpClient httpClient;

    private final ObjectMapper mapper;

    CrptApi(TimeUnit timeUnit, int requestsLimit) {
        this.timeUnit = timeUnit;
        this.refreshSchedulePeriod = 5; // По умолчанию период будет 5 сек, мин...

        this.requestLimit = requestsLimit;
        this.semaphore = new Semaphore(requestsLimit);

        this.httpClient = HttpClient.newHttpClient();

        this.mapper = new ObjectMapper();

        this.scheduleService = Executors.newScheduledThreadPool(requestsLimit);
        this.scheduleService.scheduleAtFixedRate(this::refreshSchedule, this.refreshSchedulePeriod,
                this.refreshSchedulePeriod, timeUnit);
    }

    private void refreshSchedule() {
        this.semaphore.release(this.requestLimit);
    }

    public void createDocument(CrptDocument document) throws InterruptedException, JsonProcessingException {
        String jsonDocument = convertJson(document);

        this.semaphore.acquire();

        requestToApi(jsonDocument);
    }

    private String convertJson(CrptDocument document) throws JsonProcessingException {
        try {
            return mapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestToApi(String jsonDocument) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("haha"))
                .build();

        CompletableFuture<HttpResponse<String>> response =
                this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Класс CrptDocument необходим для создания объекта документа с последующим
     * использованием его в методах класса CrptApi.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class CrptDocument {
        @JsonProperty("Description")
        private CrptDocumentDescription description;
        private String doc_id;
        private String doc_status;
        private DocType doc_type;
        private Boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<CrptDocumentProduct> products;

        private CrptDocument(CrptDocumentDescription description, String doc_id, String doc_status, DocType doc_type,
                             Boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                             String production_date, String production_type, List<CrptDocumentProduct> products) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
        }
    }

    /**
     * Класс CrptDocumentDescription необходим для создания объекта описания документа с последующим
     * использованием его в объекте класса CrptDocument.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class CrptDocumentDescription {
        private String participantInn;

        CrptDocumentDescription(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    /**
     * Класс CrptDocumentProduct необходим для создания объекта продукта с последующим
     * использованием его в объекте класса CrptDocument.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class CrptDocumentProduct {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
        private String reg_date;
        private String reg_number;

        CrptDocumentProduct(String certificate_document, String certificate_document_date,
                            String certificate_document_number, String owner_inn, String producer_inn,
                            String production_date, String tnved_code, String uit_code, String uitu_code,
                            String reg_date, String reg_number) {
            this.certificate_document = certificate_document;
            this.certificate_document_number = certificate_document_number;
            this.certificate_document_date = certificate_document_date;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }
    }

    @JsonAutoDetect
    public static enum DocType {
        LP_INTRODUCE_GOODS
    }
}

