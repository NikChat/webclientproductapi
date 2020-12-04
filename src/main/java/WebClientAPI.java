import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebClientAPI {
    private WebClient webClient; //WebClient accepts and returns publishers like Flux and Mono (alternative of RestTemplate)

    //Creating a WebClient Instance
    WebClientAPI() {
        //this.webClient = WebClient.create("http://localhost:8080/products");
        this.webClient =WebClient.builder() //mporw na valw kai headers, cookies
                .baseUrl("http://localhost:8080/products")
                .build();
    }

    public static void main(String args[]) {
        WebClientAPI api = new WebClientAPI();

        api.postNewProduct() //saving the product
                .thenMany(api.getAllProducts()) //thenMany will wait for the completion signal of the Mono returned by postNewProduct()
                .take(1)
                .flatMap(p -> api.updateProduct(p.getId(), "White Tea", 0.99))
                .flatMap(p -> api.deleteProduct(p.getId()))
                .thenMany(api.getAllProducts())
                .thenMany(api.getAllEvents())
                .subscribe(System.out::println);
    }

    //Perform a POST request to save a new product
    private Mono<ResponseEntity<Product>> postNewProduct() {
        return webClient
                .post() //WebClient.RequestBodyUriSpec
                .body(Mono.just(new Product(null, "Black Tea", 1.99)), Product.class)
                .exchange() //we perform the request, and start retrieving the result. edw exw Mono<ClientResponse> type gt xrhsimopoihsa exchange()
                .flatMap(response -> response.toEntity(Product.class)) //transform the response of type ClienResponse, to ResponseEntity<Product> wrapped in a Mono
                .doOnSuccess(o -> System.out.println("**********POST " + o));
        //I	haven't subscribed yet!
        //In main: I will combine all the methods into a sequence
    }
    
    //exchange(): returns a Mono<ClientResponse>
    //ClientResponse = an interface that provides access to the response status, headers, and has methods to consume the Response Body
//    Flux<Product> flux = 
//    		webClient
//    			.get()
//    			.exchange()
//    			.flatMap(response -> response.bodyToFlux(Product.class)); --> LATHOS??
    
//    Mono<ResponseEntity<List<Product>>> mono = 
//    		webClient
//    			.get()
//    			.exchange()
//    			.flatMap(response -> response.toEntityList(Product.class));
    
    private Flux<Product> getAllProducts() {
        return webClient
                .get()//WebClient.RequestHeaderUriSpec
                //.accept(MediaType.APPLICATION_JSON) ////WebClient.RequestHeaderSpec
                .retrieve() //after calling retrieve, we can transform the body to Mono or Flux
                .bodyToFlux(Product.class)
                .doOnNext(o -> System.out.println("**********GET: " + o)); //when a product is published-emitted by this flux (for each onNext event)
    }

    private Mono<Product> updateProduct(String id, String name, double price) {
        return webClient
                .put()
                .uri("/{id}", id)//WebClient.UriSpec
                .body(Mono.just(new Product(null, name, price)), Product.class)
                .retrieve() //the api will return the updated product
                .bodyToMono(Product.class) //convert the response to a Mono
                .doOnSuccess(o -> System.out.println("**********UPDATE " + o));
    }

    private Mono<Void> deleteProduct(String id) {
        return webClient
                .delete()
                .uri("/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(o -> System.out.println("**********DELETE " + o)); //DELETE null
    }

    private Flux<ProductEvent> getAllEvents() {
        return webClient
                .get()
                .uri("/events")
                .retrieve()
                .bodyToFlux(ProductEvent.class);
    }
    
//    If we are not working with asynchronous types:
//    Product product = ...
//    webClient
//    	.post()
//    	.uri("/products")
//    	.contentType(MediaType.APLICATION_JSON)
//    	.syncBody(product)
    
//    If I'm sending form data - Preparing the form data request:
//    .body(BodyInserters.fromFormData("field1","val1").with("field2","val2"))
//    
//    .body(BodyInserters.fromMultipartData("field1","val1").with("file", file))
}
