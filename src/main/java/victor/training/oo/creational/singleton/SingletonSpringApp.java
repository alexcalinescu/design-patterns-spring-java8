package victor.training.oo.creational.singleton;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableCaching
@SpringBootApplication
public class SingletonSpringApp implements CommandLineRunner{
	@Bean
	public static CustomScopeConfigurer defineThreadScope() {
		CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("thread", new SimpleThreadScope()); // WARNING: Leaks memory. Prefer 'request' scope or read here: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/support/SimpleThreadScope.html 
		return configurer;
	}
	
	public static void main(String[] args) {
		SpringApplication.run(SingletonSpringApp.class);
	}
	
	@Autowired 
	private OrderExporter exporter;
	
	// [1] make singleton; test multi-thread: state is [E|V|I|L]
	// [2] instantiate manually, set dependencies, pass around; no AOP
	// [3] prototype scope + ObjectFactory or @Lookup. Did you said "Factory"? ...
	// [4] thread/request scope. HOW it works?! Leaks: @see SimpleThreadScope javadoc
	// TODO [5] (after AOP): RequestContext, @Cacheable. on thread?! @ThreadLocal
	public void run(String... args) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		pool.submit(() -> exporter.export(Locale.ENGLISH));
		pool.submit(() -> exporter.export(Locale.FRENCH));
	}
}

@Slf4j
@Service
class OrderExporter  {
	@Autowired
	private InvoiceExporter invoiceExporter;
	@Autowired
	private LabelService labelService;
	
	public void export(Locale locale) {
		log.debug("WHO ARE YOU !??: " + labelService.getClass());
		
		labelService.load(locale);
		log.debug("Running export in " + locale);
		log.debug("Origin Country: " + labelService.getCountryName("rO")); 
		invoiceExporter.exportInvoice();
	}
}

@Slf4j
@Service 
class InvoiceExporter {
	@Autowired
	private LabelService labelService;
	
	public void exportInvoice() {
		log.debug("Invoice Country: " + labelService.getCountryName("ES"));
	}
}

@Slf4j
@Service
@Scope(scopeName = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
class LabelService {
	@Autowired
	private CountryRepo countryRepo;
	
	public LabelService(CountryRepo countryRepo) {
		this.countryRepo = countryRepo;
	}

	private Map<String, String> countryNames;
	
	public void load(Locale locale) {
		countryNames = countryRepo.loadCountryNamesAsMap(locale);
	}
	
	public String getCountryName(String iso2Code) {
		log.debug("getCountryName() on instance: " + this.hashCode());
		return countryNames.get(iso2Code.toUpperCase());
	}
}